package GUI.Admin;
import Service.ReservationManager;
import Model.Reservation;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.List;

/**
 * StatusGUI (มุมมองผู้ดูแลระบบ)
 * หน้าจอแสดง/แก้ไขสถานะการจองแบบเป็นตาราง แยกทุกชั้น / ชั้น 1 / ชั้น 2
 * มีปุ่มเปลี่ยนสถานะ (Available/Reserved/Closed/Reset) และดึงข้อมูลจริงจาก ReservationManager
 * การอัปเดต UI เมื่อข้อมูลเปลี่ยนจะผ่าน ReservationManager.ReservationListener
 */
public class StatusGUI extends JFrame implements ReservationManager.ReservationListener {

    private JTable tableFloor1, tableFloor2, tableAll;
    private DefaultTableModel model1, model2, modelAll;
    private JPanel sectionFloor1, sectionFloor2, sectionAll;
    private JComboBox<String> cbDate, cbFloor;

    private static final DateTimeFormatter DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HHMM     = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * สร้างหน้า Status (ADMIN)
     * - ตั้งค่าหน้าจอ/ส่วนประกอบ
     * - ผูกปุ่มและตัวกรอง
     * - สมัครฟังเหตุการณ์จาก ReservationManager
     * - โหลดสถานะตามวันที่เลือกขึ้นตาราง
     */
    public StatusGUI() {
        installThaiUIFont(new Font("Tahoma", Font.PLAIN, 14));

        setTitle("ตารางการจอง (ADMIN)");
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel sidebar = buildSidebar();

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(240, 245, 255));

        JLabel title = new JLabel("ตารางสถานะการจอง (ADMIN)", SwingConstants.CENTER);
        title.setFont(new Font("Tahoma", Font.BOLD, 30));
        title.setBorder(BorderFactory.createEmptyBorder(20,0,20,0));

        // filter
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        filter.setBackground(new Color(240,245,255));
        JLabel lblDate = new JLabel("วันที่จอง:");
        LocalDate today = LocalDate.now(), tomorrow = today.plusDays(1);
        cbDate = new JComboBox<>(new String[]{ today.format(DDMMYYYY), tomorrow.format(DDMMYYYY) });

        JLabel lblFloor = new JLabel("เลือกชั้น:");
        cbFloor = new JComboBox<>(new String[]{"ทุกชั้น","ชั้น 1","ชั้น 2"});

        JButton btnResetFilter = new JButton("รีเซ็ตตัวกรอง");
        btnResetFilter.addActionListener(e -> {
            cbDate.setSelectedIndex(0);
            cbFloor.setSelectedIndex(0);
            updateFloorVisibility();
            preloadFromManagerForSelectedDate();
        });

        filter.add(lblDate); filter.add(cbDate);
        filter.add(lblFloor); filter.add(cbFloor);
        filter.add(btnResetFilter);

        JPanel north = new JPanel();
        north.setBackground(new Color(240,245,255));
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(title); north.add(filter);
        content.add(north, BorderLayout.NORTH);

        sectionAll    = buildAllSection("ทุกชั้น");
        sectionFloor1 = buildFloorSection("ชั้น 1", true);
        sectionFloor2 = buildFloorSection("ชั้น 2", false);

        sectionFloor1.setVisible(false);
        sectionFloor2.setVisible(false);

        JPanel center = new JPanel();
        center.setBackground(Color.WHITE);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(sectionAll); center.add(Box.createVerticalStrut(20));
        center.add(sectionFloor1); center.add(Box.createVerticalStrut(20));
        center.add(sectionFloor2);
        content.add(center, BorderLayout.CENTER);

        // action buttons
        JPanel action = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        action.setBackground(new Color(240,245,255));
        JButton btnAvailable = makeActionButton("Available", Color.GREEN, Color.BLACK);
        JButton btnReserved  = makeActionButton("Reserved", Color.RED, Color.WHITE);
        JButton btnClosed    = makeActionButton("Closed", Color.BLACK, Color.WHITE);
        JButton btnReset     = makeActionButton("Reset", new Color(200,200,255), Color.BLACK);
        btnAvailable.addActionListener(e -> setStatus("Available"));
        btnReserved.addActionListener(e -> setStatus("Reserved"));
        btnClosed.addActionListener(e -> setStatus("Closed"));
        btnReset.addActionListener(e -> resetStatus());
        action.add(btnAvailable); action.add(btnReserved); action.add(btnClosed); action.add(btnReset);
        content.add(action, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        cbFloor.addActionListener(e -> updateFloorVisibility());

        // Link กับ Manager
        ReservationManager.getInstance().addListener(this);
        preloadFromManagerForSelectedDate();
        cbDate.addActionListener(e -> preloadFromManagerForSelectedDate());
    }

