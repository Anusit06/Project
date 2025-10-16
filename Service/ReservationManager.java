package Service;

import Model.Reservation;
import javax.swing.SwingUtilities;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ReservationManager
 *
 * ศูนย์รวมการจัดการข้อมูลการจองที่ใช้ไฟล์ร่วมกัน
 * ทุกครั้งที่เรียกเมธอดสาธารณะ จะโหลดข้อมูลจากไฟล์ก่อนใช้งาน
 * ทำงานกับรายการในหน่วยความจำ จากนั้นบันทึกกลับไฟล์ และแจ้งผู้ฟังบน EDT
 * หมายเหตุ: EDT คือเธรดสำหรับงาน UI ของ Swing ควรแจ้งเตือนผู้ฟังบนเธรดนี้เสมอ
 */
public class ReservationManager {

    private static final ReservationManager INSTANCE = new ReservationManager();

    /**
     * คืนอินสแตนซ์ตัวเดียวของตัวจัดการจอง (Single instance)
     * @return อินสแตนซ์ของ ReservationManager
     */
    public static ReservationManager getInstance() { return INSTANCE; }

    /** รายการจองที่เก็บไว้ในหน่วยความจำ (จะโหลดจากไฟล์ก่อนใช้งานทุกครั้ง) */
    private final List<Reservation> reservations = new ArrayList<>();

    /** รายชื่อผู้ฟังการเปลี่ยนแปลงข้อมูล (หน้าจอ UI อาจสมัครฟังเพื่อรีเฟรช) */
    private final List<ReservationListener> listeners = new ArrayList<>();

    /** ชื่อไฟล์เก็บข้อมูลการจอง (บันทึกแบบ UTF-8) */
    private static final String FILE_NAME = "Booking.txt";  
    
    /** รูปแบบวันที่ในไฟล์ (ตัวอย่าง 2025-10-09) */
    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

    /** รูปแบบเวลาในไฟล์ (ตัวอย่าง 09:00) */
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm"); // 09:00

    /**
     * สร้างตัวจัดการจอง และโหลดข้อมูลทั้งหมดจากไฟล์ครั้งแรก
     */
    private ReservationManager() { loadAllFromFile(); }

    /**
     * อินเทอร์เฟซสำหรับผู้ฟังเหตุการณ์การจอง
     * ผู้ฟังจะได้รับแจ้งเมื่อเพิ่มการจอง เปลี่ยนสถานะ หรือข้อมูลทั้งหมดมีการเปลี่ยนแปลง
     */
    public interface ReservationListener {
        /**
         * เรียกเมื่อมีการเพิ่มการจองใหม่
         * @param r รายการจองที่ถูกเพิ่ม
         */
        default void onReservationAdded(Reservation r) {}
        /**
         * เรียกเมื่อข้อมูลรวมทั้งหมดมีการเปลี่ยนแปลง
         * @param all รายการจองทั้งหมดล่าสุด
         */
        default void onReservationChanged(List<Reservation> all) {}
        /**
         * เรียกเมื่อสถานะของรายการหนึ่งรายการมีการเปลี่ยนแปลง
         * @param r รายการจองที่สถานะถูกเปลี่ยน
         */
        default void onReservationStatusChanged(Reservation r) {}
    }
    public synchronized void addListener(ReservationListener l) { if (l!=null && !listeners.contains(l)) listeners.add(l); }
    public synchronized void removeListener(ReservationListener l) { listeners.remove(l); }

    /**
     * สร้างคีย์เฉพาะของสลอตการจองจากห้อง วันที่ เวลาเริ่ม และเวลาสิ้นสุด
     * ใช้สำหรับอ้างอิงรายการจองหนึ่ง ๆ
     * @param room ห้อง
     * @param date วันที่
     * @param start เวลาเริ่ม
     * @param end เวลาสิ้นสุด
     * @return คีย์สตริงที่ระบุสลอตนี้แบบไม่ซ้ำ
     */
    private String keyOf(String room, LocalDate date, LocalTime start, LocalTime end) {
        return room + "|" + date + "|" + start + "|" + end;
    }

