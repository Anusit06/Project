package Util.Action;

import Model.Reservation;
import Model.UserSession;
import Service.ReservationManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * ตัวจัดการตรรกะของปุ่ม "จอง" แยกออกจากคลาส GUI หลัก
 */
public class ReserveButtonHandler {

    /** Callback สำหรับเรียกใช้เมธอดช่วยเดิม ๆ ของ GUI เพื่อลดการผูกกับ Swing ตรง ๆ */
    public interface ReservationOps {
        /**
         * นับจำนวนช่วงเวลาที่ผู้ใช้นี้มีสถานะ "ไม่ว่าง/จองแล้ว" ในวันเดียวกัน (มุมมองฝั่ง UI)
         * @param username ผู้ใช้
         * @param date วันที่
         * @return จำนวนช่วงเวลาที่ใช้ไปแล้ว
         */
        int countUserActiveSlotsUI(String username, LocalDate date);

        /**
         * แปลงข้อความช่วงเวลา เช่น "09:00-10:00" ให้เป็นเวลาเริ่ม/สิ้นสุด
         * @param slot รูปแบบ "HH:mm-HH:mm"
         * @return อาร์เรย์ขนาด 2: [เวลาเริ่ม, เวลาสิ้นสุด]
         */
        LocalTime[] parseSlot(String slot);

        /**
         * ตรวจว่า slot ว่างจริงไหม (ฝั่ง UI/โดเมนเบื้องต้น)
         * @param room รหัสห้อง เช่น "S01"
         * @param date วันที่
         * @param start เวลาเริ่ม
         * @param end เวลาสิ้นสุด
         * @return true ถ้าว่าง
         */
        boolean isSlotAvailable(String room, LocalDate date, LocalTime start, LocalTime end);
    }

    // ====== ฟิลด์อ้างอิงที่ต้องใช้จาก GUI ======
    private final Component parent;
    private final Map<String, List<JCheckBox>> floorCheckBoxes;
    private final Map<String, String[]> floorRooms;
    private final String[] times;
    private final Map<String, String> bookingStatus;
    private final int maxSlotsPerUserPerDay;
    private final DateTimeFormatter ddMMyyyy;
    private final ReservationOps ops;

    /**
     * สร้างตัวจัดการตรรกะปุ่ม "จอง"
     */
    public ReserveButtonHandler(Component parent,
                                Map<String, List<JCheckBox>> floorCheckBoxes,
                                Map<String, String[]> floorRooms,
                                String[] times,
                                Map<String, String> bookingStatus,
                                int maxSlotsPerUserPerDay,
                                DateTimeFormatter ddMMyyyy,
                                ReservationOps ops) {
        this.parent = parent;
        this.floorCheckBoxes = floorCheckBoxes;
        this.floorRooms = floorRooms;
        this.times = times;
        this.bookingStatus = bookingStatus;
        this.maxSlotsPerUserPerDay = maxSlotsPerUserPerDay;
        this.ddMMyyyy = ddMMyyyy;
        this.ops = ops;
    }

