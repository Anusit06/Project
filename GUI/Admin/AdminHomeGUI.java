package GUI.Admin;
import javax.swing.*;

import java.awt.*;

public class AdminHomeGUI extends JFrame {

    private JPanel contentPanel;     // Panel หลักสำหรับเนื้อหาตรงกลาง
    private CardLayout cardLayout;   // Layout ที่ใช้สลับหน้า (ถ้ามีหลายหน้าใน contentPanel)

    public AdminHomeGUI() {
        // ตั้งค่าหน้าต่างหลัก
        setTitle("Admin Panel - Home");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE); // ปิดโปรแกรมเมื่อกด X
        setLocationRelativeTo(null);             // เปิดตรงกลางหน้าจอ
        setResizable(false);                     // ไม่ให้ปรับขนาดหน้าต่างได้

        setLayout(new BorderLayout()); // ใช้ BorderLayout แบ่งซ้าย-ขวา

        // ===== Sidebar (แถบเมนูซ้าย) =====
 // ===== Sidebar =====
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(180, 600));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS)); // จัดเรียงแนวตั้ง

        // โลโก้ + ADMIN
        ImageIcon adminIcon = new ImageIcon(getClass().getResource("/GUI/Icon/images2.png"));
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

        // ===== Content Panel (พื้นที่เนื้อหา) =====
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        JPanel homePanel = new JPanel(new GridLayout(2, 2, 30, 30)); // ตาราง 2x2 สำหรับการ์ด
        homePanel.setBackground(new Color(220, 230, 250));
        homePanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40)); // เว้นขอบรอบๆ

        // การ์ด 4 ใบ
        homePanel.add(createCard("5", "ห้องว่าง"));   // การ์ด: ห้องว่าง
        homePanel.add(createCard("5", "ห้องไม่ว่าง"));    // การ์ด: ห้องไม่ว่าง
        

        // ใส่ homePanel ใน contentPanel
        contentPanel.add(homePanel, "HOME");

        // เพิ่ม Sidebar และ Content ลง JFrame
        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        cardLayout.show(contentPanel, "HOME"); // เริ่มที่หน้า Home

        // ===== Action ของปุ่ม Sidebar =====
        btnHome.addActionListener(new java.awt.event.ActionListener() {
    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        JOptionPane.showMessageDialog(null, "You are already on Home page");
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
        dispose();
        new HistoryGUI().setVisible(true);
            }
        });
    }

    // ฟังก์ชันสร้างการ์ดข้อมูล (มีตัวเลข + ข้อความ)
    private JPanel createCard(String number, String text) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        JLabel lblNumber = new JLabel(number, SwingConstants.CENTER);
        lblNumber.setFont(new Font("Tahoma", Font.BOLD, 36));

        JLabel lblText = new JLabel(text, SwingConstants.CENTER);
        lblText.setFont(new Font("Tahoma", Font.PLAIN, 25));

        card.add(lblNumber, BorderLayout.CENTER);
        card.add(lblText, BorderLayout.SOUTH);

        return card;
    }

    // ฟังก์ชันสร้างการ์ดที่เป็นปุ่มกด

   
}
