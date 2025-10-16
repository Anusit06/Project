package GUI;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.plaf.FontUIResource;
import Service.ReservationManager;
import Model.Reservation;
import Util.Action.ReserveButtonHandler;

/**
 * ReservationTableUI
 *
 * หน้าตารางการจอง (สำหรับผู้ใช้งานทั่วไป)
 * - เลือก “วันที่จอง” (วันนี้/พรุ่งนี้)
 * - เลือก “ชั้น” ที่ต้องการดู (ทุกชั้น/ชั้น 1/ชั้น 2)
 * - แสดงตารางห้อง x ช่วงเวลา เป็นช่องติ๊ก (เช็คบ็อกซ์) สีตามสถานะ:
 *     เขียว  = Available (เลือกได้)
 *     แดง    = Reserved  (มีคนจองแล้ว, กดไม่ได้)
 *     ดำ     = Closed    (ปิดใช้งาน, กดไม่ได้)
 * - ปุ่ม “จอง” จะพยายามจองทุกช่องที่ติ๊กอยู่ (ถ้าว่างและไม่เกินโควตาต่อวัน)
 * - รับการอัปเดตแบบเรียลไทม์จาก ReservationManager (เมื่อสถานะเปลี่ยน)
 */
public class ReservationTableUI extends JFrame {

    // เก็บ checkbox ของแต่ละชั้น (คีย์เป็นชื่อชั้น → ลิสต์เช็คบ็อกซ์ในกริดชั้นนั้น)
    private final Map<String, java.util.List<JCheckBox>> floorCheckBoxes = new HashMap<>();
    // แพแนลหลักที่วางตารางหลาย ๆ ชั้น (ใช้ล้าง/ใส่ใหม่เวลารีเฟรช)
    private final JPanel tablePanel = new JPanel();

    /**
     * เก็บสถานะจริงของช่อง (วาดสี/เปิดปิด)
     * คีย์รูปแบบ: "ชั้น-ROOM-HH:mm-HH:mm"
     * ค่า: "Available" | "Reserved" | "Closed"
     */
    private final Map<String, String> bookingStatus = new HashMap<>();

    // ตัวกรองวัน/ชั้น
    private JComboBox<String> cmbDate;
    private JComboBox<String> cmbFloor;

    // ฟอร์แมตเวลา/วันที่
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // รายการช่วงเวลาที่เปิดให้จอง (เปลี่ยน/เพิ่มที่นี่ที่เดียว)
    private final String[] times = {
            "09:00-10:00", "10:00-11:00", "11:00-12:00",
            "12:00-13:00", "13:00-14:00", "14:00-15:00", 
            "15:00-16:00"
    };

    /** รายชื่อห้องแต่ละชั้น */
    private final Map<String, String[]> floorRooms = new HashMap<>();

    /** อ้างอิงศูนย์กลางข้อมูล */
    private final ReservationManager rm = ReservationManager.getInstance();

    /** โควตาจำนวน “ช่วงเวลา” ต่อผู้ใช้/วัน (เช่น 3 ช่วง/วัน) */
    private static final int MAX_SLOTS_PER_USER_PER_DAY = 3;