     /**
     * ตรวจสอบว่าช่วงเวลา A และ B ทับซ้อนกันหรือไม่
     * @param aStart เวลาเริ่ม A
     * @param aEnd เวลาจบ A
     * @param bStart เวลาเริ่ม B
     * @param bEnd เวลาจบ B
     * @return true หากช่วงเวลาทับซ้อนกัน, false หากไม่ทับซ้อน
     */
    private boolean isOverlap(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    /**
     * ตรวจสอบว่าสถานะนี้ถือว่าเป็นการบล็อกการจองหรือไม่
     * สถานะที่ไม่ใช่ "Available" ถือว่าบล็อกทั้งหมด
     * @param status ชื่อสถานะ เช่น Reserved, Closed, Available
     * @return true หากบล็อก, false หากไม่บล็อก (คือ Available)
     */
    private boolean isBlocking(String status) {
    return status != null && !status.equalsIgnoreCase("Available");
    }

    /**
     * ตรวจสอบว่าสลอตหนึ่ง ๆ ยังว่างอยู่หรือไม่
     * จะโหลดข้อมูลจากไฟล์ก่อน แล้วเช็กว่ามีรายการที่สถานะบล็อกและช่วงเวลาทับซ้อนหรือไม่
     * @param room ห้อง
     * @param date วันที่
     * @param start เวลาเริ่ม
     * @param end เวลาสิ้นสุด
     * @return true หากยังว่าง, false หากไม่ว่าง
     */
    public synchronized boolean isSlotAvailable(String room, LocalDate date, LocalTime start, LocalTime end) {
        loadAllFromFile();
        for (Reservation r : reservations) {
            if (Objects.equals(r.getRoom(), room) && r.getDate().equals(date)) {
                if (isBlocking(r.getStatus()) && isOverlap(start, end, r.getStart(), r.getEnd())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * เพิ่มการจองใหม่ ถ้าและเฉพาะเมื่อสลอตว่างจริง
     * ขั้นตอนคือ โหลดไฟล์ ตรวจสอบว่าง รวมรายการทับคีย์เดิม บันทึก และแจ้งผู้ฟัง
     * @param r รายการจองใหม่ที่ต้องการเพิ่ม
     * @return true หากเพิ่มสำเร็จ, false หากสลอตไม่ว่างหรือข้อมูลไม่ครบ
     */
    public synchronized boolean addIfAvailable(Reservation r) {
        if (r == null) return false;
        loadAllFromFile();
        if (!isSlotAvailable(r.getRoom(), r.getDate(), r.getStart(), r.getEnd())) return false;

        Map<String, Reservation> map = new LinkedHashMap<>();
        for (Reservation ex : reservations) {
            map.put(keyOf(ex.getRoom(), ex.getDate(), ex.getStart(), ex.getEnd()), ex);
        }
        map.put(keyOf(r.getRoom(), r.getDate(), r.getStart(), r.getEnd()), r);

        reservations.clear();
        reservations.addAll(map.values());
        saveAllToFile();

        notifyAdded(r);
        notifyChanged();
        return true;
    }

    /**
     * เพิ่มการจองแบบเดิม โดยภายในเรียกใช้ addIfAvailable เพื่อความปลอดภัย
     * @param r รายการจองที่ต้องการเพิ่ม
     */
    public synchronized void add(Reservation r) { addIfAvailable(r); }

    /**
     * เปลี่ยนสถานะของสลอตหนึ่ง ๆ สำหรับงานแอดมิน
     * หากยังไม่มีสลอตนี้ จะสร้างรายการใหม่ที่ไม่มีชื่อผู้จอง จากนั้นตั้งสถานะ
     * ไม่แก้ไขชื่อผู้จองเดิมหากมีอยู่แล้ว
     * @param room ห้อง
     * @param date วันที่
     * @param start เวลาเริ่ม
     * @param end เวลาสิ้นสุด
     * @param newStatus สถานะใหม่ เช่น Closed, Reserved, Available
     */
    public synchronized void setStatus(String room, LocalDate date, LocalTime start, LocalTime end, String newStatus) {
        loadAllFromFile();
        String k = keyOf(room, date, start, end);
        Reservation target = null;
        for (Reservation x : reservations) {
            if (keyOf(x.getRoom(), x.getDate(), x.getStart(), x.getEnd()).equals(k)) { target = x; break; }
        }

        String actor = null;
        try { actor = Model.UserSession.getCurrentUsername(); } catch (Exception ignore) {}
        if (target == null) {
            String username = null; 
            target = new Reservation(username, room, date, start, end, newStatus);
            reservations.add(target);
        } else {
            target.setStatus(newStatus);
            // ไม่ไปแก้ username เดิม เพราะเป็นคนที่ "จอง" จริง
        }

        saveAllToFile();
        notifyStatusChanged(target);
        notifyChanged();
    }

    /**
     * อ่านรายการจองเฉพาะวันที่ระบุ
     * จะโหลดไฟล์ก่อน และคืนค่าเป็นสำเนาใหม่เพื่อป้องกันการแก้ไขรายการภายใน
     * @param date วันที่ต้องการค้นหา
     * @return รายการจองของวันนั้น ๆ
     */
    public synchronized List<Reservation> getByDate(LocalDate date) {
        loadAllFromFile();
        List<Reservation> out = new ArrayList<>();
        for (Reservation r : reservations) if (r.getDate().equals(date)) out.add(copy(r));
        return out;
    }

    /**
     * อ่านรายการจองทั้งหมด
     * จะโหลดไฟล์ก่อน และคืนค่าเป็นสำเนาใหม่เพื่อป้องกันการแก้ไขรายการภายใน
     * @return รายการจองทั้งหมด
     */
    public synchronized List<Reservation> getAll() {
        loadAllFromFile();
        List<Reservation> out = new ArrayList<>();
        for (Reservation r : reservations) out.add(copy(r));
        return out;
    }

    /**
     * สร้างสำเนาใหม่ของรายการจอง เพื่อไม่ให้ภายนอกแก้ไขรายการภายใน
     * @param r รายการจองต้นฉบับ
     * @return สำเนาของรายการจอง
     */
    private Reservation copy(Reservation r) {
        return new Reservation(r.getUsername(), r.getRoom(), r.getDate(), r.getStart(), r.getEnd(), r.getStatus());
    }

    /**
     * โหลดข้อมูลทั้งหมดจากไฟล์เข้าสู่หน่วยความจำ
     * หากไฟล์ไม่พบ จะล้างรายการให้ว่าง
     */
    private synchronized void loadAllFromFile() {
        File f = new File(FILE_NAME);
        if (!f.exists()) { reservations.clear(); return; }

        List<Reservation> loaded = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                Reservation r = parseLine(line);
                if (r != null) loaded.add(r);
            }
        } catch (IOException e) {
            System.err.println("โหลด " + FILE_NAME + " ล้มเหลว: " + e.getMessage());
        }
        reservations.clear();
        reservations.addAll(loaded);
    }
    
    /**
     * บันทึกรายการทั้งหมดจากหน่วยความจำกลับลงไฟล์
     * จะเขียนทับทั้งไฟล์ใหม่
     */
    private synchronized void saveAllToFile() {
        File f = new File(FILE_NAME);
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, false), StandardCharsets.UTF_8))) {
            for (Reservation r : reservations) {
                bw.write(formatLine(r));
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("บันทึก " + FILE_NAME + " ล้มเหลว: " + e.getMessage());
        }
    }

