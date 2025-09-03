import Config.ConfigWindowStructure.ConfigWindow;
import Waiters.Main;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

public class Launcher {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf()); // можно FlatLightLaf()
        } catch (Exception e) {
            System.err.println("Не удалось загрузить FlatLaf: " + e.getMessage());
        }

        if (args.length > 0 && args[0].equals("--config")) {
            SwingUtilities.invokeLater(() -> {
                ConfigWindow window = new ConfigWindow();
                window.setVisible(true);
            });
        } else Main.start();
    }
}