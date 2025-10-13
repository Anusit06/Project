package Util.Validation;
/**
 * ที่ตรวจสอบ Username
 */
public class UsernameValidator {
    /**
     * ตรวจสอบว่า Username ต้องมากกว่า 4 ตัวอักษร แต่ไม่เกิน 16 ตัวอักษร
     * @param username ชื่อผู้ใช้
     * @return true ถ้าตรงตามเงื่อนไข, false ถ้าไม่ตรง
     */
    public static boolean validate(String username){
        if (username.length() >= 4 && username.length() <= 16) {
            return true;
        }
        return false;
    }
}