    /**
     * แสดง/ซ่อนเซกชันตามชั้นที่เลือกในตัวกรอง
     * และโหลดสถานะจริงของวันที่ที่เลือก มาทาทับในตาราง
     */
    private void updateFloorVisibility() {
    String sel = (String) cbFloor.getSelectedItem();
    if (sel == null) sel = "ทุกชั้น";

    boolean showAll = "ทุกชั้น".equals(sel);
    boolean showF1  = "ชั้น 1".equals(sel);
    boolean showF2  = "ชั้น 2".equals(sel);

    if (sectionAll    != null) sectionAll.setVisible(showAll);
    if (sectionFloor1 != null) sectionFloor1.setVisible(showF1);
    if (sectionFloor2 != null) sectionFloor2.setVisible(showF2);

    // รีเพนต์ + โหลดสถานะจริงของวันที่ที่เลือก ทาทับลงตารางที่มองเห็น
    preloadFromManagerForSelectedDate();

    // เผื่อเค้าโครงเปลี่ยนให้จัดใหม่
    revalidate();
    repaint();
}
    /**
     * ปิดหน้าจอนี้ และเลิกเป็นผู้ฟังของ ReservationManager
     */
    @Override public void dispose() {
        ReservationManager.getInstance().removeListener(this);
        super.dispose();
    }

