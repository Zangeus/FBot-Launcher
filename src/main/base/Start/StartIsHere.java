package Start;

import java.awt.*;
import java.awt.event.KeyEvent;

import static Utils.FindButtonAndPress.*;
import static java.lang.Thread.sleep;

public class StartIsHere {

    public static boolean start() {
        activateWindows();

        if (!findAndClickWithOneMessageAndDelay("start.png"
                , "Кнопка для старта не была найдена", 6000))
            return false;

        return findAndClickWithOneMessage("start_button.png", "Не удалось найти кнопку запуска");
    }

    private static void activateWindows() {
        try {
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_WINDOWS);
            robot.keyRelease(KeyEvent.VK_WINDOWS);
            sleep(500);
        } catch (InterruptedException | AWTException e) {
            Thread.currentThread().interrupt();
            System.err.println("Прервана активация меню Windows: " + e.getMessage());
        }
    }
}