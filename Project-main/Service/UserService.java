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

    // รายชื่อผู้ใช้ทั้งหมดที่โหลดจากไฟล์ (เก็บในหน่วยความจำระหว่างรัน)
    private final List<IUser> users = new ArrayList<>();

    /**
     * Constructor
     * โหลดข้อมูลผู้ใช้จากไฟล์ Users.txt ทันทีตอนสร้าง
     * และเพิ่ม Admin เริ่มต้นหากยังไม่มี
     */
    public UserService() {
        try {
            // โหลดจากไฟล์ (สมมติ FileHandler.loadUsers() ส่งกลับ List<User>)
            users.addAll(FileHandler.loadUsers());

            // ถ้ายังไม่มี admin ในระบบ ให้สร้าง admin เริ่มต้น
            boolean hasAdmin = users.stream().anyMatch(u -> "Admin".equalsIgnoreCase(u.getRole()));
            if (!hasAdmin) {
                Admin defaultAdmin = new Admin("owen", "Owen1234", "0000000000", "admin@system.com");

                FileHandler.saveUsers(defaultAdmin);

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
            for (IUser user : users) { 
                if (user.getUsername().equals(username) && user.getPassword().equals(password)) {

                    // เก็บทั้ง username + role ไว้ใน session
                    UserSession.login(user.getUsername(), user.getRole());

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
    public boolean register(String username, String password, String phonenumber, String email) {
        if (isUsernameTaken(username)) {
            System.out.println("ชื่อผู้ใช้ถูกใช้แล้ว: " + username);
            return false;
        }

        User user = new User(username, password, phonenumber, email); // Role = "User"
        try {
            FileHandler.saveUsers(user); // เขียนลงไฟล์
            users.add(user);             // เก็บในหน่วยความจำ
            System.out.println("สมัครสมาชิกสำเร็จ: " + username);
            return true;
        } catch (Exception e) {
            System.out.println("เกิดข้อผิดพลาดตอน register: " + e);
            return false;
        }
    }

    /**
     * ค้นหาว่าชื่อถูกใช้ไปแล้วหรือยัง (กันซ้ำตอน Register)
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

    /**
     * ออกจากระบบ (ล้าง session)
     */
    public void logout() {
        UserSession.clearSession();
    }
}

