package Util.Validation;
/**
 * ที่ตรวจสอบเบอร์โทร
 */
public class PhoneValidator {
    /**
     * เบอร์โทรต้องขึ้นต้นด้วย 0, ตัวเลขต้องมี 10 ตัว
     * @param phonenumber เบอร์โทร
     * @return true ถ้าตรงตามเงื่อนไข false ถ้าไม่ตรง
     */
    public static boolean validate(String phonenumber){
        if (phonenumber.length() != 10) {
            return false;
        }
        if (!phonenumber.startsWith("0")) {
            return false;
        }
        for(char c : phonenumber.toCharArray()){
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}
