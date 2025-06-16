package Waiters;

import Config.ConfigManager;
import Config.LauncherConfig;

import java.awt.*;
import java.awt.event.KeyEvent;

import static Utils.ClickByCoords.*;
import static java.lang.Thread.sleep;

public class TakeTheMail {
    private static final class MailConfig {
        static final String WINDOW_TITLE = "MuMu Player 12";
        static final int DELAY_MS = 2000;
        static final Point BACK_BUTTON = new Point(555, 666);

        static final Point[] CLICK_POINTS = {
                new Point(288, 192),    // Esc
                new Point(1250, 320),   // Mail
                BACK_BUTTON                   // Get Rewards
        };
    }

    private static final Robot robot;
    private static final LauncherConfig config = ConfigManager.loadConfig();

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException("Failed to initialize Robot", e);
        }
    }

    public static void take() {
        if (!config.isTakeTheMailEnabled()) return;
        activateAndClick(MailConfig.WINDOW_TITLE, MailConfig.CLICK_POINTS, MailConfig.DELAY_MS);
        performActions();
    }

    static void pressEsc() throws InterruptedException {
        sleep(1000);
        robot.keyPress(KeyEvent.VK_ESCAPE);
        robot.keyRelease(KeyEvent.VK_ESCAPE);
    }

    private static void performActions() {
        try {
            performClick(MailConfig.BACK_BUTTON.x, MailConfig.BACK_BUTTON.y, MailConfig.DELAY_MS);
            pressEsc();
            pressEsc();
            sleep(MailConfig.DELAY_MS);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}



