package GUI.Admin;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.*;

import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

public class StatusGUI extends JFrame {
    private JTable tableFloor1, tableFloor2;
    private DefaultTableModel model1, model2;
    private JPanel sectionFloor1, sectionFloor2;
    private JComboBox<String> cbDate, cbFloor;
    private JTable tableAll;
    private DefaultTableModel modelAll;
    private JPanel sectionAll;

    public StatusGUI() {
        // ฟอนต์ไทยให้สวยเนียนทั้งแอป
        installThaiUIFont(new Font("Tahoma", Font.PLAIN, 14));

        setTitle("ตารางการจอง (ADMIN)");
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ===== Sidebar =====
        JPanel sidebar = buildSidebar();

        // ===== Content =====
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(240, 245, 255));

        // Title
        JLabel lblTitle = new JLabel("ตารางสถานะการจอง (ADMIN)", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 30));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // Filter แถวบน
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        filter.setBackground(new Color(240, 245, 255));

        JLabel lblDate = new JLabel("วันที่จอง:");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        cbDate = new JComboBox<>(new String[]{ today.format(formatter), tomorrow.format(formatter) });

        JLabel lblFloor = new JLabel("เลือกชั้น:");
        cbFloor = new JComboBox<>(new String[]{"ทุกชั้น", "ชั้น 1", "ชั้น 2"});

        JButton btnResetFilter = new JButton("รีเซ็ตตัวกรอง");
        btnResetFilter.addActionListener(e -> {
            cbDate.setSelectedIndex(0);
            cbFloor.setSelectedIndex(0);
            updateFloorVisibility();
        });

        filter.add(lblDate);
        filter.add(cbDate);
        filter.add(lblFloor);
        filter.add(cbFloor);
        filter.add(btnResetFilter);

        JPanel north = new JPanel();
        north.setBackground(new Color(240, 245, 255));
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(lblTitle);
        north.add(filter);

        content.add(north, BorderLayout.NORTH);

        // Sections: ทุกชั้น / ชั้น 1 / ชั้น 2
        sectionAll    = buildAllSection("ทุกชั้น");   // ค่าเริ่มต้น
        sectionFloor1 = buildFloorSection("ชั้น 1", true);
        sectionFloor2 = buildFloorSection("ชั้น 2", false);

        // เริ่มแสดงเฉพาะ "ทุกชั้น"
        sectionFloor1.setVisible(false);
        sectionFloor2.setVisible(false);

        JPanel center = new JPanel();
        center.setBackground(Color.WHITE);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(sectionAll);
        center.add(Box.createVerticalStrut(20));
        center.add(sectionFloor1);
        center.add(Box.createVerticalStrut(20));
        center.add(sectionFloor2);

        content.add(center, BorderLayout.CENTER);

        // Action buttons (กำหนดสถานะช่อง)
        JPanel action = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        action.setBackground(new Color(240, 245, 255));

        JButton btnAvailable = makeActionButton("Available", Color.GREEN, Color.BLACK);
        JButton btnReserved  = makeActionButton("Reserved", Color.RED, Color.WHITE);
        JButton btnClosed    = makeActionButton("Closed", Color.BLACK, Color.WHITE);
        JButton btnReset     = makeActionButton("Reset", new Color(200, 200, 255), Color.BLACK);

        btnAvailable.addActionListener(e -> setStatus("Available"));
        btnReserved.addActionListener(e -> setStatus("Reserved"));
        btnClosed.addActionListener(e -> setStatus("Closed"));
        btnReset.addActionListener(e -> resetStatus());

        action.add(btnAvailable);
        action.add(btnReserved);
        action.add(btnClosed);
        action.add(btnReset);

        content.add(action, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        // เปลี่ยนชั้น → เปลี่ยน section ที่แสดง
        cbFloor.addActionListener(e -> updateFloorVisibility());
    }

    /* ========================= Sidebar ========================= */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(200, 750));

        // Header: โลโก้ + ADMIN
        JLabel logo = new JLabel();
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/GUI/Icon/images2.png"));
            Image img = icon.getImage().getScaledInstance(54, 54, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(img));
        } catch (Exception ignored) {}

        JLabel lbl = new JLabel("ADMIN");
        lbl.setFont(new Font("Tahoma", Font.BOLD, 22));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 20));
        header.setBackground(Color.WHITE);
        header.add(logo);
        header.add(lbl);

        sidebar.add(header, BorderLayout.NORTH);

        // ปุ่มเมนู
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(Color.WHITE);

        JButton bHome    = makeSideButton("Home");
        JButton bStatus  = makeSideButton("Status");
        JButton bHistory = makeSideButton("History");
        JButton bBack    = makeSideButton("BACK"); // ปุ่มย้อนกลับ (ล่างสุด)

        menuPanel.add(bHome);
        menuPanel.add(Box.createVerticalStrut(15));
        menuPanel.add(bStatus);
        menuPanel.add(Box.createVerticalStrut(15));
        menuPanel.add(bHistory);

        // ดัน BACK ไปล่างสุด
        menuPanel.add(Box.createVerticalGlue());
        menuPanel.add(bBack);
        menuPanel.add(Box.createVerticalStrut(12));

        // Actions
        bHome.addActionListener(e -> {
            AdminHomeGUI home = new AdminHomeGUI();
            home.setVisible(true);
            dispose();
        });

        bStatus.addActionListener(e ->
            JOptionPane.showMessageDialog(this, "You are already on Status page")
        );

        bHistory.addActionListener(e -> {
            HistoryGUI historyGUI = new HistoryGUI();
            historyGUI.setVisible(true);
            dispose();
        });

        // BACK → กลับหน้า RoomS
        bBack.addActionListener(e -> {
        new GUI.RoomS().setVisible(true); dispose();
        });

        sidebar.add(menuPanel, BorderLayout.CENTER);
        return sidebar;
    }

    private JButton makeSideButton(String text) {
        JButton b = new JButton(text);
        Dimension size = new Dimension(170, 52);     // ขนาดใหญ่พอดีกับ sidebar
        b.setPreferredSize(size);
        b.setMaximumSize(size);
        b.setMinimumSize(size);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setFont(new Font("Tahoma", Font.PLAIN, 16)); 
        b.setFocusPainted(false);
        b.setMargin(new Insets(8, 16, 8, 16));
        return b;
    }

    /* ========================= Sections ========================= */
    private JPanel buildAllSection(String title) {
        String[] cols = {"ห้อง", "09:00-10:00", "10:00-11:00", "11:00-12:00",
                "12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00"};

        Object[][] dataAll = {
                {"S01","Available","Available","Available","Available","Available","Available","Available"},
                {"S02","Available","Available","Available","Available","Available","Available","Available"},
                {"S03","Available","Available","Available","Available","Available","Available","Available"},
                {"L01","Available","Available","Available","Available","Available","Available","Available"},
                {"L02","Available","Available","Available","Available","Available","Available","Available"},
                {"L03","Available","Available","Available","Available","Available","Available","Available"},
        };

        modelAll = new DefaultTableModel(dataAll, cols) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tableAll = new JTable(modelAll);
        setupTable(tableAll);

        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(new Font("Tahoma", Font.BOLD, 18));
        lbl.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JScrollPane sp = new JScrollPane(tableAll);
        sp.setPreferredSize(preferredSizeOf(tableAll));

        JPanel wrap = new JPanel();
        wrap.setBackground(Color.WHITE);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.add(lbl);
        wrap.add(sp);
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
            model1 = new DefaultTableModel(data1, cols) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            tableFloor1 = new JTable(model1);
            setupTable(tableFloor1);
        } else {
            model2 = new DefaultTableModel(data2, cols) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            tableFloor2 = new JTable(model2);
            setupTable(tableFloor2);
        }

        JTable table = floor1 ? tableFloor1 : tableFloor2;

        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(new Font("Tahoma", Font.BOLD, 18));
        lbl.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(preferredSizeOf(table));

        JPanel wrap = new JPanel();
        wrap.setBackground(Color.WHITE);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.add(lbl);
        wrap.add(sp);
        return wrap;
    }

    private void setupTable(JTable table) {
        table.setRowHeight(40);
        table.setShowGrid(true);
        table.setGridColor(new Color(220, 220, 220));
        table.setFont(new Font("Tahoma", Font.PLAIN, 14));
        table.getTableHeader().setReorderingAllowed(false); // ❌ ห้ามลากหัวตาราง
        table.getTableHeader().setResizingAllowed(false);   // ❌ ห้ามปรับขนาดคอลัมน์

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Tahoma", Font.BOLD, 14));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean hasFocus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tbl, val, sel, hasFocus, r, c);
                String v = val.toString();

                if (c > 0) {
                    switch (v) {
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

    private Dimension preferredSizeOf(JTable t) {
        int w = t.getColumnModel().getTotalColumnWidth();
        int h = t.getRowHeight()*t.getRowCount()+t.getTableHeader().getPreferredSize().height+2;
        return new Dimension(w+2, h+2);
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

    /* ========================= Actions ========================= */
    private void setStatus(String status) {
        JTable active =
              (tableFloor1 != null && tableFloor1.getSelectedRow() >= 0) ? tableFloor1 :
              (tableFloor2 != null && tableFloor2.getSelectedRow() >= 0) ? tableFloor2 :
              (tableAll   != null && tableAll.getSelectedRow()   >= 0) ? tableAll   : null;

        if (active != null) {
            int r = active.getSelectedRow();
            int c = active.getSelectedColumn();
            if (r >= 0 && c > 0) { // c > 0 เพื่อไม่แก้คอลัมน์ "ห้อง"
                ((DefaultTableModel)active.getModel()).setValueAt(status, r, c);
                active.repaint();
            }
        }
    }

    private void resetStatus() {
        resetModel(model1);
        resetModel(model2);
        resetModel(modelAll);

        if (tableFloor1 != null) tableFloor1.repaint();
        if (tableFloor2 != null) tableFloor2.repaint();
        if (tableAll   != null) tableAll.repaint();
    }

    private void resetModel(DefaultTableModel model) {
        if (model == null) return;
        for (int r=0; r<model.getRowCount(); r++)
            for (int c=1; c<model.getColumnCount(); c++)
                model.setValueAt("Available", r, c);
    }

    private void updateFloorVisibility() {
        String sel = (String) cbFloor.getSelectedItem();

        if ("ทุกชั้น".equals(sel)) {
            sectionAll.setVisible(true);
            sectionFloor1.setVisible(false);
            sectionFloor2.setVisible(false);
        } else if ("ชั้น 1".equals(sel)) {
            sectionAll.setVisible(false);
            sectionFloor1.setVisible(true);
            sectionFloor2.setVisible(false);
        } else if ("ชั้น 2".equals(sel)) {
            sectionAll.setVisible(false);
            sectionFloor1.setVisible(false);
            sectionFloor2.setVisible(true);
        }
        // Revalidate เพื่อให้เลย์เอาต์จัดใหม่
        SwingUtilities.getWindowAncestor(sectionAll).validate();
    }

    /* ========================= Utils ========================= */
    public static void installThaiUIFont(Font f) {
        FontUIResource fui = new FontUIResource(f);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while(keys.hasMoreElements()){
            Object k = keys.nextElement();
            Object v = UIManager.get(k);
            if(v instanceof FontUIResource) UIManager.put(k, fui);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StatusGUI().setVisible(true));
    }
}
