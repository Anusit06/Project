package Model;
/**
 * Class Admin ใช้แทนผู้ดูแลระบบ
 * เก็บข้อมูล username, password และ role = "admin"
 */
public class Admin implements IUser {
    private String username;
    private String password;
    private String phonenumber;
    private String email;
    private String role;

    /**
     * สร้างออบเจ็กต์ Admin ใหม่
     * @param username ชื่อผู้ใช้
     * @param password รหัสผ่าน
     * @param phonenumber เบอร์โทร
     * @param email อีเมลล์
     */
    public Admin(String username, String password, String phonenumber, String email) {
        this.username = username;
        this.password = password;
        this.phonenumber = phonenumber;
        this.email = email;
        this.role = "admin";
    }

    /**
     * คืนค่า username
     * @return username
     */
    @Override
    public String getUsername() { return username; }

    /**
     * คืนค่า password
     * @return password
     */
    @Override
    public String getPassword() { return password; }

    /**
     * คืนค่า PhoneNumber
     * @return phonenumber
     */
    @Override
    public String getPhoneNumber() { return phonenumber; }

    /**
     * คืนค่า Email
     * @return email
     */
    @Override
    public String getEmail() { return email; }

    /**
     * คืนค่า role (admin)
     * @return "admin"
     */
    @Override
    public String getRole() { return role; }
}
