import GUI.AdminHomeGUI;
import GUI.HistoryGUI;
import GUI.LoginForm;
import Service.UserService;

public class App {
    public static void main(String[] args) {
        UserService userService = new UserService();
        new LoginForm(userService).setVisible(true);
    }
}
