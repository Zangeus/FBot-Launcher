package End;

import Config.ConfigManager;
import Config.LauncherConfig;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.util.concurrent.TimeUnit;

import static Utils.FindButtonAndPress.*;
import static Waiters.Monitoring.reenterIntoSU;

public class EndIsNear {
    private static final LauncherConfig config = ConfigManager.loadConfig();
    private static final WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, "src");
    private static final String SU_COMPLETED = "SU_end.png";
    private static final String EMPTY_TASK_LIST = "zero.png";
    private static final String SU_FAILED_RUN = "critical.png";
    private static final String LAUNCHER_FAIL = "critical_2.png";

    public static boolean end() {

        for (int i = 0; i < 7; i++) {
            if (find(LAUNCHER_FAIL)) return false;

            if (find(EMPTY_TASK_LIST)) {

                if (config.weeklySU_IsEnabled() && !find(SU_COMPLETED)) {
                    for (int j = 0; j < 12; j++) {

                        focusApplicationWindow();
                        if (find(SU_COMPLETED)) return true;
                        else if (find(SU_FAILED_RUN)) reenterIntoSU();
                        else sleep(3);
                    }
                } else return true;

            } else sleep(1);
        }

        return false;
    }

    private static boolean find(String picToFind) {
        return findAndClickScreenless(picToFind);
    }

    private static void sleep(int minutes) {
        try {
            TimeUnit.MINUTES.sleep(minutes);
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