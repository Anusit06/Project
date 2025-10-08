package GUI.Admin;

import javax.swing.*;
import java.awt.*;

import GUI.RoomS;

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

        // ===== ปุ่มเมนู (ใช้ navBtn ให้ขนาด/ฟอนต์เท่ากัน) =====
        JButton btnHome    = navBtn("Home");
        JButton btnStatus  = navBtn("Status");
        JButton btnHistory = navBtn("History");
        JButton btnBack    = navBtn("BACK"); // ไปหน้า RoomS

        // ===== จัดวางบน Sidebar =====
        sidebar.add(Box.createVerticalStrut(30));
        sidebar.add(lblAdmin);
        sidebar.add(Box.createVerticalStrut(50));
        sidebar.add(btnHome);
        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(btnStatus);
        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(btnHistory);

        sidebar.add(Box.createVerticalGlue()); // ดันปุ่ม BACK ไปล่างสุด
        sidebar.add(btnBack);
        sidebar.add(Box.createVerticalStrut(18)); // เว้นขอบล่างนิดหน่อย

        // ===== Content Panel (พื้นที่เนื้อหา) =====
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        JPanel homePanel = new JPanel(new GridLayout(2, 2, 30, 30)); // ตาราง 2x2 สำหรับการ์ด
        homePanel.setBackground(new Color(220, 230, 250));
        homePanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40)); // เว้นขอบรอบๆ

        // การ์ด
        homePanel.add(createCard("", "ห้องว่าง"));
        homePanel.add(createCard("", "ห้องไม่ว่าง"));

        contentPanel.add(homePanel, "HOME");

        // เพิ่ม Sidebar และ Content ลง JFrame
        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
        cardLayout.show(contentPanel, "HOME"); // เริ่มที่หน้า Home

        // ===== Action ของปุ่ม Sidebar =====
        btnHome.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "You are already on Home page")
        );

        btnStatus.addActionListener(e -> {
            dispose();
            new StatusGUI().setVisible(true);
        });

        btnHistory.addActionListener(e -> {
            dispose();
            new HistoryGUI().setVisible(true);
        });

        btnBack.addActionListener(e -> {
            dispose();
            new RoomS().setVisible(true); // ถ้าอยู่คนละแพ็กเกจ ให้ปรับ import ตามจริง
        });
    }

    // ฟังก์ชันสร้างปุ่มเมนูมาตรฐาน (ขนาด/ฟอนต์เท่ากันทุกปุ่ม)
    private JButton navBtn(String text) {
        JButton b = new JButton(text);
        Dimension size = new Dimension(170, 52);   // พอดีกับ sidebar กว้าง 180px
        b.setPreferredSize(size);
        b.setMaximumSize(size);
        b.setMinimumSize(size);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setFont(new Font("Tahoma", Font.PLAIN, 16)); // ตัวหนา + ใหญ่
        b.setMargin(new Insets(8, 18, 8, 18));
        b.setFocusPainted(false);
        return b;
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
}
