package Model;

/**
 * จัดเก็บสถานะผู้ใช้ที่ล็อกอินในปัจจุบัน
 * เช่น ชื่อผู้ใช้ และ บทบาท (User/Admin)
 */
public class UserSession {

    private static String currentUsername = null; // ชื่อผู้ใช้ปัจจุบัน
    private static String currentRole = null;     // บทบาท: "ADMIN" หรือ "USER" (จะ normalize เป็นตัวพิมพ์ใหญ่)

    /** ช่วยแปลง role ให้เป็นมาตรฐาน (ADMIN/USER) หรือ null ถ้าไม่รู้จัก */
    private static String normalizeRole(String role) {
        if (role == null) return null;
        String r = role.trim().toUpperCase();
        if (r.equals("ADMIN")) return "ADMIN";
        if (r.equals("USER"))  return "USER";
        // เผื่อระบบเดิมส่ง "Admin"/"User" มา ก็จะถูก map มาแล้ว, ถ้านอกเหนือให้คืน null
        return null;
    }

    /** ตั้งค่าหลังจาก login สำเร็จ */
    public static void login(String username, String role) {
        currentUsername = (username == null || username.isBlank()) ? null : username.trim();
        currentRole = normalizeRole(role); // เก็บเป็น ADMIN/USER เท่านั้น
    }

    /** คืนค่า username ปัจจุบัน */
    public static String getCurrentUsername() {
        return currentUsername;
    }

    /** คืนค่า role ปัจจุบัน (เป็น ADMIN/USER หรือ null) */
    public static String getRole() {
        return currentRole;
    }

    /**  เป็นแอดมินไหม */
    public static boolean isAdmin() {
        return "ADMIN".equals(currentRole);
    }

    /**  เป็นผู้ใช้ทั่วไปไหม */
    public static boolean isUser() {
        return "USER".equals(currentRole);
    }

    /**  มีการ login แล้วหรือยัง */
    public static boolean isLoggedIn() {
        return currentUsername != null && !currentUsername.isBlank();
    }

    /** ตั้ง role ระหว่างรันไทม์ (เช่น หลังเปลี่ยนสิทธิ์) */
    public static void setRole(String role) {
        currentRole = normalizeRole(role);
    }

    /** (ถ้าจำเป็น) ตั้ง username ระหว่างรันไทม์ */
    public static void setCurrentUsername(String username) {
        currentUsername = (username == null || username.isBlank()) ? null : username.trim();
    }
}
