import GUI.LoginForm;
import GUI.RoomS;
import GUI.Admin.AdminHomeGUI;
import GUI.Admin.HistoryGUI;
import Service.UserService;

public class App {
    public static void main(String[] args) {
        UserService userService = new UserService();
        new LoginForm(userService).setVisible(true);
        //new RoomS().setVisible(true);
    }
}
