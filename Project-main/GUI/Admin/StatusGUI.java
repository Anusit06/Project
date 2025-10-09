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

public class StatusGUI extends JFrame implements ReservationManager.ReservationListener {

    private JTable tableFloor1, tableFloor2, tableAll;
    private DefaultTableModel model1, model2, modelAll;
    private JPanel sectionFloor1, sectionFloor2, sectionAll;
    private JComboBox<String> cbDate, cbFloor;

    private static final DateTimeFormatter DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HHMM     = DateTimeFormatter.ofPattern("HH:mm");

    
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


    @Override public void dispose() {
        ReservationManager.getInstance().removeListener(this);
        super.dispose();
    }

    /* ===== Sidebar ===== */
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

    private JButton makeSideButton(String text) {
        JButton b = new JButton(text);
        Dimension size = new Dimension(170, 52);
        b.setPreferredSize(size); b.setMaximumSize(size); b.setMinimumSize(size);
        b.setFont(new Font("Tahoma", Font.PLAIN, 16));
        b.setFocusPainted(false);
        return b;
    }

    /* ===== Sections/Table ===== */
    private JPanel buildAllSection(String title) {
        String[] cols = {"ห้อง", "09:00-10:00", "10:00-11:00", "11:00-12:00",
                "12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00"};

        Object[][] dataAll = {
                {"S01","Available","Available","Available","Available","Available","Available","Available"},
                {"S02","Available","Available","Available","Available","Available","Available","Available"},
                {"S03","Available","Available","Available","Available","Available","Available","Available"},
                {"L01","Available","Available","Available","Available","Available","Available","Available"},
                {"L02","Available","Available","Available","Available","Available","Available","Available"},
                {"L03","Available","Available","Available","Available","Available","Available","Available"}
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

    private JPanel buildFloorSection(String title, boolean floor1) {
        String[] cols = {"ห้อง", "09:00-10:00", "10:00-11:00", "11:00-12:00",
                "12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00"};

        Object[][] data1 = {
                {"S01","Available","Available","Available","Available","Available","Available","Available"},
                {"S02","Available","Available","Available","Available","Available","Available","Available"},
                {"S03","Available","Available","Available","Available","Available","Available","Available"},
                {"S04","Available","Available","Available","Available","Available","Available","Available"},
                {"S05","Available","Available","Available","Available","Available","Available","Available"},
        };
        Object[][] data2 = {
                {"L01","Available","Available","Available","Available","Available","Available","Available"},
                {"L02","Available","Available","Available","Available","Available","Available","Available"},
                {"L03","Available","Available","Available","Available","Available","Available","Available"},
                {"L04","Available","Available","Available","Available","Available","Available","Available"},
                {"L05","Available","Available","Available","Available","Available","Available","Available"},
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

    private void setupTable(JTable t) {
        t.setRowHeight(40);
        t.setShowGrid(true);
        t.setGridColor(new Color(220,220,220));
        t.setFont(new Font("Tahoma", Font.PLAIN, 14));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setResizingAllowed(false);
        t.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 14));

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object v, boolean sel, boolean focus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tbl, v, sel, focus, r, c);
                String s = String.valueOf(v);
                if (c > 0) {
                    switch (s) {
                        case "Available": comp.setBackground(Color.GREEN); comp.setForeground(Color.BLACK); break;
                        case "Reserved":  comp.setBackground(Color.RED);   comp.setForeground(Color.WHITE); break;
                        case "Closed":    comp.setBackground(Color.BLACK); comp.setForeground(Color.WHITE); break;
                        default:          comp.setBackground(Color.WHITE); comp.setForeground(Color.BLACK);
                    }
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    comp.setBackground(new Color(210,210,210));
                    comp.setForeground(Color.BLACK);
                    comp.setFont(getFont().deriveFont(Font.BOLD));
                }
                return comp;
            }
        });
    }

    private Dimension prefSizeOf(JTable t) {
        int w = t.getColumnModel().getTotalColumnWidth();
        int h = t.getRowHeight() * t.getRowCount() + t.getTableHeader().getPreferredSize().height + 2;
        return new Dimension(w + 2, h + 2);
    }

    private JButton makeActionButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("Tahoma", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(150, 45));
        b.setFocusPainted(false);
        return b;
    }
    

    /* ===== Actions (เปลี่ยนสถานะ) ===== */
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

    /* ===== Helpers ===== */
    public static void installThaiUIFont(Font f) {
        FontUIResource fui = new FontUIResource(f);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()){
            Object k = keys.nextElement();
            Object v = UIManager.get(k);
            if (v instanceof FontUIResource) UIManager.put(k, fui);
        }
    }

    private LocalTime[] parseSlot(String slot) {
        String[] p = slot.split("-");
        return new LocalTime[] {
            LocalTime.parse(p[0], HHMM),
            LocalTime.parse(p[1], HHMM)
        };
    }

    private String getFloorForRoom(String room){
        if (room != null && room.toUpperCase().startsWith("S")) return "ชั้น 1";
        if (room != null && room.toUpperCase().startsWith("L")) return "ชั้น 2";
        return "ทุกชั้น";
    }

    private LocalDate getSelectedDate(){
        return LocalDate.parse((String) cbDate.getSelectedItem(), DDMMYYYY);
    }

    /** ลงสถานะให้ cell ตามห้อง+ช่วงเวลาใน JTable ที่กำหนด */
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

    /** โหลดสถานะจาก ReservationManager สำหรับวันที่ที่เลือก แล้วทาทับตาราง */
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

    private void resetModel(DefaultTableModel model) {
        if (model == null) return;
        for (int r=0; r<model.getRowCount(); r++)
            for (int c=1; c<model.getColumnCount(); c++)
                model.setValueAt("Available", r, c);
    }

    /* ===== ReservationManager Listener ===== */
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

    @Override
    public void onReservationChanged(List<Reservation> all) {
        // รีเพนต์จากข้อมูลจริงของวันปัจจุบัน
        preloadFromManagerForSelectedDate();
    }
    

    /* ===== main (สำหรับเทสต์เดี่ยว) ===== */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StatusGUI().setVisible(true));
    }
}
