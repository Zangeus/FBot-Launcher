import Config.ConfigWindowStructure.ConfigWindow;
import Waiters.Main;

import javax.swing.*;

public class Launcher {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--config")) {
            SwingUtilities.invokeLater(() -> {
                ConfigWindow window = new ConfigWindow();
                window.setVisible(true);
            });
        } else
            Main.start();
    }
}