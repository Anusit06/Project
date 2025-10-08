package GUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class MyReservationGUI extends JFrame {

    public MyReservationGUI() {
        setTitle("การจองของฉัน");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);

        // === พื้นหลังฟ้าอ่อน ===
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(230, 240, 255)); 
        add(mainPanel);

        // === แถบเมนูด้านบน ===
        JPanel topMenu = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        topMenu.setBackground(new Color(230, 240, 255));

        JButton btnRoom = createMenuButton("ห้อง");
        JButton btnSchedule = createMenuButton("ตารางการจอง");
        JButton btnMyReservation = createMenuButton("การจองของฉัน");

        topMenu.add(btnRoom);
        topMenu.add(btnSchedule);
        topMenu.add(btnMyReservation);

        mainPanel.add(topMenu, BorderLayout.NORTH);

        // ใส่ ActionListener ให้ปุ่ม Home
btnSchedule.addActionListener((ActionListener) new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        // เปิดหน้า HomeGUI
        ReservationTableUI reservationTableUI = new ReservationTableUI();
        reservationTableUI.setVisible(true);

        // ปิดหน้าปัจจุบัน (StatusGUI)
        dispose();
    }
});
        // ปุ่ม ห้อง
btnRoom.addActionListener((ActionListener) new ActionListener() {
    public void actionPerformed(ActionEvent e){
        dispose();
        new RoomS().setVisible(true);
    }
});

        // === ส่วนกลาง ===
        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(new Color(230, 240, 255));
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // หัวข้อ
        JLabel lblTitle = new JLabel("การจองของฉัน");
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 22));
        lblTitle.setForeground(Color.BLACK);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(lblTitle);

        centerPanel.add(Box.createVerticalStrut(15));

        // === แถบเลือกวันที่ ===
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        datePanel.setBackground(new Color(230, 240, 255));

        JLabel lblDate = new JLabel("วันที่จอง:");
        lblDate.setFont(new Font("Tahoma", Font.PLAIN, 16));
        lblDate.setForeground(Color.BLACK);

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String[] dates = {
                today.minusDays(1).format(fmt),
                today.format(fmt),
                today.plusDays(1).format(fmt)
        };

        JComboBox<String> cmbDate = new JComboBox<>(dates);
        cmbDate.setFont(new Font("Tahoma", Font.PLAIN, 14));

        datePanel.add(lblDate);
        datePanel.add(cmbDate);
        centerPanel.add(datePanel);
        centerPanel.add(Box.createVerticalStrut(15));

// === ตาราง (ใส่แถวว่างไว้ก่อน) ===
String[] columnNames = {"ห้อง", "วันที่จอง", "เวลา", "การดำเนินการ"};

// ข้อมูลเริ่มต้น (แถวว่าง 5 แถว)
Object[][] data = {
    {"", "", "", ""},
    {"", "", "", ""},
    {"", "", "", ""},
    {"", "", "", ""},
    {"", "", "", ""},
    {"", "", "", ""},
    {"", "", "", ""},
    {"", "", "", ""},
    {"", "", "", ""},
    {"", "", "", ""},
    {"", "", "", ""},
    {"", "", "", ""},
    {"", "", "", ""},
    {"", "", "", ""},
   
};

DefaultTableModel model = new DefaultTableModel(data, columnNames) {
    @Override
    public boolean isCellEditable(int row, int column) {
        return false; // ไม่ให้แก้ไขช่อง
    }
};

JTable table = new JTable(model);
table.setRowHeight(35);
table.setFont(new Font("Tahoma", Font.PLAIN, 14));
table.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 15));

// เส้นตารางดำเข้ม
table.setShowGrid(true);
table.setGridColor(Color.BLACK);

JScrollPane scrollPane = new JScrollPane(table);
scrollPane.getViewport().setBackground(new Color(245, 245, 245));



// --- ตั้งค่า ---
table.setRowHeight(30);
table.setFont(new Font("Tahoma", Font.PLAIN, 14));
table.setGridColor(Color.BLACK);

// หัวตาราง
table.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 15));
table.getTableHeader().setBackground(new Color(220, 220, 220));
table.getTableHeader().setReorderingAllowed(false);
table.getTableHeader().setResizingAllowed(false);

// ✅ เส้นด้านในบาง ๆ
table.setShowGrid(true);
table.setShowHorizontalLines(true);
table.setShowVerticalLines(true);

// ✅ กำหนดเส้นนอกหนา
table.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.gray));



centerPanel.add(scrollPane);
        setVisible(true);
    }

   private JButton createMenuButton(String text) {
    JButton button = new JButton(text);
    button.setFont(new Font("Tahoma", Font.BOLD, 15));
    button.setBackground(new Color(51, 102, 255)); // ฟ้าน้ำเงิน
    button.setForeground(Color.WHITE);             // ตัวอักษรสีขาว
    button.setFocusPainted(false);
    button.setPreferredSize(new Dimension(160, 40)); // ขนาดปุ่มกำหนดตายตัว
    return button;
}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MyReservationGUI::new);
    }
}
