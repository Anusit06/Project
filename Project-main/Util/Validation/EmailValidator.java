package Util.Validation;

/**
 * ที่ตรวจสอบความถูกต้องของ Email
 */
public class EmailValidator {

    /**
     * Email ต้องลงท้ายด้วย @gmail.com หรือ @ต่างๆ
     * @param email
     * @return true ถ้าตรงตามเงื่อนไข, false ถ้าไม่ตรง
     */
    public static boolean validate(String email){
        if (email.endsWith("@gmail.com")) {
            return true;
        }
        if (email.endsWith("@ku.th")) {
            return true;
        }
        if (email.endsWith("@hotmail.com")) {
            return true;
        }
        return false;
    }
}
