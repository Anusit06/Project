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
import Util.Action.CancelButtonColumn;

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
        cbDate = new JComboBox<>(new String[]{ today.format(DDMMYYYY), tomorrow.format(DDMMYYYY),"ทั้งหมด (วันนี้+พรุ่งนี้)" });
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
        new CancelButtonColumn(table, 4, this::canCancelRow, this::performCancel);

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

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        for (LocalDate d : new LocalDate[]{ today, tomorrow }){
            for (Reservation r : rm.getByDate(d)) {
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
        }
        // (ทางเลือก) เรียงให้สวย: วันที่ > เวลา > ห้อง
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(1, (a,b) -> LocalDate.parse(a.toString(), DDMMYYYY)
                .compareTo(LocalDate.parse(b.toString(), DDMMYYYY)));
        sorter.setComparator(2, (a,b) -> a.toString().compareTo(b.toString()));
        sorter.setSortKeys(java.util.List.of(
            new RowSorter.SortKey(1, SortOrder.ASCENDING),
            new RowSorter.SortKey(2, SortOrder.ASCENDING),
            new RowSorter.SortKey(0, SortOrder.ASCENDING)
        ));
        table.setRowSorter(sorter);
        
        model.fireTableDataChanged();
    }
  
    /* ===== ReservationManager listener ===== */

    /**
     * เมื่อมีการเพิ่มการจองใหม่จากแหล่งข้อมูล (เรียกอัตโนมัติ)
     * ถ้าเป็นของฉันและตรงกับวันที่เลือกอยู่ จะรีโหลดตาราง
     * @param r รายการจองที่ถูกเพิ่ม
     */
    @Override public void onReservationAdded(Reservation r) { SwingUtilities.invokeLater(this::reloadTable); }

    /**
     * เมื่อสถานะของรายการใดรายการหนึ่งถูกเปลี่ยน (เรียกอัตโนมัติ)
     * ถ้าเป็นของฉันและตรงกับวันที่เลือกอยู่ จะรีโหลดตาราง
     * @param r รายการที่ถูกเปลี่ยนสถานะ
     */
    @Override public void onReservationStatusChanged(Reservation r) { SwingUtilities.invokeLater(this::reloadTable); }

    /**
     * เมื่อข้อมูลทั้งหมดมีการเปลี่ยนแปลง (เรียกอัตโนมัติ)
     * จะรีโหลดตารางใหม่ตามวันที่เลือก
     * @param all รายการจองทั้งหมดล่าสุด
     */
    @Override public void onReservationChanged(List<Reservation> all) { SwingUtilities.invokeLater(this::reloadTable); }

    /**
     * ปิดหน้าจอและถอดตัวเองออกจากการเป็นผู้ฟัง ReservationManager
     */
    @Override
    public void dispose() {
        rm.removeListener(this);
        super.dispose();
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
        LocalDate date = LocalDate.parse((String) model.getValueAt(row, 1), DDMMYYYY);
        String time = (String) model.getValueAt(row, 2); // "HH:mm-HH:mm"

        String[] p = time.split("-");
        if (p.length != 2) {
            JOptionPane.showConfirmDialog(this, JOptionPane.WARNING_MESSAGE);
            return;
        }

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