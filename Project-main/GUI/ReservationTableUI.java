package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.plaf.FontUIResource;
import Service.ReservationManager;
import Model.Reservation;

public class ReservationTableUI extends JFrame {

    // เก็บ checkbox ของแต่ละชั้น (คีย์เป็นชื่อชั้น → ลิสต์เช็คบ็อกซ์ในกริดชั้นนั้น)
    private final Map<String, java.util.List<JCheckBox>> floorCheckBoxes = new HashMap<>();
    // แพแนลหลักที่วางตารางหลาย ๆ ชั้น (ใช้ล้าง/ใส่ใหม่เวลารีเฟรช)
    private final JPanel tablePanel = new JPanel();

    // เก็บ “สถานะจริง” ของแต่ละช่องเวลาในรูปแบบคีย์ "ชั้น-ROOM-HH:mm-HH:mm"
    // ค่า: "Available" | "Reserved" | "Closed" (เพื่อทำสี/ปิดการใช้งานให้ถูกต้อง)
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
            "12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00"
    };

    // ห้องแต่ละชั้น
    private final Map<String, String[]> floorRooms = new HashMap<>();

    // อ้างอิง service ส่วนกลาง
    private final ReservationManager rm = ReservationManager.getInstance();

    // ===================== [เพิ่ม] ค่าจำกัดสิทธิ์ต่อวัน =====================
    // ผู้ใช้ 1 คน จองได้สูงสุดกี่ “ช่วงเวลา” ต่อวัน
    private static final int MAX_SLOTS_PER_USER_PER_DAY = 3;

    public ReservationTableUI() {
        
        UIManager.put("OptionPane.messageFont", new FontUIResource("Tahoma", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont",  new FontUIResource("Tahoma", Font.PLAIN, 13));
        setTitle("การจองของฉัน");
        setTitle("ตารางการจอง");
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // กำหนดห้องของแต่ละชั้น (10 ห้องต่อชั้นตามที่คุณใส่)
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

        // แถวตัวกรอง (วันที่ / ชั้น + ปุ่มรีเซ็ต)
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
        filter.add(lblDate); filter.add(cmbDate);
        filter.add(lblFloor); filter.add(cmbFloor);
        filter.add(btnReset);
        center.add(filter, BorderLayout.NORTH);

        // แพแนลสำหรับตารางหลายชั้น (วางในสกรอลล์)
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(tablePanel);
        center.add(scroll, BorderLayout.CENTER);

        // แถบด้านล่าง: คำอธิบายสี + ปุ่ม “จอง”
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        legend.setOpaque(false);
        legend.add(makeLegend("ว่าง (AVAILABLE)", Color.GREEN));
        legend.add(makeLegend("จองแล้ว (OCCUPIED)", Color.RED));
        legend.add(makeLegend("ไม่เปิดใช้ (CLOSE)", Color.BLACK));
        JButton btnReserve = new JButton("จอง");                 // ปุ่มกดจอง
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

        // เมื่อเปลี่ยนชั้น/วันที่/รีเซ็ตตัวกรอง → สร้างตารางใหม่
        cmbFloor.addActionListener(e -> updateTable.run());
        cmbDate.addActionListener(e -> updateTable.run());
        btnReset.addActionListener(e -> { cmbFloor.setSelectedItem("ทุกชั้น"); });

        // =============== ปุ่ม "จอง" ===============
        btnReserve.addActionListener((ActionEvent e) -> {
            boolean booked = false;                         // ธงว่ามีการจองได้สำเร็จบ้างไหม
            LocalDate date = getSelectedDate();             // วันที่ที่เลือก
            String username = Model.UserSession.getCurrentUsername(); // ผู้ใช้ปัจจุบัน

            // ---------- [เพิ่ม] ตรวจสิทธิ์คงเหลือต่อวัน ----------
            int used = countUserActiveSlotsUI(username, date);            // นับจำนวนช่วงที่ผู้ใช้นี้จอง/ไม่ว่างในวันเดียวกัน
            int remaining = MAX_SLOTS_PER_USER_PER_DAY - used;            // สิทธิ์ที่เหลือ
            if (remaining <= 0) {
                JOptionPane.showMessageDialog(this,
                        "คุณใช้สิทธิ์จองครบ " + MAX_SLOTS_PER_USER_PER_DAY + " ช่วงเวลาของวันที่ "
                                + date.format(DDMMYYYY) + " แล้ว",
                        "เกินโควตา", JOptionPane.WARNING_MESSAGE);
                return; // ไม่มีสิทธิ์เหลือแล้ว
            }
            int pickedThisClick = 0; // จำนวน “ช่วง” ที่จองสำเร็จจากการคลิกครั้งนี้

            // วนทุกเช็คบ็อกซ์ของทุกชั้นที่กำลังแสดง
            for (String floor : floorCheckBoxes.keySet()) {
                java.util.List<JCheckBox> list = floorCheckBoxes.get(floor);
                String[] rooms = floorRooms.get(floor);

                for (int i = 0; i < list.size(); i++) {
                    JCheckBox cb = list.get(i);
                    int roomIdx = i / times.length; // หาค่า index ห้องจากตำแหน่งในลิสต์
                    int timeIdx = i % times.length; // หาค่า index เวลา

                    String room = rooms[roomIdx];
                    String slot = times[timeIdx];
                    String key  = floor + "-" + room + "-" + slot;

                    // เฉพาะช่องที่ผู้ใช้ติ๊กและยังเปิดให้กด
                    if (cb.isSelected() && cb.isEnabled()) {

                        // ---------- [เพิ่ม] กันเกินสิทธิ์คงเหลือ ----------
                        if (pickedThisClick >= remaining) {
                            JOptionPane.showMessageDialog(this,
                                    "จองได้ไม่เกิน " + MAX_SLOTS_PER_USER_PER_DAY + " ช่วงต่อวัน",
                                    "เกินโควตา", JOptionPane.WARNING_MESSAGE);
                            cb.setSelected(false);    // เอาติ๊กออก
                            continue;                 // ข้ามช่องนี้
                        }

                        LocalTime[] tt = parseSlot(slot); // แปลง "HH:mm-HH:mm" → LocalTime

                        // กันชนเวลาซ้อน (ถือว่าทุกสถานะที่ไม่ใช่ Available คือไม่ว่าง)
                        if (!isSlotAvailable(room, date, tt[0], tt[1])) {
                            JOptionPane.showMessageDialog(this,
                                    "ช่วงเวลา " + slot + " ของห้อง " + room + " ถูกจอง/ปิดไว้แล้ว",
                                    "ซ้ำเวลา", JOptionPane.WARNING_MESSAGE);
                            cb.setSelected(false);
                            continue;
                        }

                        // สร้างเรคคอร์ดการจองของผู้ใช้ปัจจุบัน
                        Reservation r = new Reservation(username, room, date, tt[0], tt[1], "Reserved");

                        // เรียก service ให้จอง (แนะนำใช้ addIfAvailable เพื่อให้ฝั่ง service ตรวจความปลอดภัยซ้ำ)
                        boolean ok = ReservationManager.getInstance().addIfAvailable(r);
                        if (!ok) {
                            // เผื่อกรณี service ปฏิเสธ (เช่น มีคนแย่งจองพร้อมกัน / ฝั่ง service ก็มีโควต้า)
                            JOptionPane.showMessageDialog(this,
                                    "ไม่สามารถจองได้ (อาจเกินโควตาหรือเวลาถูกจอง/ปิดแล้ว)",
                                    "จองไม่สำเร็จ", JOptionPane.WARNING_MESSAGE);
                            cb.setSelected(false);
                            continue;
                        }

                        // อัปเดต UI ของช่องนี้ทันทีให้เป็น “จองแล้ว”
                        cb.setBackground(Color.RED);
                        cb.setEnabled(false);
                        bookingStatus.put(key, "Reserved");

                        booked = true;         // มีจองอย่างน้อย 1 รายการ
                        pickedThisClick++;     // ใช้สิทธิ์เพิ่ม 1 ช่อง
                    }
                }
            }

            // แจ้งผลรวม
            if (booked) {
                JOptionPane.showMessageDialog(this, "Reservation Success", "แจ้งเตือน", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "กรุณาเลือกห้องก่อนทำการจอง", "แจ้งเตือน", JOptionPane.WARNING_MESSAGE);
            }
        });

        // =============== subscribe event จาก ReservationManager ===============
        rm.addListener(new ReservationManager.ReservationListener() {
            @Override
            public void onReservationStatusChanged(Reservation r) {
                // ถ้าเหตุการณ์เกิดกับ “วันที่ที่กำลังดู” → โหลดสถานะใหม่ + ทาสีเช็คบ็อกซ์ใหม่
                if (r != null && r.getDate().equals(getSelectedDate())) {
                    preloadBookedStatusFor(getSelectedDate());
                    refreshCheckBoxesColors(); // เร็ว: ไม่ต้องสร้างกริดใหม่ทั้งหมด
                }
            }
            @Override
            public void onReservationChanged(java.util.List<Reservation> all) {
                // กรณีมีการเปลี่ยนแปลงหลายช่อง → รีบิลด์กริดใหม่ทั้งชุดให้ตรงข้อมูลล่าสุด
                preloadBookedStatusFor(getSelectedDate());
                rebuildCurrentFloorTables();
            }
        });

        // แสดงครั้งแรก (สร้างกริดตามตัวกรองเริ่มต้น)
        updateTable.run();
    }

    /* ===== UI helpers ===== */
    // ปุ่มบนแถบนำทางให้โทนเดียวกัน
    private JButton makeTopButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(51,102,255));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Tahoma", Font.BOLD, 16));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(160,45));
        return b;
    }

    // กล่องคำอธิบายสี (legend)
    private JPanel makeLegend(String text, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT)); p.setOpaque(false);
        JPanel box = new JPanel(); box.setBackground(color); box.setPreferredSize(new Dimension(30,20));
        p.add(box); JLabel lbl = new JLabel(text); lbl.setFont(new Font("Tahoma", Font.PLAIN, 14)); p.add(lbl);
        return p;
    }

    // สร้างเซลล์หัวตาราง/ชื่อห้อง
    private JPanel makeCell(String text, Color bg, boolean header) {
        JPanel panel = new JPanel(new BorderLayout()); panel.setBackground(bg);
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Tahoma", header ? Font.BOLD : Font.PLAIN, header ? 14 : 12));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    // เพิ่มกริดของ “ชั้น” ลงใน parent (limitTo3=true → แสดงสูงสุด 3 ห้องเพื่อไม่แน่นเกิน)
    private void addFloorTable(JPanel parent, String floorName, String[] rooms, String[] times, boolean limitTo3) {
        JLabel floorLabel = new JLabel(floorName); floorLabel.setFont(new Font("Tahoma", Font.BOLD, 18));
        floorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(floorLabel);

        int maxRooms = limitTo3 ? Math.min(3, rooms.length) : rooms.length; // จำกัดจำนวนแถว (ห้อง) ที่แสดง

        JPanel grid = new JPanel(new GridLayout(maxRooms+1, times.length+1, 1, 1)); // +1 เฮดเดอร์
        grid.setBackground(Color.BLACK);

        // แถวหัวตาราง (ชื่อคอลัมน์เป็นช่วงเวลา)
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

                // กำหนดสี/เปิดปิดตามสถานะใน bookingStatus
                String st = bookingStatus.getOrDefault(key, "Available");
                cb.setBackground(colorForStatus(st));
                cb.setEnabled(isEnabledForStatus(st));

                grid.add(cb); list.add(cb);
            }
        }

        // เก็บลิสต์เช็คบ็อกซ์ของชั้นนี้ไว้ปรับสีภายหลัง
        floorCheckBoxes.put(floorName, list);
        parent.add(grid);
        parent.add(Box.createVerticalStrut(20)); // เว้นระยะระหว่างชั้น
    }

    /* รีเฟรชสี/เปิดปิดของเช็คบ็อกซ์ทั้งหมดตาม bookingStatus (ไม่สร้างกริดใหม่) */
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

    /* สร้างกริดใหม่ทั้งหมด (ใช้เมื่อข้อมูลเปลี่ยนหลายช่อง/หลายห้อง) */
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

    /* ===== logic helpers ===== */
    private static String slotOf(LocalTime s, LocalTime e) {
        return s.format(HHMM) + "-" + e.format(HHMM);
    }

    // แปลง “HH:mm-HH:mm” → LocalTime[] {start, end}
    private LocalTime[] parseSlot(String slot) {
        String[] p = slot.split("-");
        return new LocalTime[]{ LocalTime.parse(p[0], HHMM), LocalTime.parse(p[1], HHMM) };
    }

    // แปลงค่าจาก combo วันที่ (dd/MM/yyyy) → LocalDate
    private LocalDate getSelectedDate() {
        return LocalDate.parse((String)cmbDate.getSelectedItem(), DDMMYYYY);
    }

    // เดาว่าห้องอยู่ชั้นไหนจากตัวอักษรนำหน้า (S → ชั้น 1, L → ชั้น 2)
    private String getFloorForRoom(String room) {
        if (room != null && room.toUpperCase().startsWith("S")) return "ชั้น 1";
        if (room != null && room.toUpperCase().startsWith("L")) return "ชั้น 2";
        return "ทุกชั้น";
    }

    // ตรวจว่า “ช่วงเวลานั้นในห้องนั้นของวันนั้น” ว่างจริงหรือไม่
    // บล็อกทุกสถานะที่ไม่ใช่ Available (เช่น Reserved/Closed)
    private boolean isSlotAvailable(String room, LocalDate date, LocalTime start, LocalTime end) {
        return ReservationManager.getInstance().getByDate(date).stream()
                .filter(r -> room.equals(r.getRoom()))
                .filter(r -> !"Available".equalsIgnoreCase(r.getStatus()))
                .noneMatch(r -> start.isBefore(r.getEnd()) && r.getStart().isBefore(end));
    }

    // โหลดสถานะจากไฟล์ให้กับทุกช่องของวันนั้น ๆ แล้วเก็บลง bookingStatus
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

    // คืนสีสำหรับสถานะต่าง ๆ (เขียว=ว่าง, แดง=จองแล้ว, ดำ=ปิด)
    private Color colorForStatus(String status) {
        if ("Reserved".equalsIgnoreCase(status)) return Color.RED;
        if ("Closed".equalsIgnoreCase(status))   return Color.BLACK;
        return Color.GREEN;
    }

    // ช่องไหนที่ไม่ใช่ Available ให้ปิดการกด (กันจองซ้อน)
    private boolean isEnabledForStatus(String status) {
        return !"Reserved".equalsIgnoreCase(status) && !"Closed".equalsIgnoreCase(status);
    }

    // ===================== [เพิ่ม] ตัวช่วยนับสิทธิ์ที่ใช้ไปแล้วของผู้ใช้ =====================
    // หมายเหตุ: ถ้า ReservationManager ของคุณมีเมธอด countUserActiveSlots(...) แล้ว
    // คุณสามารถเรียกใช้จาก service ได้เลย แต่อันนี้เผื่อไม่มีจึงนับจาก getByDate ที่นี่
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