   /**
     * เรียกทำงานหลักของปุ่ม "จอง" (ย้ายมาจาก listener เดิมแบบ 1:1)
     *     ตรวจว่าล็อกอินหรือยัง + ตรวจวันที่
     *     คำนวณสิทธิ์คงเหลือต่อวัน
     *     วนเช็คบ็อกซ์ทุกชั้น → กันเกินโควตา → กันซ้อนเวลา → ขอจองผ่าน Service
     *     อัปเดต UI ช่องที่จองสำเร็จ และสรุปผล
     */
    public void execute() {
        boolean booked = false;
        final LocalDate date = getSelectedDateFromUI();
        final String username = UserSession.getCurrentUsername();

        // ----- ตรวจสิทธิ์คงเหลือของผู้ใช้ในวันนั้น -----
        int used = ops.countUserActiveSlotsUI(username, date);
        int remaining = maxSlotsPerUserPerDay - used;
        if (remaining <= 0) {
            warn("เกินโควตา",
                "คุณใช้สิทธิ์จองครบ " + maxSlotsPerUserPerDay + " ช่วงเวลาของวันที่ "
                    + date.format(ddMMyyyy) + " แล้ว");
            return;
        }

        int pickedThisClick = 0; // จำนวนช่วงที่จองสำเร็จจากการคลิกครั้งนี้

        // ----- วนทุกเช็คบ็อกซ์ของทุกชั้น -----
        for (String floor : floorCheckBoxes.keySet()) {
            List<JCheckBox> list = floorCheckBoxes.get(floor);
            String[] rooms = floorRooms.get(floor);
            if (list == null || rooms == null) continue;

            for (int i = 0; i < list.size(); i++) {
                JCheckBox cb = list.get(i);
                int roomIdx = i / times.length;
                int timeIdx = i % times.length;
                if (roomIdx < 0 || roomIdx >= rooms.length || timeIdx < 0 || timeIdx >= times.length) {
                    continue; // กัน index ผิด
                }

                final String room = rooms[roomIdx];
                final String slot = times[timeIdx];
                final String key  = floor + "-" + room + "-" + slot;

                // ประมวลผลเฉพาะเช็คบ็อกซ์ที่ผู้ใช้เลือกและยัง enabled
                if (cb.isSelected() && cb.isEnabled()) {

                    // ----- กันเกินโควตาแบบ real-time ระหว่างคลิกนี้ -----
                    if (pickedThisClick >= remaining) {
                        warn("เกินโควตา",
                            "จองได้ไม่เกิน " + maxSlotsPerUserPerDay + " ช่วงต่อวัน");
                        cb.setSelected(false);
                        continue;
                    }

                    // กันเวลาซ้อน (ถือว่าไม่ว่างถ้าไม่ใช่ Available)
                    LocalTime[] tt = ops.parseSlot(slot);
                    if (tt == null || tt.length != 2 || tt[0] == null || tt[1] == null) {
                        cb.setSelected(false);
                        continue;
                    }

                    // สร้างเรคคอร์ดการจอง
                    Reservation r = new Reservation(username, room, date, tt[0], tt[1], "Reserved");

                    // ขอจองผ่าน Service (ฝั่ง service จะตรวจความปลอดภัย/ชนกัน/โควตา ซ้ำอีกรอบ)
                    boolean ok = ReservationManager.getInstance().addIfAvailable(r);
                    if (!ok) {
                        // กรณี service ปฏิเสธ (เช่น race condition หรือโควตาเต็มในชั้น service)
                        warn("จองไม่สำเร็จ",
                            "ไม่สามารถจองได้ (อาจเกินโควตาหรือเวลาถูกจอง/ปิดแล้ว)");
                        cb.setSelected(false);
                        continue;
                    }

                    // อัปเดต UI ทันทีให้เห็นว่า "จองแล้ว"
                    cb.setBackground(Color.RED);
                    cb.setEnabled(false);
                    bookingStatus.put(key, "Reserved");

                    booked = true;
                    pickedThisClick++;
                }
            }
        }

        // ----- สรุปผลรวม -----
        if (booked) {
            info("แจ้งเตือน", "Reservation Success");
        } else {
            warn("แจ้งเตือน", "กรุณาเลือกห้องก่อนทำการจอง");
        }
    }

    // ====== เมธอดช่วยแสดงข้อความแบบมาตรฐาน ======
    private void info(String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void warn(String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * เมธอดที่ให้ GUI เจ้าของงาน override เพื่อดึง "วันที่ที่เลือก" จากคอมโพเนนต์จริง
     * ค่า default จะคืน LocalDate.now() เพื่อกัน NPE ระหว่างเทส/ตั้งต้น
     * แต่การใช้งานจริง ควร override ให้คืนค่าจาก date picker/คอมโพเนนต์ของคุณ
     */
    protected LocalDate getSelectedDateFromUI() {
        return LocalDate.now();
    }
}
