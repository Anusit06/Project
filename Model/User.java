package Model;

/**
 * Class User แทนผู้ใช้ทั่วไป
 * เก็บข้อมูล Username, Password, Phone, Email และ Role
 */
public class User implements IUser {
    private String username;
    private String password;
    private String phonenumber;
    private String email;
    private String role;

    /**
     * Constructor สำหรับสร้าง User ปกติ (role = "User")
     */
    public User(String username, String password, String phonenumber, String email) {
        this.username = username;
        this.password = password;
        this.phonenumber = phonenumber;
        this.email = email;
        this.role = "User"; // ค่าเริ่มต้น
    }

    /**
     * Constructor ที่ระบุ role เองได้ (ใช้ตอนโหลดจากไฟล์)
     */
    public User(String username, String password, String phonenumber, String email, String role) {
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
     * แปลง User เป็นข้อความ 1 บรรทัดสำหรับเก็บไฟล์
     * @return "username,password,phonenumber,email,role"
     */
    public String toFileString() {
        return username + "," + password + "," + phonenumber + "," + email + "," + role;
    }

    /**
     * สร้าง User จากข้อมูลในไฟล์ .txt
     * @param line ข้อความ 1 บรรทัดในไฟล์ Users.txt
     * @return User ที่สร้างจากข้อมูล หรือ null ถ้าข้อมูลไม่ครบ
     */
    public static User fromFileString(String line) {
        String[] parts = line.split(",");
        if (parts.length < 5) {
            return null;
        }

        String username = parts[0].trim();
        String password = parts[1].trim();
        String phone = parts[2].trim();
        String email = parts[3].trim();
        String role = parts[4].trim();

        return new User(username, password, phone, email, role);
    }

    /**
     * แสดงข้อมูลผู้ใช้ (ใช้ debug ได้)
     */
    @Override
    public String toString() {
        return String.format("User{username='%s', role='%s'}", username, role);
    }
}
