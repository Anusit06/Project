package GUI;

import Model.Reservation;
import Service.ReservationManager;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MyReservationGUI
 *
 * หน้าจอ “การจองของฉัน” สำหรับผู้ใช้ทั่วไป:
 * - แสดงเฉพาะรายการจองของผู้ใช้คนปัจจุบัน ตามวันที่ที่เลือก (วันนี้/พรุ่งนี้)
 * - ซ่อนรายการที่เป็น "Available"
 * - มีปุ่ม “ยกเลิก” สำหรับรายการที่สถานะเป็น "Reserved" เท่านั้น
 * - สมัครเป็นผู้ฟัง ReservationManager เพื่อรีเฟรชหน้าจอเมื่อข้อมูลเปลี่ยน
 */
public class MyReservationGUI extends JFrame implements ReservationManager.ReservationListener {

    /** ตัวจัดการข้อมูลการจอง (ศูนย์กลางอ่าน/เขียนไฟล์) */
    private final ReservationManager rm = ReservationManager.getInstance();

    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> cbDate;
    private final DateTimeFormatter DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter HHMM     = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * สร้างหน้าจอ “การจองของฉัน”
     * - ตั้งค่าปุ่มสลับหน้า
     * - สร้างตาราง + ปุ่ม "ยกเลิก" ในคอลัมน์สุดท้าย
     * - สมัครเป็นผู้ฟังกับ ReservationManager
     * - โหลดข้อมูลครั้งแรกตามวันที่ที่เลือก
     */
    public MyReservationGUI() {

        UIManager.put("OptionPane.messageFont", new FontUIResource("Tahoma", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont",  new FontUIResource("Tahoma", Font.PLAIN, 13));
        setTitle("การจองของฉัน");
        setSize(900, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // ===== Header buttons =====
        JPanel topNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 14));
        JButton bRooms = topButton("ห้อง");
        JButton bSchedule = topButton("ตารางการจอง");
        JButton bMine = topButton("การจองของฉัน");
        topNav.add(bRooms); topNav.add(bSchedule); topNav.add(bMine);
        add(topNav, BorderLayout.NORTH);

        bRooms.addActionListener(e -> { dispose(); new RoomS().setVisible(true); });
        bSchedule.addActionListener(e -> { dispose(); new ReservationTableUI().setVisible(true); });
        bMine.addActionListener(e -> JOptionPane.showMessageDialog(this, "คุณอยู่ที่หน้านี้แล้ว"));

        // ===== Center =====
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(new Color(225, 235, 255));
        add(center, BorderLayout.CENTER);

        JLabel title = new JLabel("การจองของฉัน", SwingConstants.CENTER);
        title.setFont(new Font("Tahoma", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(10, 0, 8, 0));
        center.add(title, BorderLayout.NORTH);

        // Filter bar
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filter.setOpaque(false);

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        cbDate = new JComboBox<>(new String[]{ today.format(DDMMYYYY), tomorrow.format(DDMMYYYY) });
        filter.add(new JLabel("วันที่จอง:"));
        filter.add(cbDate);

        center.add(filter, BorderLayout.WEST);

        // Table
        String[] cols = {"ห้อง", "วันที่จอง", "เวลา", "สถานะ", "การดำเนินการ"};
        model = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return c==4; } };
        table = new JTable(model);
        styleTable(table);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Button column (cancel)
        TableColumn actionCol = table.getColumnModel().getColumn(4);
        actionCol.setCellRenderer(new ButtonRenderer());
        actionCol.setCellEditor(new ButtonEditor(new JCheckBox()));

        // Events
        cbDate.addActionListener(e -> reloadTable());

        // Subscribe to reservation events
        rm.addListener(this);

        // First load
        reloadTable();
    }

