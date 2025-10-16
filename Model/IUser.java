package Model;
/**
 * Interface IUser เป็นสัญญา (Contact) สำหรับ User หรือ Admin
 */
public interface IUser {
    /**
     * คืนค่า Username
     */
    String getUsername();
    /**
     * คืนค่า Password
     */
    String getPassword();
    /**
     * คืนค่า PhoneNumber
     */
    String getPhoneNumber();
    /**
     * คืนค่า Email
     */
    String getEmail();
    /**
     * คืนค่า role ของผู้ใช้ เช่น "User" or "Admin"
     */
    String getRole();
}
