import Config.ConfigWindowStructure.ConfigWindow;
import Waiters.Main;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.swing.*;
public class Launcher {

    public static void main(String[] args) throws TelegramApiException {
        if (args.length > 0 && args[0].equals("--config")) {
            SwingUtilities.invokeLater(() -> new ConfigWindow().setVisible(true));
        } else {
            Main.start();
        }
    }
}