    /**
     * สร้างหน้าตารางการจอง
     * - ตั้ง UI หลัก (ตัวกรอง, ตาราง, คำอธิบายสี, ปุ่มจอง)
     * - ผูก event กับตัวกรองและปุ่ม
     * - สมัครรับฟังการเปลี่ยนแปลงจาก ReservationManager
     * - โหลดข้อมูลครั้งแรก
     */
    public ReservationTableUI() {
        UIManager.put("OptionPane.messageFont", new FontUIResource("Tahoma", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont",  new FontUIResource("Tahoma", Font.PLAIN, 13));
        setTitle("การจองของฉัน");
        setTitle("ตารางการจอง");
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ห้องของแต่ละชั้น
        floorRooms.put("ชั้น 1", new String[]{"S01","S02","S03","S04","S05","S06","S07","S08","S09","S10"});
        floorRooms.put("ชั้น 2", new String[]{"L01","L02","L03","L04","L05","L06","L07","L08","L09","L10"});

        // ================= UI หลัก =================
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(220, 235, 255));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(mainPanel, BorderLayout.CENTER);

        // แถบบน (ปุ่มนำทาง)
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        nav.setOpaque(false);
        JButton btnRoom = makeTopButton("ห้อง");               // ไปหน้ารายละเอียดห้อง
        JButton btnSchedule = makeTopButton("ตารางการจอง");   // หน้านี้เอง
        JButton btnMyBooking = makeTopButton("การจองของฉัน");  // ไปหน้าการจองของฉัน
        nav.add(btnRoom); nav.add(btnSchedule); nav.add(btnMyBooking);
        mainPanel.add(nav, BorderLayout.NORTH);

        // ลิงก์เปลี่ยนหน้า
        btnRoom.addActionListener(e -> { dispose(); new RoomS().setVisible(true); });
        btnMyBooking.addActionListener(e -> { dispose(); new MyReservationGUI().setVisible(true); });

        // พื้นที่กลาง
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        mainPanel.add(center, BorderLayout.CENTER);

        // ตัวกรอง (วันที่ / ชั้น + ปุ่มรีเซ็ต)
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        filter.setOpaque(false);
        JLabel lblDate = new JLabel("วันที่จอง");           // ป้ายกำกับ
        lblDate.setFont(new Font("Tahoma", Font.PLAIN, 16));
        LocalDate today = LocalDate.now();                   // วันนี้
        LocalDate tomorrow = today.plusDays(1);              // พรุ่งนี้
        cmbDate = new JComboBox<>(new String[]{ today.format(DDMMYYYY), tomorrow.format(DDMMYYYY) }); // ตัวเลือกวันที่
        cmbDate.setFont(new Font("Tahoma", Font.PLAIN, 14));

        JLabel lblFloor = new JLabel("เลือกชั้น");
        lblFloor.setFont(new Font("Tahoma", Font.PLAIN, 16));
        cmbFloor = new JComboBox<>(new String[]{"ทุกชั้น", "ชั้น 1", "ชั้น 2"}); // เลือกกรองชั้น
        cmbFloor.setFont(new Font("Tahoma", Font.PLAIN, 14));

        JButton btnReset = new JButton("รีเซ็ต"); // ปุ่มเคลียร์ตัวกรองชั้น
        btnReset.setFont(new Font("Tahoma", Font.BOLD, 14));

        // ประกอบตัวกรองเข้าด้วยกัน
        filter.add(lblDate); 
        filter.add(cmbDate);
        filter.add(lblFloor); 
        filter.add(cmbFloor);
        filter.add(btnReset);
        center.add(filter, BorderLayout.NORTH);

        // ตารางแสดงผล (หลายชั้น)
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(tablePanel);
        center.add(scroll, BorderLayout.CENTER);

        // แถบล่าง (legend + ปุ่มจอง)
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        legend.setOpaque(false);
        legend.add(makeLegend("ว่าง (AVAILABLE)", Color.GREEN));
        legend.add(makeLegend("จองแล้ว (OCCUPIED)", Color.RED));
        legend.add(makeLegend("ไม่เปิดใช้ (CLOSE)", Color.BLACK));
        JButton btnReserve = new JButton("จอง"); // ปุ่มกดจอง
        btnReserve.setBackground(new Color(51,102,255));
        btnReserve.setForeground(Color.WHITE);
        btnReserve.setFont(new Font("Tahoma", Font.BOLD, 18));
        btnReserve.setPreferredSize(new Dimension(120,40));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT)); right.setOpaque(false);
        right.add(btnReserve);
        bottom.add(legend, BorderLayout.WEST);
        bottom.add(right, BorderLayout.EAST);
        mainPanel.add(bottom, BorderLayout.SOUTH);

        // ============ ฟังก์ชันรีเฟรชตารางตามตัวกรอง ============
        Runnable updateTable = () -> {
            // โหลดสถานะจริงจากไฟล์ให้ตรงกับวันที่ที่เลือก
            preloadBookedStatusFor(getSelectedDate());

            // ล้างกริดเก่า + แผนที่ checkbox ของแต่ละชั้น
            tablePanel.removeAll();
            floorCheckBoxes.clear();

            // สร้างกริดใหม่ตามชั้นที่เลือก (ทุกชั้น/ชั้นเดียว)
            String selFloor = (String) cmbFloor.getSelectedItem();
            if ("ทุกชั้น".equals(selFloor)) {
                for (String floor : floorRooms.keySet()) {
                    addFloorTable(tablePanel, floor, floorRooms.get(floor), times, true); // true = แสดงสูงสุด 3 ห้องต่อชั้นเพื่อไม่ให้แน่นเกิน
                }
            } else {
                addFloorTable(tablePanel, selFloor, floorRooms.get(selFloor), times, false);
            }
            tablePanel.revalidate(); tablePanel.repaint();
        };