    /**
     * สร้างบรรทัดข้อมูลตามรูปแบบไฟล์
     * รูปแบบคือ username|room|date|start|end|status
     * @param r รายการจอง
     * @return สตริงบรรทัดเดียวตามรูปแบบไฟล์
     */
    private String formatLine(Reservation r) {
        return String.join("|",
                nz(r.getUsername()),
                nz(r.getRoom()),
                r.getDate().format(DF),
                r.getStart().format(TF),
                r.getEnd().format(TF),
                nz(r.getStatus()));
    }

    /**
     * แปลงบรรทัดข้อความจากไฟล์ให้เป็นอ็อบเจ็กต์การจอง
     * หากรูปแบบไม่ตรงหรือแปลงไม่ได้จะคืนค่า null
     * @param line บรรทัดจากไฟล์
     * @return อ็อบเจ็กต์ Reservation หรือ null หากแปลงไม่ได้
     */
    private Reservation parseLine(String line) {
        if (line == null || line.isBlank()) return null;
        String[] p = line.split("\\|");
        if (p.length != 6) return null;
        try {
            String username = znull(p[0]);
            String room     = znull(p[1]);
            LocalDate date  = LocalDate.parse(p[2], DF);
            LocalTime start = LocalTime.parse(p[3], TF);
            LocalTime end   = LocalTime.parse(p[4], TF);
            String status   = znull(p[5]);
            return new Reservation(username, room, date, start, end, status);
        } catch (Exception ex) {
            System.err.println("แปลงบรรทัดไม่สำเร็จ: " + line + " (" + ex.getMessage() + ")");
            return null;
        }
    }

