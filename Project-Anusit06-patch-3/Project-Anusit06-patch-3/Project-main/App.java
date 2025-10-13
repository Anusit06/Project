import GUI.LoginForm;
import Service.UserService;

public class App {
    public static void main(String[] args) {
        UserService userService = new UserService();
        LoginForm loginForm = new LoginForm(userService);
        loginForm.setVisible(true);
    }
}
