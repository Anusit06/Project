package Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import Model.User;

/**
 * ที่จัดการ อ่าน/เขียนไฟล์ เก็บไว้ใน Users.txt
 */
public class FileHandler {
    private static final String File_Name = "Users.txt";


    /**
     * บันทึก User ใหม่ลงไฟล์ .txt
     * @param user User ที่จะบันทึก
     */
    public static void saveUsers(User user){
        File file = new File(File_Name);

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file,true))) {
            writer.write(user.toFileString());
            writer.newLine();
            
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static List<User> loadUsers(){
        List<User> users = new ArrayList<>();
        File file = new File(File_Name);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))){
            String line;
            while ((line = reader.readLine()) != null) {
                User user = User.FormFileString(line);
                if (user != null) {
                    users.add(user);
                }
            }

        } catch (Exception e) {
            System.out.println(e);
        }
        return users;
    }
}
