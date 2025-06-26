package End;

import Config.ConfigManager;
import Config.LauncherConfig;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.util.concurrent.TimeUnit;

import static Utils.FindButtonAndPress.*;

public class EndIsNear {
    private static final LauncherConfig config = ConfigManager.loadConfig();
    private static final WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, "src");
    private static final String SU_COMPLETED = "SU_end.png";
    private static final String EMPTY_TASK_LIST = "zero.png";
    private static final String SU_FAILED_RUN = "critical.png";

    public static boolean end() {

        if (config.isMondayCheckEnabled() && !find(SU_COMPLETED)) {
            for (int i = 0; i < 12; i++) {
                focusApplicationWindow();
                if (find(SU_COMPLETED)) break;
                else sleep(3);
            }
        }

        for (int i = 0; i < 10; i++) {
            if (find(EMPTY_TASK_LIST)) break;

            if (find(SU_FAILED_RUN)) return false;
            else sleep(1);
        }

        return true;
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