    /**
     * สร้างปุ่มหัวหน้า (ปุ่มนำทางบนแถบด้านบน)
     * @param text ข้อความบนปุ่ม
     * @return ปุ่มที่ตั้งสี/ขนาด/ฟอนต์เรียบร้อยแล้ว
     */
    private JButton topButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(51,102,255));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Tahoma", Font.BOLD, 16));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(180, 40));
        return b;
    }

    /**
     * จัดสไตล์ตาราง: ความสูงแถว, ฟอนต์, เส้นตาราง และสีแถวตามสถานะ
     * @param t ตารางที่ต้องการตั้งค่ารูปแบบ
     */
    private void styleTable(JTable t) {
        t.setRowHeight(36);
        t.setFont(new Font("Tahoma", Font.PLAIN, 14));
        t.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 14));
        t.setGridColor(new Color(220,220,220));
        t.setShowGrid(true);

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, selected, focus, row, col);
                if (!selected) {
                    String status = String.valueOf(tbl.getValueAt(row, 3));
                    if ("Reserved".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(255,230,230));
                    } else if ("Closed".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(230,230,230));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                setHorizontalAlignment(col == 0 ? SwingConstants.CENTER : SwingConstants.CENTER);
                return c;
            }
        });
    }

    /**
     * คืนค่าวันที่ที่เลือกจากคอมโบ (แปลง dd/MM/yyyy -> LocalDate)
     * @return วันที่ที่เลือก
     */
    private LocalDate getSelectedDate() {
        return LocalDate.parse((String) cbDate.getSelectedItem(), DDMMYYYY);
    }

    /**
     * โหลดข้อมูล “การจองของฉัน” สำหรับวันที่ที่เลือก
     * - ดึงรายการจาก ReservationManager
     * - แสดงเฉพาะของ username ปัจจุบัน และสถานะที่ไม่ใช่ "Available"
     * - เติมลงตารางใหม่ทั้งหมด
     */
    private void reloadTable() {
        String me;
        try { me = Model.UserSession.getCurrentUsername(); }
        catch (Exception ex) { me = null; }

        model.setRowCount(0);
        if (me == null || me.isBlank()) {
            model.fireTableDataChanged();
            return;
        }

        LocalDate d = getSelectedDate();
        List<Reservation> list = rm.getByDate(d);

        for (Reservation r : list) {
            if (me.equals(r.getUsername())
                    && r.getStatus() != null
                    && !"Available".equalsIgnoreCase(r.getStatus())) {

                model.addRow(new Object[]{
                        r.getRoom(),
                        r.getDate().format(DDMMYYYY),
                        r.getStart().format(HHMM) + "-" + r.getEnd().format(HHMM),
                        r.getStatus(),
                        "ยกเลิก" // ปุ่ม
                });
            }
        }
        model.fireTableDataChanged();
    }

    /* ===== ReservationManager listener ===== */

    /**
     * เมื่อมีการเพิ่มการจองใหม่จากแหล่งข้อมูล (เรียกอัตโนมัติ)
     * ถ้าเป็นของฉันและตรงกับวันที่เลือกอยู่ จะรีโหลดตาราง
     * @param r รายการจองที่ถูกเพิ่ม
     */
    @Override public void onReservationAdded(Reservation r) { if (isMineAndTodayChoice(r)) reloadTable(); }

    /**
     * เมื่อสถานะของรายการใดรายการหนึ่งถูกเปลี่ยน (เรียกอัตโนมัติ)
     * ถ้าเป็นของฉันและตรงกับวันที่เลือกอยู่ จะรีโหลดตาราง
     * @param r รายการที่ถูกเปลี่ยนสถานะ
     */
    @Override public void onReservationStatusChanged(Reservation r) { if (isMineAndTodayChoice(r)) reloadTable(); }

    /**
     * เมื่อข้อมูลทั้งหมดมีการเปลี่ยนแปลง (เรียกอัตโนมัติ)
     * จะรีโหลดตารางใหม่ตามวันที่เลือก
     * @param all รายการจองทั้งหมดล่าสุด
     */
    @Override public void onReservationChanged(List<Reservation> all) { reloadTable(); }

    /**
     * เช็กว่า Reservation ที่เข้ามาเป็นของฉันและเป็นวันที่เดียวกับที่เลือกหรือไม่
     * @param r รายการจอง
     * @return true ถ้าเป็นของฉันและตรงวัน, false ถ้าไม่ใช่
     */
    private boolean isMineAndTodayChoice(Reservation r) {
        String me;
        try { me = Model.UserSession.getCurrentUsername(); } catch (Exception ex) { me = null; }
        return me != null && me.equals(r.getUsername()) && r.getDate().equals(getSelectedDate());
    }

    /**
     * ปิดหน้าจอและถอดตัวเองออกจากการเป็นผู้ฟัง ReservationManager
     */
    @Override
    public void dispose() {
        rm.removeListener(this);
        super.dispose();
    }

    /* ===== Button column ===== */

    /**
     * ตัว renderer ของคอลัมน์ปุ่ม “ยกเลิก”
     * ทำหน้าที่วาดปุ่มในแต่ละแถว และเปิด/ปิดปุ่มตามสถานะ
     */
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        ButtonRenderer() {
            setOpaque(true);
            setText("ยกเลิก");
            setBackground(new Color(230, 230, 255));
            setFont(new Font("Tahoma", Font.BOLD, 12));
        }

        /**
         * วาดปุ่มในเซลล์
         * @param table ตาราง
         * @param value ค่าในเซลล์ (ไม่ใช้)
         * @param isSelected ถูกเลือกหรือไม่
         * @param hasFocus โฟกัสหรือไม่
         * @param row แถว
         * @param column คอลัมน์
         * @return คอมโพเนนต์ปุ่มสำหรับแสดงผลในเซลล์
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,  boolean hasFocus, int row, int column) {
            setEnabled(canCancelRow(row));
            return this;
        }
    }

    /**
     * ตัว editor ของคอลัมน์ปุ่ม “ยกเลิก”
     * รับคลิกและเรียกเมธอด performCancel(row)
     */
    private class ButtonEditor extends DefaultCellEditor {
        private final JButton button = new JButton("ยกเลิก");
        private int editingRow = -1;

        /**
         * สร้าง editor ด้วย JCheckBox (ตามข้อกำหนดของ DefaultCellEditor)
         * @param checkBox ตัวเช็คบ็อกซ์ที่ส่งต่อให้ซุปเปอร์คลาส
         */
        ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button.setOpaque(true);
            button.setBackground(new Color(230, 230, 255));
            button.setFont(new Font("Tahoma", Font.BOLD, 12));
            button.addActionListener(e -> {
                if (editingRow >= 0) performCancel(editingRow);
                fireEditingStopped();
            });
        }

        /**
         * สร้างคอมโพเนนต์สำหรับแก้ไข (ปุ่ม) ในเซลล์
         * @param table ตาราง
         * @param value ค่าในเซลล์ (ไม่ใช้)
         * @param isSelected ถูกเลือกหรือไม่
         * @param row แถวที่กำลังแก้ไข
         * @param column คอลัมน์ที่กำลังแก้ไข
         * @return คอมโพเนนต์ปุ่มสำหรับรับคลิก
         */
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            editingRow = row;
            button.setEnabled(canCancelRow(row));
            return button;
        }
    }

    /**
     * ทำงานเมื่อกด "ยกเลิก" ของแถวนั้น:
     * - ตรวจสอบว่าเป็นสถานะ Reserved เท่านั้นจึงยกเลิกได้
     * - ยืนยันด้วย dialog
     * - สั่ง ReservationManager.setStatus(..., "Available") เพื่อคืนสภาพช่อง
     * @param row เลขแถวในตาราง
     */
    private void performCancel(int row) {
        String room = (String) model.getValueAt(row, 0);
        LocalDate date = getSelectedDate();
        String time = (String) model.getValueAt(row, 2); // "HH:mm-HH:mm"
        String[] p = time.split("-");
        java.time.LocalTime start = java.time.LocalTime.parse(p[0], HHMM);
        java.time.LocalTime end   = java.time.LocalTime.parse(p[1], HHMM);

        // อนุญาตยกเลิกเฉพาะสถานะ Reserved (ถ้าอยากยกเลิก Closed ด้วยให้ลบ if นี้)
        String status = (String) model.getValueAt(row, 3);
        if (!"Reserved".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "ยกเลิกได้เฉพาะรายการที่จองอยู่เท่านั้น", "แจ้งเตือน", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "ยืนยันยกเลิกการจองห้อง " + room + " เวลา " + time + " ?",
                "ยืนยัน", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            ReservationManager.getInstance().setStatus(room, date, start, end, "Available");
            // reloadTable() จะถูกเรียกผ่าน listener อีกทีเมื่อข้อมูลเปลี่ยน
        }
    }

    /**
     * เช็กว่าแถวนั้นสามารถกดยกเลิกได้หรือไม่
     * @param row เลขแถว
     * @return true ถ้าสถานะเป็น Reserved, false ถ้าไม่ใช่
     */
    private boolean canCancelRow(int row) {
        if (row < 0) return false;
        String status = String.valueOf(model.getValueAt(row, 3));
        return "Reserved".equalsIgnoreCase(status);
    }
}
