package GUI.Icon;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class TestImage {
    public static void main(String[] args) {
        java.net.URL imgURL = TestImage.class.getResource("/GUI/Icon/images2.png");
        System.out.println("DEBUG: " + imgURL);

        JFrame frame = new JFrame("Test Image");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 300);

        JLabel label = new JLabel();
        label.setIcon(new ImageIcon(imgURL));
        frame.add(label);

        frame.setVisible(true);
    }
}
