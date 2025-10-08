package Service;

import java.util.ArrayList;
import java.util.List;

import Model.User;
import Model.Admin;
import Model.IUser;
import Model.UserSession;
import Util.FileHandler;

/**
 * UserService ทำหน้าที่จัดการผู้ใช้ทั้งหมด
 * - ตรวจสอบการ Login
 * - จัดการการ Register
 * - โหลดข้อมูลผู้ใช้จากไฟล์
 */
public class UserService {
    // รายชื่อผู้ใช้ทั้งหมดที่โหลดจากไฟล์ (ใช้ชั่วคราวในโปรแกรม)
    private List<IUser> users = new ArrayList<>();

    /**
     * Constructor
     * โหลดข้อมูลผู้ใช้จากไฟล์ Users.txt ทันทีตอนสร้าง
     * และเพิ่ม Admin ตัวอย่างไว้ในระบบ (ถ้ายังไม่มี)
     */
    public UserService() {
        try {
            users = new ArrayList<>(FileHandler.loadUsers()); // โหลดจากไฟล์

            // ถ้ายังไม่มี admin ในไฟล์ ให้เพิ่ม admin ตัวอย่าง (กันไว้)
            boolean hasAdmin = users.stream().anyMatch(u -> u.getRole().equalsIgnoreCase("Admin"));
            if (!hasAdmin) {
                Admin defaultAdmin = new Admin("admin", "1234", "0000000000", "admin@system.com");
                FileHandler.saveUsers(defaultAdmin); // บันทึกลงไฟล์
                users.add(defaultAdmin);
            }
        } catch (Exception e) {
            System.out.println("โหลดผู้ใช้ล้มเหลว: " + e);
        }
    }

    /**
     * ตรวจสอบการ Login
     * @param username ชื่อผู้ใช้
     * @param password รหัสผ่าน
     * @return true ถ้าข้อมูลถูกต้อง
     */
    public boolean login(String username, String password) {
        try {
            // โหลดผู้ใช้ทั้งหมดจากไฟล์
            for (IUser user : FileHandler.loadUsers()) {
                if (user.getUsername().equals(username) && user.getPassword().equals(password)) {

                    // ✅ เก็บ role ของผู้ใช้ไว้ใน UserSession
                    UserSession.setRole(user.getRole()); // Admin / User

                    // สามารถเพิ่มฟิลด์อื่นในอนาคตได้ เช่น username, id
                    System.out.println("เข้าสู่ระบบสำเร็จ: " + username + " (" + user.getRole() + ")");
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("เกิดข้อผิดพลาดตอน login: " + e);
        }
        return false;
    }

    /**
     * การ Register (สมัครสมาชิกใหม่)
     * - เพิ่ม User ใหม่ลงไฟล์
     * - Role จะเป็น "User" โดยอัตโนมัติ
     */
    public void register(String username, String password, String phonenumber, String email) {
        User user = new User(username, password, phonenumber, email); // Role = "User"

        try {
            FileHandler.saveUsers(user); // เขียนลงไฟล์
            users.add(user); // เพิ่มเข้าลิสต์ในหน่วยความจำ
            System.out.println("สมัครสมาชิกสำเร็จ: " + username);
        } catch (Exception e) {
            System.out.println("เกิดข้อผิดพลาดตอน register: " + e);
        }
    }

    /**
     * ค้นหาผู้ใช้จากชื่อ (ใช้ตรวจซ้ำตอน Register)
     * @param username ชื่อผู้ใช้
     * @return true ถ้าชื่อถูกใช้ไปแล้ว
     */
    public boolean isUsernameTaken(String username) {
        return users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
    }

    /**
     * คืนค่าผู้ใช้ทั้งหมด (ถ้าต้องการแสดงในตาราง)
     */
    public List<IUser> getAllUsers() {
        return new ArrayList<>(users);
    }
}
