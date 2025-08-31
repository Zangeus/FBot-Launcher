import Config.ConfigManager;
import Config.ConfigWindowStructure.ConfigWindow;
import Config.LauncherConfig;
import Waiters.Main;
//import Waiters.SU_related;

import javax.swing.*;

public class Launcher {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--config")) {
            SwingUtilities.invokeLater(() -> {
                ConfigWindow window = new ConfigWindow();
                window.setVisible(true);
            });
        } else {
            LauncherConfig config = ConfigManager.loadConfig();
            startMainApplication(config, args);
        }
    }

    private static void startMainApplication(LauncherConfig config, String[] args) {

        if (!config.isMonitoringEnabled())
            Main.start();
        else Main.start(999);
    }
}