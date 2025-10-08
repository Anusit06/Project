package Model;

/**
 * เก็บบทบาท (role) ของผู้ใช้ที่ล็อกอิน
 */
public class UserSession {

    // เก็บเฉพาะ role ปัจจุบัน เช่น "Admin" หรือ "User"
    private static String currentRole = null;

    /** ตั้งค่า role หลังจากตรวจสอบจากไฟล์ตอน login สำเร็จ */
    public static void setRole(String role) {
        currentRole = role;
    }

    /** ล้าง role (ใช้เมื่อปิดโปรแกรมหรืออยากรีเซ็ตสถานะ) */
    public static void clearRole() {
        currentRole = null;
    }

    /** คืนค่า role ปัจจุบัน (อาจเป็น null ถ้ายังไม่ตั้ง) */
    public static String getRole() {
        return currentRole;
    }

    /** helper: เป็นแอดมินไหม */
    public static boolean isAdmin() {
        return "Admin".equalsIgnoreCase(currentRole);
    }

    /** helper: เป็นผู้ใช้ทั่วไปไหม */
    public static boolean isUser() {
        return "User".equalsIgnoreCase(currentRole);
    }

    /** helper: มี role ถูกตั้งไว้แล้วหรือยัง */
    public static boolean hasRole() {
        return currentRole != null && !currentRole.isBlank();
    }
}