        // เปลี่ยนตัวกรอง → รีเฟรช
        cmbFloor.addActionListener(e -> updateTable.run());
        cmbDate.addActionListener(e -> updateTable.run());
        btnReset.addActionListener(e -> { cmbFloor.setSelectedItem("ทุกชั้น"); });

        // =============== ชังก์ชันการจอง ===============
        ReserveButtonHandler handler = new ReserveButtonHandler(this, 
            floorCheckBoxes, 
            floorRooms, 
            times, 
            bookingStatus, 
            MAX_SLOTS_PER_USER_PER_DAY, 
            DDMMYYYY, 
            new ReserveButtonHandler.ReservationOps() {

                @Override
                public int countUserActiveSlotsUI(String username, LocalDate date){
                    return ReservationTableUI.this.countUserActiveSlotsUI(username, date);
                }

                @Override
                public LocalTime[] parseSlot(String slot) {
                    return ReservationTableUI.this.parseSlot(slot);
                }

                @Override
                public boolean isSlotAvailable(String room, LocalDate date, LocalTime start, LocalTime end) {
                    return ReservationTableUI.this.isSlotAvailable(room, date, start, end);
                }
            }
        ){
            @Override
            protected LocalDate getSelectedDateFromUI() {
                return ReservationTableUI.this.getSelectedDate();
            }
        };

        // =============== ปุ่ม "จอง" ===============
        btnReserve.addActionListener(e -> handler.execute()); // เรียกใช้ ฟังก์ชันการจองด้านบน

        // =============== subscribe event จาก ReservationManager ===============
        rm.addListener(new ReservationManager.ReservationListener() {
            @Override
            public void onReservationStatusChanged(Reservation r) {
                if (r != null && r.getDate().equals(getSelectedDate())) {
                    preloadBookedStatusFor(getSelectedDate());
                    refreshCheckBoxesColors(); // อัปเดตสีรวดเร็ว
                }
            }
            @Override
            public void onReservationChanged(java.util.List<Reservation> all) {
                preloadBookedStatusFor(getSelectedDate());
                rebuildCurrentFloorTables(); // กรณีเปลี่ยนหลายช่อง
            }
        });

