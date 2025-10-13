package GUI.Admin;

import Service.ReservationManager;
import Model.Reservation;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class HistoryGUI extends JFrame implements ReservationManager.ReservationListener {

    /* ===== UI ===== */
    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> cbDateScope; // ทั้งหมด / วันนี้ / พรุ่งนี้ / เลือกวันที่…
    private JFormattedTextField txtCustomDate;
    private final DateTimeFormatter DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter HHMM     = DateTimeFormatter.ofPattern("HH:mm");

    /* ===== State ===== */
    private final ReservationManager rm = ReservationManager.getInstance();

    public HistoryGUI() {
        setTitle("ประวัติการจอง");
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        /* ===== Sidebar (ให้ไปหน้าอื่นได้เหมือนเดิม) ===== */
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(180, 700));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel lblAdmin = new JLabel("ADMIN", SwingConstants.CENTER);
        lblAdmin.setFont(new Font("Tahoma", Font.BOLD, 20));
        lblAdmin.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnHome    = navBtn("Home");
        JButton btnStatus  = navBtn("Status");
        JButton btnHistory = navBtn("History");
        JButton btnBack    = navBtn("BACK");

        sidebar.add(Box.createVerticalStrut(30));
        sidebar.add(lblAdmin);
        sidebar.add(Box.createVerticalStrut(50));
        sidebar.add(btnHome);
        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(btnStatus);
        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(btnHistory);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(btnBack);
        sidebar.add(Box.createVerticalStrut(18));

        btnHome.addActionListener(e -> { new AdminHomeGUI().setVisible(true); dispose(); });
        btnStatus.addActionListener(e -> { new StatusGUI().setVisible(true); dispose(); });
        btnHistory.addActionListener(e -> JOptionPane.showMessageDialog(this, "คุณอยู่ที่หน้า History แล้ว"));
        btnBack.addActionListener(e -> { new GUI.RoomS().setVisible(true); dispose(); });

        add(sidebar, BorderLayout.WEST);

        /* ===== Content ===== */
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(240,245,255));
        add(content, BorderLayout.CENTER);

        JLabel title = new JLabel("ประวัติการจอง", SwingConstants.CENTER);
        title.setFont(new Font("Tahoma", Font.BOLD, 28));
        title.setBorder(BorderFactory.createEmptyBorder(16,0,10,0));
        content.add(title, BorderLayout.NORTH);

        // Filters (ซ้าย: ขอบเขตวันที่ / วันที่กำหนดเอง — เราจะ “ซ่อน Closed เสมอ” อยู่แล้ว)
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        filters.setBackground(new Color(240,245,255));

        cbDateScope = new JComboBox<>(new String[]{"ทั้งหมด", "วันนี้", "พรุ่งนี้", "กำหนดวัน (dd/MM/yyyy)"});
        cbDateScope.setFont(new Font("Tahoma", Font.PLAIN, 14));

        txtCustomDate = new JFormattedTextField(DDMMYYYY.toFormat());
        txtCustomDate.setColumns(10);
        txtCustomDate.setFont(new Font("Tahoma", Font.PLAIN, 14));
        txtCustomDate.setToolTipText("รูปแบบ dd/MM/yyyy เช่น 09/10/2025");
        txtCustomDate.setEnabled(false);

        JButton btnRefresh = new JButton("รีเฟรช");
        btnRefresh.setFont(new Font("Tahoma", Font.BOLD, 14));

        filters.add(new JLabel("ช่วงวันที่:"));
        filters.add(cbDateScope);
        filters.add(txtCustomDate);
        filters.add(btnRefresh);

        content.add(filters, BorderLayout.SOUTH); // วางล่าง — จะไม่บังหัวตาราง

        // ตาราง
        String[] cols = {"ห้อง", "วันที่", "เวลา", "สถานะ", "ผู้จอง"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        setupTableLook(table);
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        // Events
        cbDateScope.addActionListener(e -> txtCustomDate.setEnabled("กำหนดวัน (dd/MM/yyyy)".equals(cbDateScope.getSelectedItem())));
        btnRefresh.addActionListener(e -> reloadTableFromManager());

        // Subscribe manager events → เด้งอัตโนมัติ
        rm.addListener(this);

        // แรกเข้า
        reloadTableFromManager();
    }

    /* ===== Table Style ===== */
    private void setupTableLook(JTable t) {
        t.setRowHeight(36);
        t.setFont(new Font("Tahoma", Font.PLAIN, 14));
        t.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 14));
        t.setGridColor(new Color(220,220,220));
        t.setShowGrid(true);

        // สีสถานะ
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean selected, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, selected, focus, row, col);
                String status = String.valueOf(tbl.getValueAt(row, 3)); // คอลัมน์สถานะ

                if (!selected) {
                    if ("Reserved".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(255, 230, 230));
                    } else if ("Available".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(230, 255, 230));
                    } else if ("Closed".equalsIgnoreCase(status)) {
                        // ปกติจะไม่เห็นเพราะเรากรองออก แต่เผื่อกรณีพิเศษ
                        c.setBackground(new Color(230, 230, 230));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                ((JLabel) c).setHorizontalAlignment(col == 0 ? SwingConstants.CENTER : SwingConstants.CENTER);
                return c;
            }
        });
    }

    /* ===== Load & Filter ===== */
    private void reloadTableFromManager() {
        // 1) ดึงข้อมูลตามช่วงวันที่
        List<Reservation> raw = getRawByScope();

        // 2) ซ่อน Closed เสมอ + ซ่อนแถวที่ไม่มีผู้จอง (กันเคสปิดห้อง/record พิเศษ)
        List<Reservation> visible = raw.stream()
                .filter(r -> !"Closed".equalsIgnoreCase(r.getStatus()))                  // <<— ซ่อนปิดห้อง
                .filter(r -> r.getUsername() != null && !r.getUsername().isBlank())      // <<— ซ่อนแถวไม่มีผู้จอง
                .sorted(Comparator
                        .comparing(Reservation::getDate)
                        .thenComparing(Reservation::getRoom)
                        .thenComparing(Reservation::getStart))
                .collect(Collectors.toList());

        // 3) เติมตารางใหม่
        model.setRowCount(0);
        for (Reservation r : visible) {
            model.addRow(new Object[]{
                    r.getRoom(),
                    r.getDate().format(DDMMYYYY),
                    r.getStart().format(HHMM) + "-" + r.getEnd().format(HHMM),
                    r.getStatus(),
                    safeBookedBy(r)
            });
        }
        model.fireTableDataChanged();
    }

    private List<Reservation> getRawByScope() {
        String scope = (String) cbDateScope.getSelectedItem();
        if (scope == null || "ทั้งหมด".equals(scope)) {
            return rm.getAll();
        }
        if ("วันนี้".equals(scope)) {
            return rm.getByDate(LocalDate.now());
        }
        if ("พรุ่งนี้".equals(scope)) {
            return rm.getByDate(LocalDate.now().plusDays(1));
        }
        if ("กำหนดวัน (dd/MM/yyyy)".equals(scope)) {
            try {
                LocalDate d = LocalDate.parse(txtCustomDate.getText().trim(), DDMMYYYY);
                return rm.getByDate(d);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "กรุณากรอกวันที่รูปแบบ dd/MM/yyyy ให้ถูกต้อง", "รูปแบบวันที่ไม่ถูกต้อง", JOptionPane.WARNING_MESSAGE);
                return Collections.emptyList();
            }
        }
        return rm.getAll();
    }

    private String safeBookedBy(Reservation r) {
        String u = r.getUsername();
        if (u == null || u.isBlank()) return "(ไม่ระบุ)";
        return u;
    }

    private JButton navBtn(String text) {
        JButton b = new JButton(text);
        Dimension size = new Dimension(170, 52);
        b.setPreferredSize(size); b.setMaximumSize(size); b.setMinimumSize(size);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setFont(new Font("Tahoma", Font.PLAIN, 16));
        b.setMargin(new Insets(8,18,8,18));
        b.setFocusPainted(false);
        return b;
    }

    /* ===== ReservationManager Listener ===== */
    @Override public void onReservationAdded(Reservation r) { reloadIfInScope(r); }
    @Override public void onReservationStatusChanged(Reservation r) { reloadIfInScope(r); }
    @Override public void onReservationChanged(List<Reservation> all) { reloadTableFromManager(); }

    private void reloadIfInScope(Reservation r) {
        // ถ้ารายการเปลี่ยนอยู่ในช่วงวันที่ที่เราเลือก → รีโหลด
        String scope = (String) cbDateScope.getSelectedItem();
        if ("ทั้งหมด".equals(scope)) { reloadTableFromManager(); return; }
        if ("วันนี้".equals(scope) && r.getDate().equals(LocalDate.now())) { reloadTableFromManager(); return; }
        if ("พรุ่งนี้".equals(scope) && r.getDate().equals(LocalDate.now().plusDays(1))) { reloadTableFromManager(); return; }
        if ("กำหนดวัน (dd/MM/yyyy)".equals(scope)) {
            try {
                LocalDate d = LocalDate.parse(txtCustomDate.getText().trim(), DDMMYYYY);
                if (r.getDate().equals(d)) reloadTableFromManager();
            } catch (Exception ignore) {}
        }
    }

    @Override
    public void dispose() {
        ReservationManager.getInstance().removeListener(this);
        super.dispose();
    }

    /* ===== main (สำหรับทดสอบแยกไฟล์) ===== */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HistoryGUI().setVisible(true));
    }
}
