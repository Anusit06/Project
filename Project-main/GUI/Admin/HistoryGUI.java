package GUI.Admin;

import javax.swing.*;
import javax.swing.table.*;

import GUI.RoomS;

import java.awt.*;
import java.awt.event.*;

public class HistoryGUI extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private JScrollPane scroll;

    public HistoryGUI() {
        setTitle("Admin Panel - History");
        setSize(1000, 650);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(226, 237, 255));

        // ===== Sidebar =====
        root.add(buildSidebar(), BorderLayout.WEST);

        // ===== Content =====
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(226, 237, 255));
        content.add(buildHeader(), BorderLayout.NORTH);
        content.add(buildTableArea(), BorderLayout.CENTER);

        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        // เติมแถวว่างพอดีหน้าจอหลังเฟรมแสดงผลแล้ว + ตอน resize
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> ensureFullPageRows());
            }
        });
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                ensureFullPageRows();
            }
        });
    }

    // ---------------- UI building ----------------
    private JPanel buildSidebar() {
    JPanel sidebar = new JPanel();
    sidebar.setPreferredSize(new Dimension(180, 650));
    sidebar.setBackground(Color.WHITE);
    sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

    ImageIcon adminIcon = new ImageIcon(getClass().getResource("/GUI/Icon/images2.png"));
    Image img = adminIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
    JLabel lblAdmin = new JLabel("ADMIN", new ImageIcon(img), SwingConstants.CENTER);
    lblAdmin.setFont(new Font("Tahoma", Font.BOLD, 20));
    lblAdmin.setAlignmentX(Component.CENTER_ALIGNMENT);
    lblAdmin.setHorizontalTextPosition(SwingConstants.RIGHT);
    lblAdmin.setIconTextGap(8);

    JButton btnHome    = navBtn("Home");
    JButton btnStatus  = navBtn("Status");
    JButton btnHistory = navBtn("History");
    JButton btnBack    = navBtn("BACK");  // ✅ ปุ่ม BACK

    // วางปุ่ม
    sidebar.add(Box.createVerticalStrut(28));
    sidebar.add(lblAdmin);
    sidebar.add(Box.createVerticalStrut(40));
    sidebar.add(btnHome);
    sidebar.add(Box.createVerticalStrut(18));
    sidebar.add(btnStatus);
    sidebar.add(Box.createVerticalStrut(18));
    sidebar.add(btnHistory);

    sidebar.add(Box.createVerticalGlue()); // ✅ ดัน BACK ไปล่างสุด
    sidebar.add(btnBack);
    sidebar.add(Box.createVerticalStrut(18));

    // Action
    btnHome.addActionListener(e -> { dispose(); new AdminHomeGUI().setVisible(true); });
    btnStatus.addActionListener(e -> { dispose(); new StatusGUI().setVisible(true); });
    btnHistory.addActionListener(e -> JOptionPane.showMessageDialog(this, "You are already on History page"));
    btnBack.addActionListener(e -> {        dispose();new RoomS().setVisible(true);}); // ✅ กดแล้วกลับหน้า RoomS (Admin)

    return sidebar;
}


    // ปุ่มเมนู: ขนาดเท่ากัน + ฟอนต์หนา 18 (ใหญ่เหมือนที่ขอ)
    private JButton navBtn(String text) {
        JButton b = new JButton(text);
        Dimension sz = new Dimension(170, 52);       // พอดีกับ sidebar 180px
        b.setPreferredSize(sz);
        b.setMaximumSize(sz);
        b.setMinimumSize(sz);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setFont(new Font("Tahoma", Font.PLAIN, 16)); 
        b.setMargin(new Insets(10, 18, 10, 18));
        b.setFocusPainted(false);
        return b;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(new Color(210, 225, 250));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(190,200,215)));

        JLabel title = new JLabel("All History", SwingConstants.CENTER);
        title.setFont(new Font("Tahoma", Font.BOLD, 30)); // หัวข้อใหญ่
        title.setForeground(new Color(40, 40, 40));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(16, 16, 16, 16);
        header.add(title, gc);
        return header;
    }

    private JComponent buildTableArea() {
        // หัวตาราง: ชื่อ | ห้อง | เวลา | วันที่
        String[] cols = {"ชื่อ", "ห้อง", "เวลา", "วันที่"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) c.setBackground(row % 2 == 0 ? new Color(248,250,255) : Color.WHITE);
                else c.setBackground(new Color(204,224,255));
                return c;
            }
        };
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setShowGrid(true);
        table.setGridColor(new Color(215, 220, 230));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader h = table.getTableHeader();
        ((DefaultTableCellRenderer) h.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        h.setFont(h.getFont().deriveFont(Font.BOLD, 16f)); // หัวคอลัมน์ใหญ่ขึ้นเล็กน้อยให้บาลานซ์กับปุ่ม
        h.setBackground(new Color(235, 240, 250));

        // จัดกึ่งกลางเฉพาะคอลัมน์ ห้อง/เวลา/วันที่
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(center); // ห้อง
        table.getColumnModel().getColumn(2).setCellRenderer(center); // เวลา
        table.getColumnModel().getColumn(3).setCellRenderer(center); // วันที่

        // ความกว้างคร่าว ๆ
        table.getColumnModel().getColumn(0).setPreferredWidth(260); // ชื่อ
        table.getColumnModel().getColumn(1).setPreferredWidth(80);  // ห้อง
        table.getColumnModel().getColumn(2).setPreferredWidth(180); // เวลา
        table.getColumnModel().getColumn(3).setPreferredWidth(140); // วันที่

        scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    // ---------------- Core: เติม "แถวว่าง" ให้เต็มจอ ----------------
    private void ensureFullPageRows() {
        if (scroll == null || table == null || model == null) return;

        // ลบแถวว่างเก่าก่อน
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            boolean emptyRow = true;
            for (int c = 0; c < model.getColumnCount(); c++) {
                Object v = model.getValueAt(i, c);
                if (v != null && !v.toString().isEmpty()) { emptyRow = false; break; }
            }
            if (emptyRow) model.removeRow(i);
        }

        int rowH = table.getRowHeight();
        int headerH = table.getTableHeader().getHeight();
        int viewportH = scroll.getViewport().getHeight();

        int needRows = Math.max(10, (viewportH - headerH) / rowH);
        int toAdd = needRows - model.getRowCount();
        for (int i = 0; i < toAdd; i++) {
            model.addRow(new Object[]{"", "", "", ""});
        }
    }

    // ----- ใส่ข้อมูลจริงภายหลัง -----
    /** rows: {ชื่อ, ห้อง, เวลา, วันที่} */
    public void setHistoryData(java.util.List<Object[]> rows) {
        model.setRowCount(0);
        for (Object[] r : rows) model.addRow(r);
        ensureFullPageRows();
    }
}
