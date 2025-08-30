package End;

import Config.ConfigManager;
import Config.LauncherConfig;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.time.Duration;

import static Utils.FindButtonAndPress.findAndClickScreenless;
import static Waiters.Monitoring.reenterIntoSU;

public class EndIsNear {
    private static final LauncherConfig config = ConfigManager.loadConfig();
    private static final WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, "src");
    private static final String SU_COMPLETED = "SU_end.png";
    private static final String EMPTY_TASK_LIST = "zero.png";
    private static final String SU_FAILED_RUN = "critical.png";
    private static final String LAUNCHER_FAIL = "critical_2.png";

    private static final int MAX_MAIN_ATTEMPTS = 7;
    private static final int MAX_SU_ATTEMPTS = 12;
    private static final Duration SHORT_SLEEP = Duration.ofMinutes(1);
    private static final Duration LONG_SLEEP = Duration.ofMinutes(3);

    public static boolean end() {
        for (int i = 0; i < MAX_MAIN_ATTEMPTS; i++) {
            if (find(LAUNCHER_FAIL)) return false;

            if (find(EMPTY_TASK_LIST)) {
                if (config.isWeekSUEnabled() && !find(SU_COMPLETED))
                    return processWeeklySU();

                return true;
            }
            sleep(SHORT_SLEEP);
        }
        return false;
    }

    private static boolean processWeeklySU() {
        for (int j = 0; j < MAX_SU_ATTEMPTS; j++) {
            if (find(SU_COMPLETED)) return true;
            if (find(SU_FAILED_RUN)) reenterIntoSU();

            sleep(LONG_SLEEP);
        }
        return false;
    }

    private static boolean find(String image) {
        focusApplicationWindow();
        return findAndClickScreenless(image);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void focusApplicationWindow() {
        if (hwnd != null) {
            User32.INSTANCE.ShowWindow(hwnd, WinUser.SW_RESTORE);
            User32.INSTANCE.SetForegroundWindow(hwnd);
        }
    }
}