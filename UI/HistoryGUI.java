import javax.swing.*;
import java.awt.*;

public class HistoryGUI extends JFrame {

    public HistoryGUI() {
        // ตั้งค่าหน้าต่างหลัก
        setTitle("Admin Panel - History");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // ===== Main Panel =====
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(226, 237, 255));

        // ===== Sidebar =====
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(180, 600));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS)); // จัดเรียงแนวตั้ง

        // โลโก้ + ADMIN
        ImageIcon adminIcon = new ImageIcon(getClass().getResource("images2.png"));
        Image img = adminIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
        adminIcon = new ImageIcon(img);


        JLabel lblAdmin = new JLabel("ADMIN", adminIcon, SwingConstants.CENTER);
        lblAdmin.setFont(new Font("Tahoma", Font.BOLD, 20));
        lblAdmin.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblAdmin.setHorizontalTextPosition(SwingConstants.RIGHT);
        lblAdmin.setIconTextGap(8);

        // ปุ่ม
        JButton btnHome = new JButton("Home");
        JButton btnStatus = new JButton("Status");
        JButton btnHistory = new JButton("History");

        Dimension btnSize = new Dimension(160, 45); // กำหนดขนาดปุ่มใหม่ (กว้าง x สูง)
        btnHome.setMaximumSize(btnSize);
        btnStatus.setMaximumSize(btnSize);
        btnHistory.setMaximumSize(btnSize);
        btnHome.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnHistory.setAlignmentX(Component.CENTER_ALIGNMENT);

        // เพิ่มปุ่มและ spacing
        sidebar.add(Box.createVerticalStrut(30));
        sidebar.add(lblAdmin);
        sidebar.add(Box.createVerticalStrut(50));
        sidebar.add(btnHome);
        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(btnStatus);
        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(btnHistory);
        // ===== Content =====
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(new Color(226, 237, 255));

        // Title
        JLabel lblTitle = new JLabel("History Page", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 24));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        contentPanel.add(lblTitle, BorderLayout.NORTH);

        // Placeholder (เว้นไว้รอใส่ตาราง/ข้อมูลทีหลัง)
        JPanel placeholderPanel = new JPanel();
        placeholderPanel.setBackground(new Color(240, 240, 250));
        placeholderPanel.setPreferredSize(new Dimension(600, 400));

        JLabel lblComing = new JLabel("History data will be loaded here...", SwingConstants.CENTER);
        lblComing.setFont(new Font("Tahoma", Font.ITALIC, 16));

        placeholderPanel.setLayout(new BorderLayout());
        placeholderPanel.add(lblComing, BorderLayout.CENTER);

        contentPanel.add(placeholderPanel, BorderLayout.CENTER);

        // ===== Add panels =====
        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel);

        // ===== Action ของปุ่ม Sidebar =====
         btnHome.addActionListener(new java.awt.event.ActionListener() {
    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        dispose();
        new AdminHomeGUI().setVisible(true);
        }
    });

        btnStatus.addActionListener(new java.awt.event.ActionListener() {
    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        dispose();
        new StatusGUI().setVisible(true);
        }
    });

    btnHistory.addActionListener(new java.awt.event.ActionListener() {
    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        JOptionPane.showMessageDialog(null, "You are already on History page");
            }
        });
    }

    
}