        // โหลดครั้งแรก
        updateTable.run();
    }

    /* ===== UI helpers ===== */

    /**
     * สร้างปุ่มนำทางด้านบนให้มีรูปแบบเดียวกัน
     * @param text ข้อความบนปุ่ม
     * @return ปุ่มที่จัดสไตล์แล้ว
     */
    private JButton makeTopButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(51,102,255));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Tahoma", Font.BOLD, 16));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(160,45));
        return b;
    }

    /**
     * กล่องคำอธิบายสี (legend)
     * @param text คำอธิบาย
     * @param color สีตัวอย่าง
     * @return แพแนลสำหรับแสดง legend 1 รายการ
     */
    private JPanel makeLegend(String text, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT)); p.setOpaque(false);
        JPanel box = new JPanel(); box.setBackground(color); box.setPreferredSize(new Dimension(30,20));
        p.add(box); JLabel lbl = new JLabel(text); lbl.setFont(new Font("Tahoma", Font.PLAIN, 14)); p.add(lbl);
        return p;
    }

    /**
     * สร้างเซลล์หัวตาราง/ชื่อห้อง
     * @param text ข้อความในเซลล์
     * @param bg สีพื้นหลัง
     * @param header true=หัวตาราง(ตัวหนา), false=ปกติ
     * @return แพแนลเซลล์พร้อมข้อความ
     */
    private JPanel makeCell(String text, Color bg, boolean header) {
        JPanel panel = new JPanel(new BorderLayout()); panel.setBackground(bg);
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Tahoma", header ? Font.BOLD : Font.PLAIN, header ? 14 : 12));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    /**
     * เพิ่มกริดของ “ชั้น” ลงใน parent
     * @param parent แพแนลแม่ที่ใส่กริด
     * @param floorName ชื่อชั้น (เช่น "ชั้น 1")
     * @param rooms รายชื่อห้องในชั้น
     * @param times รายการช่วงเวลา
     * @param limitTo3 true=แสดงสูงสุด 3 ห้อง/ชั้น (ให้กระชับ), false=แสดงทั้งหมด
     */
    private void addFloorTable(JPanel parent, String floorName, String[] rooms, String[] times, boolean limitTo3) {
        JLabel floorLabel = new JLabel(floorName); floorLabel.setFont(new Font("Tahoma", Font.BOLD, 18));
        floorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(floorLabel);

        int maxRooms = limitTo3 ? Math.min(3, rooms.length) : rooms.length; // จำกัดจำนวนแถว (ห้อง) ที่แสดง

        JPanel grid = new JPanel(new GridLayout(maxRooms+1, times.length+1, 1, 1)); // +1 เฮดเดอร์
        grid.setBackground(Color.BLACK);

        // หัวตาราง
        grid.add(makeCell("ห้อง", Color.LIGHT_GRAY, true));
        for (String t : times) grid.add(makeCell(t, Color.LIGHT_GRAY, true));

        java.util.List<JCheckBox> list = new ArrayList<>();

        for (int i = 0; i < maxRooms; i++) {
            String room = rooms[i];
            grid.add(makeCell(room, Color.LIGHT_GRAY, true)); // คอลัมน์ชื่อห้อง

            for (String t : times) {
                String key = floorName + "-" + room + "-" + t; // คีย์สถานะ

                JCheckBox cb = new JCheckBox();
                cb.setHorizontalAlignment(SwingConstants.CENTER);

                // ตั้งค่าสี/เปิดปิดตามสถานะ
                String st = bookingStatus.getOrDefault(key, "Available");
                cb.setBackground(colorForStatus(st));
                cb.setEnabled(isEnabledForStatus(st));

                grid.add(cb); list.add(cb);
            }
        }

        floorCheckBoxes.put(floorName, list);
        parent.add(grid);
        parent.add(Box.createVerticalStrut(20));
    }

    /**
     * รีเฟรชสี/สถานะของเช็คบ็อกซ์ทั้งหมดจากข้อมูลใน {@code bookingStatus}
     * (ไม่สร้างกริดใหม่ เพื่อความเร็ว)
     */
    private void refreshCheckBoxesColors() {
        for (Map.Entry<String, java.util.List<JCheckBox>> e : floorCheckBoxes.entrySet()) {
            String floor = e.getKey();
            java.util.List<JCheckBox> list = e.getValue();
            String[] rooms = floorRooms.getOrDefault(floor, new String[0]);

            for (int i = 0; i < list.size(); i++) {
                JCheckBox cb = list.get(i);
                int roomIdx = i / times.length;
                int timeIdx = i % times.length;
                if (roomIdx >= rooms.length) continue;

                String room = rooms[roomIdx];
                String slot = times[timeIdx];
                String key  = floor + "-" + room + "-" + slot;

                String st = bookingStatus.getOrDefault(key, "Available");
                cb.setEnabled(isEnabledForStatus(st));
                cb.setSelected(false);
                cb.setBackground(colorForStatus(st));
            }
        }
        tablePanel.revalidate();
        tablePanel.repaint();
    }

    /**
     * สร้างกริดใหม่ทั้งหมด (ใช้เมื่อข้อมูลเปลี่ยนหลายช่องพร้อมกัน)
     */
    private void rebuildCurrentFloorTables() {
        tablePanel.removeAll();
        floorCheckBoxes.clear();

        String selFloor = (String) cmbFloor.getSelectedItem();
        if ("ทุกชั้น".equals(selFloor)) {
            for (String floor : floorRooms.keySet()) {
                addFloorTable(tablePanel, floor, floorRooms.get(floor), times, true);
            }
        } else {
            addFloorTable(tablePanel, selFloor, floorRooms.get(selFloor), times, false);
        }
        tablePanel.revalidate();
        tablePanel.repaint();
    }

    /**
     * รวม start/end ให้เป็นข้อความช่วงเวลา "HH:mm-HH:mm"
     * @param s เวลาเริ่ม
     * @param e เวลาสิ้นสุด
     * @return ข้อความช่วงเวลา
     */
    private static String slotOf(LocalTime s, LocalTime e) {
        return s.format(HHMM) + "-" + e.format(HHMM);
    }

    /**
     * แปลงข้อความช่วงเวลา "HH:mm-HH:mm" เป็น LocalTime[] {start, end}
     * @param slot ข้อความช่วงเวลา
     * @return อาร์เรย์เวลาขนาด 2 ช่อง (0=start, 1=end)
     */
    private LocalTime[] parseSlot(String slot) {
        String[] p = slot.split("-");
        return new LocalTime[]{ LocalTime.parse(p[0], HHMM), LocalTime.parse(p[1], HHMM) };
    }

    /**
     * แปลงค่าจากคอมโบวันที่ (dd/MM/yyyy) เป็น LocalDate
     * @return วันที่ที่เลือก
     */
    private LocalDate getSelectedDate() {
        return LocalDate.parse((String)cmbDate.getSelectedItem(), DDMMYYYY);
    }

    /**
     * เดาว่าห้องอยู่ชั้นไหนจากตัวอักษรนำหน้า
     * @param room รหัสห้อง (เช่น S01/L02)
     * @return "ชั้น 1", "ชั้น 2" หรือ "ทุกชั้น" ถ้าไม่รู้จัก
     */
    private String getFloorForRoom(String room) {
        if (room != null && room.toUpperCase().startsWith("S")) return "ชั้น 1";
        if (room != null && room.toUpperCase().startsWith("L")) return "ชั้น 2";
        return "ทุกชั้น";
    }

   /**
     * ตรวจว่าช่วงเวลานี้ “ว่างจริง” หรือไม่ โดยถือว่า:
     * - ทุกสถานะที่ไม่ใช่ "Available" = ไม่ว่าง (กันทับซ้อน)
     * @param room ห้อง
     * @param date วันที่
     * @param start เวลาเริ่ม
     * @param end เวลาสิ้นสุด
     * @return true ถ้าว่าง, false ถ้าไม่ว่าง
     */
    private boolean isSlotAvailable(String room, LocalDate date, LocalTime start, LocalTime end) {
        return ReservationManager.getInstance().getByDate(date).stream()
                .filter(r -> room.equals(r.getRoom()))
                .filter(r -> !"Available".equalsIgnoreCase(r.getStatus()))
                .noneMatch(r -> start.isBefore(r.getEnd()) && r.getStart().isBefore(end));
    }

    /**
     * โหลดสถานะจาก ReservationManager สำหรับวันที่ที่กำหนด
     * แล้วบันทึกเป็นแผนที่ bookingStatus เพื่อใช้ทาสี/ปิดเช็คบ็อกซ์
     * @param date วันที่ที่ต้องการโหลด
     */
    private void preloadBookedStatusFor(LocalDate date) {
        bookingStatus.clear();
        ReservationManager.getInstance().getByDate(date).forEach(r -> {
            String floor = getFloorForRoom(r.getRoom());
            String slot  = slotOf(r.getStart(), r.getEnd());
            String key   = floor + "-" + r.getRoom() + "-" + slot;

            String st = (r.getStatus() == null) ? "Available" : r.getStatus();
            bookingStatus.put(key, st);
        });
    }

    /**
     * แปลงสถานะเป็น “สี”
     * @param status สถานะ (Available/Reserved/Closed)
     * @return สีที่ใช้แสดงในเช็คบ็อกซ์
     */
    private Color colorForStatus(String status) {
        if ("Reserved".equalsIgnoreCase(status)) return Color.RED;
        if ("Closed".equalsIgnoreCase(status))   return Color.BLACK;
        return Color.GREEN;
    }

    /**
     * เช็คว่าช่องนี้ “กดได้ไหม”
     * @param status สถานะปัจจุบันของช่อง
     * @return true ถ้ากดได้ (Available), false ถ้าห้ามกด (Reserved/Closed)
     */
    private boolean isEnabledForStatus(String status) {
        return !"Reserved".equalsIgnoreCase(status) && !"Closed".equalsIgnoreCase(status);
    }

    /**
     * นับจำนวน “ช่วงเวลา” ของผู้ใช้ในวันนั้นที่ไม่ใช่ Available
     * ใช้สำหรับเช็คโควตา/วันก่อนจอง
     * @param username ชื่อผู้ใช้ปัจจุบัน
     * @param date วันที่
     * @return จำนวนช่วงเวลาที่ถูกใช้งานแล้วในวันนั้น
     */
    private int countUserActiveSlotsUI(String username, LocalDate date) {
        if (username == null || username.isBlank()) return 0;
        int c = 0;
        for (Reservation r : rm.getByDate(date)) {
            if (username.equals(r.getUsername())
                    && r.getStatus() != null
                    && !"Available".equalsIgnoreCase(r.getStatus())) {
                c++;
            }
        }
        return c;
    }
}
