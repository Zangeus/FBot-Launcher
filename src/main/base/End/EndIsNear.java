package End;

import Config.ConfigManager;
import Config.LauncherConfig;
import Utils.Extractor;
import Waiters.TelegramBotSender;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static Utils.FindButtonAndPress.*;

public class EndIsNear {
    private static final LauncherConfig config = ConfigManager.loadConfig();
    private static final WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, "src");

    public static boolean end() {
        try {
            focusApplicationWindow();

            if (config.isMondayCheckEnabled()
            && LocalDateTime.now().getDayOfWeek() == DayOfWeek.MONDAY) {
                    for (int i = 0; i < 12; i++) {
                        focusApplicationWindow();

                        if (findAndClickScreenless("checking.png") &&
                                findAndClickScreenless("SU_end.png")) {
                            return findAndClickForTasks("tasks_done.png");
                        } else sleep(3);
                    }
                    TelegramBotSender
                            .sendPhoto(Extractor.captureScreenshot()
                                    , "Очередной баг в виртуалке");
                    return false;
                }

            return findAndClickWithOneMessage("checking.png"
                    , "Кажется завершение пошло не по сценарию")
                    && findAndClickForTasks("tasks_done.png");

        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            CloseProcess.terminateProcesses();
            return false;
        }
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