package Model;

/**
 * Class Admin ใช้แทนผู้ดูแลระบบ
 * เก็บข้อมูล username, password, phone, email, role = "Admin"
 */
public class Admin implements IUser {
    private String username;
    private String password;
    private String phonenumber;
    private String email;
    private String role;

    /**
     * Constructor หลัก: สร้างออบเจ็กต์ Admin ใหม่
     */
    public Admin(String username, String password, String phonenumber, String email) {
        this.username = username;
        this.password = password;
        this.phonenumber = phonenumber;
        this.email = email;
        this.role = "Admin"; // ค่าเริ่มต้น
    }

    /**
     * Constructor เพิ่มเติม (ใช้ตอนโหลดจากไฟล์)
     */
    public Admin(String username, String password, String phonenumber, String email, String role) {
        this.username = username;
        this.password = password;
        this.phonenumber = phonenumber;
        this.email = email;
        this.role = role;
    }

    // ===== Getter =====
    @Override
    public String getUsername() { return username; }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getPhoneNumber() { return phonenumber; }

    @Override
    public String getEmail() { return email; }

    @Override
    public String getRole() { return role; }

    // ===== File Conversion =====

    /**
     * แปลงข้อมูล Admin เป็นข้อความ 1 บรรทัดสำหรับบันทึกไฟล์
     * @return username,password,phonenumber,email,role
     */
    public String toFileString() {
        return username + "," + password + "," + phonenumber + "," + email + "," + role;
    }

    /**
     * สร้าง Admin จากข้อความ 1 บรรทัดที่อ่านจากไฟล์
     * @param line ข้อความ 1 บรรทัดในไฟล์ Users.txt
     * @return Admin object หรือ null ถ้าข้อมูลไม่ครบ
     */
    public static Admin fromFileString(String line) {
        String[] parts = line.split(",");
        if (parts.length < 5) {
            return null;
        }

        String username = parts[0].trim();
        String password = parts[1].trim();
        String phone = parts[2].trim();
        String email = parts[3].trim();
        String role = parts[4].trim();

        return new Admin(username, password, phone, email, role);
    }

    /**
     * แสดงข้อมูล admin (debug)
     */
    @Override
    public String toString() {
        return String.format("Admin{username='%s', role='%s'}", username, role);
    }
}