    /**
     * แทนค่า null ด้วยสตริงว่าง เพื่อป้องกันปัญหาเวลา join ข้อความ
     * @param s สตริงที่อาจเป็น null
     * @return สตริงเดิม หรือ "" หากเป็น null
     */
    private String nz(String s) { return s == null ? "" : s; }

    /**
     * แปลงสตริงว่างให้เป็น null เพื่อเก็บในอ็อบเจ็กต์ให้ชัดเจน
     * @param s สตริงที่อาจว่าง
     * @return null หากสตริงว่าง หรือสตริงเดิมหากไม่ว่าง
     */
    private String znull(String s) { return (s == null || s.isEmpty()) ? null : s; }

    /**
     * แจ้งผู้ฟังว่ามีการเพิ่มรายการจองใหม่
     * จะเรียกบน EDT เพื่อความปลอดภัยในการอัปเดต UI
     * @param r รายการจองที่เพิ่งถูกเพิ่ม
     */
    private void notifyAdded(Reservation r) {
        SwingUtilities.invokeLater(() -> {
            for (ReservationListener l : snapshotListeners()) {
                try { l.onReservationAdded(copy(r)); } catch (Exception ignored) {}
            }
        });
    }

    /**
     * แจ้งผู้ฟังว่าสถานะของรายการหนึ่งมีการเปลี่ยนแปลง
     * จะเรียกบน EDT เพื่อความปลอดภัยในการอัปเดต UI
     * @param r รายการจองที่สถานะถูกเปลี่ยน
     */
    private void notifyStatusChanged(Reservation r) {
        SwingUtilities.invokeLater(() -> {
            for (ReservationListener l : snapshotListeners()) {
                try { l.onReservationStatusChanged(copy(r)); } catch (Exception ignored) {}
            }
        });
    }

    /**
     * แจ้งผู้ฟังว่าข้อมูลทั้งหมดมีการเปลี่ยนแปลง
     * จะเรียกบน EDT เพื่อความปลอดภัยในการอัปเดต UI
     */
    private void notifyChanged() {
        List<Reservation> all = getAll(); // getAll() รีโหลดแล้ว
        SwingUtilities.invokeLater(() -> {
            for (ReservationListener l : snapshotListeners()) {
                try { l.onReservationChanged(all); } catch (Exception ignored) {}
            }
        });
    }

    /**
     * คืนรายการผู้ฟังแบบสำเนาใหม่ เพื่อเลี่ยงปัญหาแก้ไขรายการระหว่างวนลูป
     * @return รายการผู้ฟังแบบสำเนาใหม่
     */
    private synchronized List<ReservationListener> snapshotListeners() {
        return new ArrayList<>(listeners);
    }
}


