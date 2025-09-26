package Service;

import java.util.ArrayList;
import java.util.List;

import Model.User;
import Model.Admin;
import Model.IUser;
import Util.FileHandler;

/**
 * UserService ทำหน้าที่จัดการผู้ใช้ทั้งหมด
 * เก็บ User
 * ตรวจสอบการ Login
 * ตรวจสอบการ Register
 */
public class UserService {
    private List<IUser> users = new ArrayList<>();

    /**
     * สร้าง User และ Admin ตรงนี้ได้
     */
    public UserService() {
        users.add(new Admin(null, null, null, null)); // สร้าง Admin ตรงนี้ก่อน
    }

    /**
     * ตรวจสอบการ login
     * @param username ชื่อผู้ใช้
     * @param password รหัสผ่าน
     * @return true ถ้า username และ password ตรงกับ users ที่เก็บไว้, อื่นๆ false
     */
    public boolean login(String username, String password) {
        try {
            for (IUser user : FileHandler.loadUsers()) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return true;
            }
        }
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }

    /**
     * การ register (เพิ่ม user เข้าไปใน List)
     * และบันทึก user ลงไฟล์ .txt ตอน Register
     * @param username ชื่อผู้ใช้
     * @param password รหัสผ่าน
     * @param email อีเมลล์
     * @param phonenumber เบอร์โทร
     */
    public void register(String username, String password, String phonenumber, String email ) {
        User user = new User(username, password, phonenumber, email);
        try {
            FileHandler.saveUsers(user);
        } catch (Exception e) {
            System.out.println(e);
        }
        users.add(user);
    }
}
