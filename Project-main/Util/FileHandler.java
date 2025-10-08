package Util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import Model.Admin;
import Model.User;
import Model.IUser;

/**
 * จัดการอ่าน/เขียนไฟล์ Users.txt
 * ใช้เก็บข้อมูลผู้ใช้ทั้งหมด (ทั้ง Admin และ User)
 */
public class FileHandler {

    private static final String FILE_NAME = "Users.txt";

    /**
     * บันทึกข้อมูลผู้ใช้ใหม่ลงไฟล์ (Append ต่อท้าย)
     * @param user ผู้ใช้ที่ต้องการบันทึก (Admin หรือ User)
     */
    public static void saveUsers(IUser user) {
        File file = new File(FILE_NAME);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            // ใช้เมธอด toFileString() ของคลาสนั้นเอง
            if (user instanceof Admin) {
                writer.write(((Admin) user).toFileString());
            } else if (user instanceof User) {
                writer.write(((User) user).toFileString());
            }
            writer.newLine(); // ขึ้นบรรทัดใหม่
        } catch (Exception e) {
            System.out.println("❌ เกิดข้อผิดพลาดตอนบันทึกผู้ใช้: " + e);
        }
    }

    /**
     * โหลดข้อมูลผู้ใช้ทั้งหมดจากไฟล์ Users.txt
     * @return รายชื่อผู้ใช้ทั้งหมดในไฟล์ (ทั้ง Admin และ User)
     */
    public static List<IUser> loadUsers() {
        List<IUser> users = new ArrayList<>();
        File file = new File(FILE_NAME);

        // ถ้าไฟล์ยังไม่ถูกสร้าง ให้คืนลิสต์ว่าง
        if (!file.exists()) {
            return users;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // แยกข้อมูลออกจากบรรทัด
                String[] parts = line.split(",");
                if (parts.length < 5) continue;

                String username = parts[0].trim();
                String password = parts[1].trim();
                String phone = parts[2].trim();
                String email = parts[3].trim();
                String role = parts[4].trim();

                // ตรวจ role เพื่อสร้าง object ที่ถูกต้อง
                if (role.equalsIgnoreCase("Admin")) {
                    users.add(new Admin(username, password, phone, email, role));
                } else {
                    users.add(new User(username, password, phone, email, role));
                }
            }
        } catch (Exception e) {
            System.out.println("❌ เกิดข้อผิดพลาดตอนโหลดผู้ใช้: " + e);
        }

        return users;
    }
}