    /**
     * สร้างแถบเมนูด้านซ้าย (ปุ่ม Home/Status/History/Back)
     * @return แผงเมนูด้านซ้าย
     */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(200, 750));

        JLabel logo = new JLabel("ADMIN");
        logo.setFont(new Font("Tahoma", Font.BOLD, 22));
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 20));
        header.setBackground(Color.WHITE);
        header.add(logo);
        sidebar.add(header, BorderLayout.NORTH);

        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Color.WHITE);

        JButton bHome = makeSideButton("Home");
        JButton bStatus = makeSideButton("Status");
        JButton bHistory = makeSideButton("History");
        JButton bBack = makeSideButton("BACK");

        menu.add(bHome); menu.add(Box.createVerticalStrut(15));
        menu.add(bStatus); menu.add(Box.createVerticalStrut(15));
        menu.add(bHistory);
        menu.add(Box.createVerticalGlue());
        menu.add(bBack); menu.add(Box.createVerticalStrut(12));

        bHome.addActionListener(e -> { new AdminHomeGUI().setVisible(true); dispose(); });
        bStatus.addActionListener(e -> JOptionPane.showMessageDialog(this, "You are already on Status page"));
        bHistory.addActionListener(e -> { new HistoryGUI().setVisible(true); dispose(); });
        bBack.addActionListener(e -> { new GUI.RoomS().setVisible(true); dispose(); });

        sidebar.add(menu, BorderLayout.CENTER);
        return sidebar;
    }

    /**
     * สร้างปุ่มเมนูด้านข้าง
     * @param text ชื่อปุ่ม
     * @return ปุ่มพร้อมขนาดและฟอนต์มาตรฐาน
     */
    private JButton makeSideButton(String text) {
        JButton b = new JButton(text);
        Dimension size = new Dimension(170, 52);
        b.setPreferredSize(size); b.setMaximumSize(size); b.setMinimumSize(size);
        b.setFont(new Font("Tahoma", Font.PLAIN, 16));
        b.setFocusPainted(false);
        return b;
    }

    /**
     * สร้างส่วนแสดงตารางรวมทุกชั้น
     * @param title ชื่อหัวข้อของเซกชัน
     * @return แผง (panel) ที่มีตารางรวมทุกชั้น
     */
    private JPanel buildAllSection(String title) {
        String[] cols = {"ห้อง", "09:00-10:00", "10:00-11:00", "11:00-12:00",
                "12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00"};

        Object[][] dataAll = {
                {"S01","Available","Available","Available","Available","Available","Available","Available"},
                {"S02","Available","Available","Available","Available","Available","Available","Available"},
                {"S03","Available","Available","Available","Available","Available","Available","Available"},
                {"S04","Available","Available","Available","Available","Available","Available","Available"},
                {"S05","Available","Available","Available","Available","Available","Available","Available"},
                {"S06","Available","Available","Available","Available","Available","Available","Available"},
                {"S07","Available","Available","Available","Available","Available","Available","Available"},
                {"S08","Available","Available","Available","Available","Available","Available","Available"},
                {"S09","Available","Available","Available","Available","Available","Available","Available"},
                {"S10","Available","Available","Available","Available","Available","Available","Available"},
                {"L01","Available","Available","Available","Available","Available","Available","Available"},
                {"L02","Available","Available","Available","Available","Available","Available","Available"},
                {"L03","Available","Available","Available","Available","Available","Available","Available"},
                {"L04","Available","Available","Available","Available","Available","Available","Available"},
                {"L05","Available","Available","Available","Available","Available","Available","Available"},
                {"L06","Available","Available","Available","Available","Available","Available","Available"},
                {"L07","Available","Available","Available","Available","Available","Available","Available"},
                {"L08","Available","Available","Available","Available","Available","Available","Available"},
                {"L09","Available","Available","Available","Available","Available","Available","Available"},
                {"L10","Available","Available","Available","Available","Available","Available","Available"}
        };

        modelAll = new DefaultTableModel(dataAll, cols){ @Override public boolean isCellEditable(int r,int c){return false;}};
        tableAll = new JTable(modelAll);
        setupTable(tableAll);

        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(new Font("Tahoma", Font.BOLD, 18));
        lbl.setBorder(BorderFactory.createEmptyBorder(8,0,8,0));

        JScrollPane sp = new JScrollPane(tableAll); sp.setPreferredSize(prefSizeOf(tableAll));

        JPanel wrap = new JPanel(); wrap.setBackground(Color.WHITE);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.add(lbl); wrap.add(sp);
        return wrap;
    }

    /**
     * สร้างส่วนแสดงตารางของแต่ละชั้น
     * @param title ชื่อหัวข้อของเซกชัน
     * @param floor1 true = ชั้น 1, false = ชั้น 2
     * @return แผง (panel) ที่มีตารางของชั้นนั้น
     */
    private JPanel buildFloorSection(String title, boolean floor1) {
        String[] cols = {"ห้อง", "09:00-10:00", "10:00-11:00", "11:00-12:00",
                "12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00"};

        Object[][] data1 = {
                {"S01","Available","Available","Available","Available","Available","Available","Available"},
                {"S02","Available","Available","Available","Available","Available","Available","Available"},
                {"S03","Available","Available","Available","Available","Available","Available","Available"},
                {"S04","Available","Available","Available","Available","Available","Available","Available"},
                {"S05","Available","Available","Available","Available","Available","Available","Available"},
                {"S06","Available","Available","Available","Available","Available","Available","Available"},
                {"S07","Available","Available","Available","Available","Available","Available","Available"},
                {"S08","Available","Available","Available","Available","Available","Available","Available"},
                {"S09","Available","Available","Available","Available","Available","Available","Available"},
                {"S10","Available","Available","Available","Available","Available","Available","Available"},
        };
        Object[][] data2 = {
                {"L01","Available","Available","Available","Available","Available","Available","Available"},
                {"L02","Available","Available","Available","Available","Available","Available","Available"},
                {"L03","Available","Available","Available","Available","Available","Available","Available"},
                {"L04","Available","Available","Available","Available","Available","Available","Available"},
                {"L05","Available","Available","Available","Available","Available","Available","Available"},
                {"L06","Available","Available","Available","Available","Available","Available","Available"},
                {"L07","Available","Available","Available","Available","Available","Available","Available"},
                {"L08","Available","Available","Available","Available","Available","Available","Available"},
                {"L09","Available","Available","Available","Available","Available","Available","Available"},
                {"L10","Available","Available","Available","Available","Available","Available","Available"},
        };

        if (floor1) {
            model1 = new DefaultTableModel(data1, cols){ @Override public boolean isCellEditable(int r,int c){return false;}};
            tableFloor1 = new JTable(model1); setupTable(tableFloor1);
        } else {
            model2 = new DefaultTableModel(data2, cols){ @Override public boolean isCellEditable(int r,int c){return false;}};
            tableFloor2 = new JTable(model2); setupTable(tableFloor2);
        }

        JTable t = floor1 ? tableFloor1 : tableFloor2;

        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(new Font("Tahoma", Font.BOLD, 18));
        lbl.setBorder(BorderFactory.createEmptyBorder(8,0,8,0));

        JScrollPane sp = new JScrollPane(t); sp.setPreferredSize(prefSizeOf(t));

        JPanel wrap = new JPanel(); wrap.setBackground(Color.WHITE);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.add(lbl); wrap.add(sp);
        return wrap;
    }

    /**
     * ตั้งค่ารูปแบบตาราง เช่น ความสูงแถว สีพื้นหลังตามสถานะ หัวตาราง ฯลฯ
     * @param t ตารางที่ต้องการตั้งค่า renderer และคุณสมบัติ
     */
   private void setupTable(JTable t) {
    t.setRowHeight(40);
    t.setShowGrid(true);
    t.setGridColor(new Color(220,220,220));
    t.setFont(new Font("Tahoma", Font.PLAIN, 14));
    t.getTableHeader().setReorderingAllowed(false);
    t.getTableHeader().setResizingAllowed(false);
    t.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 14));

    // ให้เลือกเซลล์ได้ตามเดิม เพื่อใช้กับปุ่มเปลี่ยนสถานะ
    t.setCellSelectionEnabled(true);
    t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(
                JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            Component comp = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
            String s = String.valueOf(value);

            if (column == 0) { // ★ แก้เฉพาะคอลัมน์ "ห้อง" (S01, S02, ...)
                comp.setBackground(new Color(210,210,210));
                comp.setForeground(Color.BLACK);
                comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                ((JLabel) comp).setHorizontalAlignment(SwingConstants.CENTER);

                // ★ บังคับให้ไม่มี focus border/ระยะขอบหนา ๆ ที่ทำให้ S01 ดูแปลก
                ((JComponent) comp).setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            } else {          // คอลัมน์ช่วงเวลา
                switch (s) {
                    case "Available": comp.setBackground(Color.GREEN); comp.setForeground(Color.BLACK); break;
                    case "Reserved":  comp.setBackground(Color.RED);   comp.setForeground(Color.WHITE); break;
                    case "Closed":    comp.setBackground(Color.BLACK); comp.setForeground(Color.WHITE); break;
                    default:          comp.setBackground(Color.WHITE); comp.setForeground(Color.BLACK);
                }
                ((JLabel) comp).setHorizontalAlignment(SwingConstants.CENTER);
                ((JComponent) comp).setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            }
            return comp;
        }
    });
}


    /**
     * คำนวณขนาดแนะนำของ JScrollPane ที่ห่อ JTable
     * @param t ตาราง
     * @return ขนาดที่พอดีกับจำนวนคอลัมน์และแถวปัจจุบัน
     */
    private Dimension prefSizeOf(JTable t) {
        int w = t.getColumnModel().getTotalColumnWidth();
        int h = t.getRowHeight() * t.getRowCount() + t.getTableHeader().getPreferredSize().height + 2;
        return new Dimension(w + 2, h + 2);
    }

    /**
     * สร้างปุ่มเปลี่ยนสถานะด้านล่าง
     * @param text ข้อความปุ่ม
     * @param bg สีพื้นหลัง
     * @param fg สีตัวอักษร
     * @return ปุ่มพร้อมสไตล์
     */
    private JButton makeActionButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("Tahoma", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(150, 45));
        b.setFocusPainted(false);
        return b;
    }

    /**
     * เปลี่ยนสถานะช่องที่เลือกในตาราง (ต้องเลือก cell ภายในช่วงเวลา ไม่ใช่คอลัมน์ชื่อห้อง)
     * จะสั่งให้ ReservationManager เซฟและบรอดแคสต์ และอัปเดต cell ปัจจุบันทันที
     * @param status สถานะใหม่ เช่น Available, Reserved, Closed
     */
    private void setStatus(String status) {
        JTable active = tableFloor1 != null && tableFloor1.getSelectedRow()>=0 ? tableFloor1 :
                        tableFloor2 != null && tableFloor2.getSelectedRow()>=0 ? tableFloor2 :
                        tableAll    != null && tableAll.getSelectedRow()>=0    ? tableAll    : null;
        if (active == null) return;

        int r = active.getSelectedRow();
        int c = active.getSelectedColumn();
        if (r < 0 || c <= 0) return;

        String room = String.valueOf(active.getValueAt(r, 0));
        String slot = active.getColumnName(c); // "HH:mm-HH:mm"
        LocalTime[] tt = parseSlot(slot);
        LocalDate date = getSelectedDate();

        // เปลี่ยนสถานะจริง (persist + broadcast)
        ReservationManager.getInstance().setStatus(room, date, tt[0], tt[1], status);

        // feedback ทันทีในตารางนี้ (แม้จะได้อัปเดตจาก listener อีกครั้ง)
        ((DefaultTableModel) active.getModel()).setValueAt(status, r, c);
        active.repaint();
    }

    /**
     * รีเซ็ตสถานะทั้งตารางของวันที่ที่เลือก ให้เป็น "Available"
     * จะสั่งให้ ReservationManager เซฟทับทุกช่อง และอัปเดตตารางทันที
     */
    private void resetStatus() {
        JTable[] tables = new JTable[]{ tableFloor1, tableFloor2, tableAll };
        LocalDate date = getSelectedDate();

        for (JTable t : tables) {
            if (t == null) continue;
            for (int r = 0; r < t.getRowCount(); r++) {
                String room = String.valueOf(t.getValueAt(r, 0));
                for (int c = 1; c < t.getColumnCount(); c++) {
                    String slot = t.getColumnName(c);
                    LocalTime[] tt = parseSlot(slot);
                    ReservationManager.getInstance().setStatus(room, date, tt[0], tt[1], "Available");
                    ((DefaultTableModel) t.getModel()).setValueAt("Available", r, c);
                }
            }
            t.repaint();
        }
    }

    /**
     * ติดตั้งฟอนต์เริ่มต้นของ Swing ให้เป็นฟอนต์ไทยที่กำหนด
     * @param f ฟอนต์ที่ต้องการใช้เป็นค่ามาตรฐาน
     */
    public static void installThaiUIFont(Font f) {
        FontUIResource fui = new FontUIResource(f);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()){
            Object k = keys.nextElement();
            Object v = UIManager.get(k);
            if (v instanceof FontUIResource) UIManager.put(k, fui);
        }
    }

    /**
     * แปลงข้อความช่วงเวลา "HH:mm-HH:mm" เป็น LocalTime[2] {เริ่ม, จบ}
     * @param slot ข้อความช่วงเวลา เช่น "09:00-10:00"
     * @return อาร์เรย์เวลา 2 ตัว [เริ่ม, จบ]
     */
    private LocalTime[] parseSlot(String slot) {
        String[] p = slot.split("-");
        return new LocalTime[] {
            LocalTime.parse(p[0], HHMM),
            LocalTime.parse(p[1], HHMM)
        };
    }

    /**
     * หาว่าห้องอยู่ชั้นใดจาก prefix ชื่อห้อง
     * @param room ชื่อห้อง เช่น S01, L02
     * @return "ชั้น 1", "ชั้น 2" หรือ "ทุกชั้น" หากไม่เข้าเงื่อนไข
     */
    private String getFloorForRoom(String room){
        if (room != null && room.toUpperCase().startsWith("S")) return "ชั้น 1";
        if (room != null && room.toUpperCase().startsWith("L")) return "ชั้น 2";
        return "ทุกชั้น";
    }

     /**
     * คืนค่่าวันที่ที่ถูกเลือกในคอมโบ (แปลงจาก dd/MM/yyyy เป็น LocalDate)
     * @return วันที่ที่เลือก
     */
    private LocalDate getSelectedDate(){
        return LocalDate.parse((String) cbDate.getSelectedItem(), DDMMYYYY);
    }

    /**
     * ลงสถานะให้เซลล์หนึ่งใน JTable ตามชื่อห้อง+คอลัมน์ช่วงเวลา
     * @param t ตารางเป้าหมาย
     * @param room ห้อง
     * @param slot ช่วงเวลา "HH:mm-HH:mm"
     * @param status สถานะที่ต้องการลง
     */
    private void applyStatusToCell(JTable t, String room, String slot, String status){
        if (t == null) return;
        int row = -1;
        for (int r = 0; r < t.getRowCount(); r++) {
            if (room.equals(t.getValueAt(r, 0))) { row = r; break; }
        }
        if (row < 0) return;

        int col = -1;
        for (int c = 1; c < t.getColumnCount(); c++) {
            if (slot.equals(t.getColumnName(c))) { col = c; break; }
        }
        if (col < 0) return;

        ((DefaultTableModel)t.getModel()).setValueAt(status, row, col);
        t.repaint();
    }

    /**
     * โหลดรายการสถานะของวันที่ที่เลือกจาก ReservationManager
     * ตั้งค่าเริ่มทุกช่องเป็น "Available" แล้วทาทับเฉพาะช่องที่มีสถานะจริง
     */
    private void preloadFromManagerForSelectedDate(){
        LocalDate date = getSelectedDate();
        List<Reservation> list = ReservationManager.getInstance().getByDate(date);

        // เคลียร์ก่อน (ให้ทุกช่องเป็น Available)
        resetModel(modelAll);
        resetModel(model1);
        resetModel(model2);
        if (tableAll != null) tableAll.repaint();
        if (tableFloor1 != null) tableFloor1.repaint();
        if (tableFloor2 != null) tableFloor2.repaint();

        // ลงสถานะแต่ละรายการ
        for (Reservation r : list) {
            String room = r.getRoom();
            String slot = r.getStart().format(HHMM) + "-" + r.getEnd().format(HHMM);
            String status = r.getStatus();

            applyStatusToCell(tableAll, room, slot, status);
            String floor = getFloorForRoom(room);
            if ("ชั้น 1".equals(floor)) applyStatusToCell(tableFloor1, room, slot, status);
            if ("ชั้น 2".equals(floor)) applyStatusToCell(tableFloor2, room, slot, status);
        }
    }

    /**
     * รีเซ็ตค่าทั้งหมดในโมเดลให้เป็น "Available"
     * @param model โมเดลของตาราง
     */
    private void resetModel(DefaultTableModel model) {
        if (model == null) return;
        for (int r=0; r<model.getRowCount(); r++)
            for (int c=1; c<model.getColumnCount(); c++)
                model.setValueAt("Available", r, c);
    }

    /**
     * เมื่อมีการเพิ่มการจองใหม่จากแหล่งข้อมูล (เรียกอัตโนมัติ)
     * จะลงสถานะให้กับเซลล์ที่ตรงกับวันที่เลือกอยู่เท่านั้น
     * @param r รายการจองที่ถูกเพิ่ม
     */
    @Override
    public void onReservationAdded(Reservation r) {
        if (!r.getDate().equals(getSelectedDate())) return;
        String room = r.getRoom();
        String slot = r.getStart().format(HHMM) + "-" + r.getEnd().format(HHMM);
        String status = r.getStatus();

        applyStatusToCell(tableAll, room, slot, status);
        String floor = getFloorForRoom(room);
        if ("ชั้น 1".equals(floor)) applyStatusToCell(tableFloor1, room, slot, status);
        if ("ชั้น 2".equals(floor)) applyStatusToCell(tableFloor2, room, slot, status);
    }

    /**
     * เมื่อมีการเปลี่ยนสถานะของรายการหนึ่งจากแหล่งข้อมูล (เรียกอัตโนมัติ)
     * จะลงสถานะให้กับเซลล์ที่ตรงกับวันที่เลือกอยู่เท่านั้น
     * @param r รายการจองที่ถูกเปลี่ยนสถานะ
     */
    public void onReservationStatusChanged(Reservation r) {
        if (!r.getDate().equals(getSelectedDate())) return;
        String room = r.getRoom();
        String slot = r.getStart().format(HHMM) + "-" + r.getEnd().format(HHMM);
        String status = r.getStatus();

        applyStatusToCell(tableAll, room, slot, status);
        String floor = getFloorForRoom(room);
        if ("ชั้น 1".equals(floor)) applyStatusToCell(tableFloor1, room, slot, status);
        if ("ชั้น 2".equals(floor)) applyStatusToCell(tableFloor2, room, slot, status);
    }

    /**
     * เมื่อข้อมูลทั้งหมดเปลี่ยนจากแหล่งข้อมูล (เรียกอัตโนมัติ)
     * จะโหลดใหม่ตามวันที่ที่เลือกอยู่
     * @param all รายการจองทั้งหมดล่าสุด
     */
    @Override
    public void onReservationChanged(List<Reservation> all) {
        // รีเพนต์จากข้อมูลจริงของวันปัจจุบัน
        preloadFromManagerForSelectedDate();
    }
}
