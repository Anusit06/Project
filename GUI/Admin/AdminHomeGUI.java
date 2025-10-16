package GUI.Admin;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import GUI.RoomS;

public class AdminHomeGUI extends JFrame {

    private JPanel contentPanel;
    private CardLayout cardLayout;

    // ===== Labels แสดงผลสรุป =====
    private JLabel lblAvailCount;
    private JLabel lblNotAvailCount;

    // ===== ค่าคงที่ต้องตรงกับทั้งระบบ =====
    private static final String[] TIMES = {
        "09:00-10:00","10:00-11:00","11:00-12:00",
        "12:00-13:00","13:00-14:00","14:00-15:00",
        "15:00-16:00"
    };
    private static final String[] ALL_ROOMS = {
        "S01","S02","S03","S04","S05",
        "S06","S07","S08","S09","S10",
        "L01","L02","L03","L04","L05",
        "L06","L07","L08","L09","L10"
    };

    // เก็บ listener ไว้เพื่อลบตอนปิดหน้า
    private Service.ReservationManager.ReservationListener rmListener;

    public AdminHomeGUI() {
        setTitle("Admin Panel - Home");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        // ===== Sidebar =====
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(180, 600));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        ImageIcon adminIcon = new ImageIcon(getClass().getResource("/GUI/Icon/images2.png"));
        Image img = adminIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
        adminIcon = new ImageIcon(img);

        JLabel lblAdmin = new JLabel("ADMIN", adminIcon, SwingConstants.CENTER);
        lblAdmin.setFont(new Font("Tahoma", Font.BOLD, 20));
        lblAdmin.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblAdmin.setHorizontalTextPosition(SwingConstants.RIGHT);
        lblAdmin.setIconTextGap(8);

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

        // ===== Content =====
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        JPanel homePanel = new JPanel(new GridLayout(2, 2, 30, 30));
        homePanel.setBackground(new Color(220, 230, 250));
        homePanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // การ์ดสรุปตัวเลข
       
        homePanel.add(makeStatBox("จำนวนช่วงเวลาที่ว่าง", new Color(230,255,230)));
        homePanel.add(makeStatBox("จำนวนช่วงเวลาที่ถูกจอง", new Color(255,230,230)));

        contentPanel.add(homePanel, "HOME");

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
        cardLayout.show(contentPanel, "HOME");

        // ===== Actions =====
        btnHome.addActionListener(e -> JOptionPane.showMessageDialog(this, "You are already on Home page"));
        btnStatus.addActionListener(e -> { dispose(); new StatusGUI().setVisible(true); });
        btnHistory.addActionListener(e -> { dispose(); new HistoryGUI().setVisible(true); });
        btnBack.addActionListener(e -> { dispose(); new RoomS().setVisible(true); });

        // ===== แสดงตัวเลขครั้งแรก + subscribe อัปเดตอัตโนมัติ =====
        refreshRoomSummary(LocalDate.now());
        rmListener = new Service.ReservationManager.ReservationListener() {
            @Override public void onReservationAdded(Model.Reservation r) {
                if (r.getDate().equals(LocalDate.now())) refreshRoomSummary(LocalDate.now());
            }
            @Override public void onReservationStatusChanged(Model.Reservation r) {
                if (r.getDate().equals(LocalDate.now())) refreshRoomSummary(LocalDate.now());
            }
            @Override public void onReservationChanged(java.util.List<Model.Reservation> all) {
                refreshRoomSummary(LocalDate.now());
            }
        };
        Service.ReservationManager.getInstance().addListener(rmListener);
    }

    @Override
    public void dispose() {
        if (rmListener != null) {
            Service.ReservationManager.getInstance().removeListener(rmListener);
            rmListener = null;
        }
        super.dispose();
    }

    // ===== UI helpers =====
    private JButton navBtn(String text) {
        JButton b = new JButton(text);
        Dimension size = new Dimension(170, 52);
        b.setPreferredSize(size); b.setMaximumSize(size); b.setMinimumSize(size);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setFont(new Font("Tahoma", Font.PLAIN, 16));
        b.setMargin(new Insets(8, 18, 8, 18));
        b.setFocusPainted(false);
        return b;
    }

    // กล่องสรุปตัวเลข (ตั้งค่า label ภายในให้เราเก็บอ้างอิงไว้)
    private JPanel makeStatBox(String title, Color faceColor) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createLineBorder(new Color(200,210,230)));

        JLabel numLbl = new JLabel("0", SwingConstants.CENTER);
        numLbl.setOpaque(true);
        numLbl.setBackground(faceColor);
        numLbl.setFont(new Font("Tahoma", Font.BOLD, 48));
        numLbl.setPreferredSize(new Dimension(0, 140));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Tahoma", Font.BOLD, 18));
        titleLbl.setBorder(BorderFactory.createEmptyBorder(6,0,6,0));

        box.add(numLbl, BorderLayout.CENTER);
        box.add(titleLbl, BorderLayout.SOUTH);

        if ("จำนวนช่วงเวลาที่ว่าง".equals(title)) {
                lblAvailCount = numLbl;
        } else if ("จำนวนช่วงเวลาที่ถูกจอง".equals(title)) {
            lblNotAvailCount = numLbl;
        }

        return box;
    }

    // ===== Logic: คำนวณจำนวนห้องว่าง/ไม่ว่างของวันนั้น =====
    private void refreshRoomSummary(LocalDate date) {
        java.util.List<Model.Reservation> list =
        Service.ReservationManager.getInstance().getByDate(date);

        // เก็บ set ของ slot ที่ "ไม่ใช่ Available" เพื่อไม่นับซ้ำ
        java.util.Set<String> notAvailSlots = new java.util.HashSet<>();

        // คีย์ slot: ROOM|HH:mm-HH:mm
        java.time.format.DateTimeFormatter HHMM = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

        for (Model.Reservation r : list) {
                String status = (r.getStatus() == null ? "Available" : r.getStatus());
                if (!"Available".equalsIgnoreCase(status)) {
                    String slotKey = r.getRoom() + "|" + r.getStart().format(HHMM) + "-" + r.getEnd().format(HHMM);
                    notAvailSlots.add(slotKey);
                }
        }

        int totalSlots = ALL_ROOMS.length * TIMES.length;
        int notAvailable = 0;

        // นับเฉพาะห้อง/ช่วงเวลาที่เราดูแล (กันกรณีมีห้องนอกลิสต์)
        for (String room : ALL_ROOMS) {
            for (String t : TIMES) {
                String key = room + "|" + t;
                if (notAvailSlots.contains(key)) notAvailable++;
            }
        }

        int available = totalSlots - notAvailable;

        if (lblAvailCount    != null) lblAvailCount.setText(String.valueOf(available));
        if (lblNotAvailCount != null) lblNotAvailCount.setText(String.valueOf(notAvailable));
    }
}
