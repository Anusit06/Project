package Service;

import java.util.ArrayList;
import java.util.List;

import Model.User;
import Model.Admin;
import Model.IUser;
import Model.UserSession;
import Util.FileHandler;

/**
 * UserService
 * 
 * บริการจัดการผู้ใช้: โหลดผู้ใช้จากไฟล์, ตรวจสอบการเข้าสู่ระบบ (login),
 * สมัครสมาชิก (register), ตรวจสอบชื่อซ้ำ และจัดการ session ออกจากระบบ
 * 
 *
 *  โค้ดนี้สมมติว่า {@link Util.FileHandler} มีความสามารถบันทึก/โหลดผู้ใช้
 * และรองรับการบันทึกทั้ง {@link Model.User} และ {@link Model.Admin} (ผ่านเมธอดเดียวกันเช่น saveUsers(IUser)).
 * หากในโปรเจกต์ปัจจุบันมีเฉพาะ {@code saveUsers(User user)} ให้ทำ overload เพิ่ม
 * หรือแยกเมธอดบันทึก Admin ต่างหากตามที่ใช้งานจริง
 */
public class UserService {

    /** รายชื่อผู้ใช้ทั้งหมดที่โหลดไว้ในหน่วยความจำระหว่างรัน */
    private final List<IUser> users = new ArrayList<>();

    /**
     * สร้างออบเจ็กต์บริการผู้ใช้
     * <ul>
     *   <li>โหลดข้อมูลผู้ใช้ทั้งหมดจากไฟล์ Users.txt</li>
     *   <li>ถ้ายังไม่มีผู้ใช้ที่มี role = "admin" จะสร้างแอดมินเริ่มต้น</li>
     * </ul>
     */
    public UserService() {
        try {
            // โหลดจากไฟล์ (รองรับเคสที่ FileHandler.loadUsers() คืน List<User>)
            List<IUser> loaded = FileHandler.loadUsers();
            if (loaded != null) {
                for (IUser u : loaded) {
                    users.add(u); // User implements IUser
                }
            }

            // ตรวจว่ามี admin แล้วหรือยัง 
            boolean hasAdmin = false;
            for (IUser u : users) {
                if (u.getRole() != null && u.getRole().equalsIgnoreCase("admin")) {
                    hasAdmin = true;
                    break;
                }
            }

            // ถ้ายังไม่มี ให้เพิ่มแอดมินเริ่มต้น
            if (!hasAdmin) {
                Admin defaultAdmin = new Admin("owen", "Owen1234", "0000000000", "admin@system.com");

                // แนะนำ: ให้มีเมธอด saveUsers(IUser user) ใน FileHandler
                // หากตอนนี้มีแค่ saveUsers(User user) ให้ทำ overload เพิ่ม
                FileHandler.saveUsers(defaultAdmin);

                users.add(defaultAdmin);
            }
        } catch (Exception e) {
            System.out.println("โหลดผู้ใช้ล้มเหลว: " + e);
        }
    }

    /**
     * ตรวจสอบการเข้าสู่ระบบ (Login)
     *
     * @param username ชื่อผู้ใช้ที่ป้อนเข้ามา
     * @param password รหัสผ่านที่ป้อนเข้ามา
     * @return {@code true} ถ้าพบผู้ใช้และรหัสผ่านถูกต้อง, {@code false} หากไม่พบ/ไม่ถูกต้อง
     */
    public boolean login(String username, String password) {
        try {
            //  ไม่โหลดไฟล์ซ้ำ ใช้ข้อมูลในหน่วยความจำ
            for (IUser user : users) {
                if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                    // เก็บ username + role ลง session สำหรับใช้งานภายหลัง
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
     * สมัครสมาชิก (Register) สำหรับผู้ใช้ทั่วไป (role = "User")
     *
     * @param username ชื่อผู้ใช้ใหม่ (ต้องไม่ซ้ำ)
     * @param password รหัสผ่าน
     * @param phonenumber เบอร์โทรศัพท์
     * @param email อีเมล
     * @return {@code true} เมื่อสมัครและบันทึกสำเร็จ, {@code false} ถ้าชื่อซ้ำหรือบันทึกล้มเหลว
     */
    public boolean register(String username, String password, String phonenumber, String email) {
        if (isUsernameTaken(username)) {
            System.out.println("ชื่อผู้ใช้ถูกใช้แล้ว: " + username);
            return false;
        }

        User user = new User(username, password, phonenumber, email); // โดยปกติ role ภายใน User = "User"
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
     * ตรวจสอบว่าชื่อผู้ใช้ถูกใช้ไปแล้วหรือยัง (กันซ้ำตอนสมัครสมาชิก)
     *
     * @param username ชื่อผู้ใช้ที่ต้องการตรวจสอบ
     * @return {@code true} ถ้าพบว่ามีผู้ใช้ชื่อนี้แล้ว, {@code false} หากยังไม่ถูกใช้
     */
    public boolean isUsernameTaken(String username) {
        for (IUser u : users) {
            if (u.getUsername() != null && u.getUsername().equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * คืนค่ารายชื่อผู้ใช้ทั้งหมด (สำเนาใหม่ เพื่อป้องกันภายนอกแก้ไขลิสต์ภายใน)
     *
     * @return รายชื่อผู้ใช้ทั้งหมดในระบบ ณ ขณะนั้น
     */
    public List<IUser> getAllUsers() {
        return new ArrayList<>(users);
    }

}
