package Service;

import Model.Reservation;

import javax.swing.SwingUtilities;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** ศูนย์กลางข้อมูลการจอง (User/Admin ใช้ร่วมกัน) — ปลอดภัยเมื่อเปิดหลายหน้าต่าง */
public class ReservationManager {

    /* ====================== Singleton ====================== */
    private static final ReservationManager INSTANCE = new ReservationManager();
    public static ReservationManager getInstance() { return INSTANCE; }

    /* ====================== Storage ====================== */
    private final List<Reservation> reservations = new ArrayList<>();
    private final List<ReservationListener> listeners = new ArrayList<>();

    private static final String FILE_NAME = "Booking.txt";                 // <- ใช้ไฟล์นี้
    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;   // 2025-10-09
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm"); // 09:00

    private ReservationManager() { loadAllFromFile(); }

    /* ====================== Listener ====================== */
    public interface ReservationListener {
        default void onReservationAdded(Reservation r) {}
        default void onReservationChanged(List<Reservation> all) {}
        default void onReservationStatusChanged(Reservation r) {}
    }
    public synchronized void addListener(ReservationListener l) { if (l!=null && !listeners.contains(l)) listeners.add(l); }
    public synchronized void removeListener(ReservationListener l) { listeners.remove(l); }

    /* ====================== Key/Overlap helpers ====================== */
    private String keyOf(String room, LocalDate date, LocalTime start, LocalTime end) {
        return room + "|" + date + "|" + start + "|" + end;
    }
    private boolean isOverlap(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    /* ====================== Public APIs (safe for multi windows) ====================== */

    /** โหลดไฟล์ก่อนตรวจ แล้วเช็กว่าสลอตว่างจริงหรือไม่ (กันจองซ้ำ) */
    // บล็อกทุกสถานะที่ไม่ใช่ Available (เช่น Reserved, Closed, Approved, Pending ฯลฯ)
        private boolean isBlocking(String status) {
    return status != null && !status.equalsIgnoreCase("Available");
    }

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

    /** จองถ้าว่างจริง: รีโหลด → ตรวจ → merge → เซฟ → broadcast */
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

    
    /** เดิม add() → ให้ไปใช้ addIfAvailable() เพื่อความปลอดภัย */
    public synchronized void add(Reservation r) { addIfAvailable(r); }

    /** แอดมินเปลี่ยนสถานะ (upsert + merge) */
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
        // ⬇️ เดิมใส่ "System" → เปลี่ยนเป็น null/"" จะไม่ไปโผล่ใน History ว่าใคร "จอง"
        String username = null; // หรือ "" ตามที่ Reservation/formatLine รองรับ
        target = new Reservation(username, room, date, start, end, newStatus);
        reservations.add(target);
    } else {
        target.setStatus(newStatus);
        // ไม่ไปแก้ username เดิม เพราะเป็นคนที่ "จอง" จริง
    }

    // (ถ้ามีฟิลด์ไว้เก็บผู้แก้ไข ให้เติมด้วย)
    try {
        
    } catch (Exception ignore) {}

    saveAllToFile();
    notifyStatusChanged(target);
    notifyChanged();
}


    /** อ่านรายการของวันนั้น (รีโหลดก่อนเสมอ) */
    public synchronized List<Reservation> getByDate(LocalDate date) {
        loadAllFromFile();
        List<Reservation> out = new ArrayList<>();
        for (Reservation r : reservations) if (r.getDate().equals(date)) out.add(copy(r));
        return out;
    }

    /** อ่านทั้งหมด (รีโหลดก่อนเสมอ) */
    public synchronized List<Reservation> getAll() {
        loadAllFromFile();
        List<Reservation> out = new ArrayList<>();
        for (Reservation r : reservations) out.add(copy(r));
        return out;
    }

    /* ====================== Internal helpers ====================== */

    private Reservation copy(Reservation r) {
        return new Reservation(r.getUsername(), r.getRoom(), r.getDate(), r.getStart(), r.getEnd(), r.getStatus());
    }

    /* ====================== Persistence ====================== */

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

    // username|room|date|start|end|status  (start/end = HH:mm)
    private String formatLine(Reservation r) {
        return String.join("|",
                nz(r.getUsername()),
                nz(r.getRoom()),
                r.getDate().format(DF),
                r.getStart().format(TF),
                r.getEnd().format(TF),
                nz(r.getStatus()));
    }

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

    private String nz(String s) { return s == null ? "" : s; }
    private String znull(String s) { return (s == null || s.isEmpty()) ? null : s; }

    /* ====================== Notifiers (EDT) ====================== */

    private void notifyAdded(Reservation r) {
        SwingUtilities.invokeLater(() -> {
            for (ReservationListener l : snapshotListeners()) {
                try { l.onReservationAdded(copy(r)); } catch (Exception ignored) {}
            }
        });
    }

    private void notifyStatusChanged(Reservation r) {
        SwingUtilities.invokeLater(() -> {
            for (ReservationListener l : snapshotListeners()) {
                try { l.onReservationStatusChanged(copy(r)); } catch (Exception ignored) {}
            }
        });
    }

    private void notifyChanged() {
        List<Reservation> all = getAll(); // getAll() รีโหลดแล้ว
        SwingUtilities.invokeLater(() -> {
            for (ReservationListener l : snapshotListeners()) {
                try { l.onReservationChanged(all); } catch (Exception ignored) {}
            }
        });
    }

    private synchronized List<ReservationListener> snapshotListeners() {
        return new ArrayList<>(listeners);
    }
}
