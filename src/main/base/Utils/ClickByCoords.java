package Utils;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.awt.*;
import java.awt.event.InputEvent;

import static java.lang.Thread.sleep;

public class ClickByCoords {
    private static final Robot robot;

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException("Failed to initialize Robot", e);
        }
    }

    public static boolean activateWindow(String WINDOW_TITLE) {
        final int MAX_ATTEMPTS = 3;
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, WINDOW_TITLE);
            if (hwnd != null) {
                User32.INSTANCE.ShowWindow(hwnd, WinUser.SW_RESTORE);
                User32.INSTANCE.SetForegroundWindow(hwnd);
                return true;
            }
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Sleep interrupted");
            }
        }
        logError("Window not found: " + WINDOW_TITLE, null);
        return false;
    }

    private static void logError(String message, Exception e) {
        System.err.println("[ERROR] " + message);
        if (e != null) System.out.println(e.getMessage());
    }

    public static void performClick(int x, int y, int DELAY_MS) {
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        try {
            sleep(DELAY_MS);
        } catch (InterruptedException e) {
            logError("Critical error", e);
        }
    }

    public static void activateAndClick(String WINDOW_TITLE
            , Point[] CLICK_POINTS, int DELAY_MS) {
        try {
            if (!activateWindow(WINDOW_TITLE)) return;
            sleep(DELAY_MS);

            for (Point p : CLICK_POINTS) {
                performClick(p.x, p.y, DELAY_MS);
            }
        } catch (Exception e) {
            logError("Critical error", e);
        }
    }
}
