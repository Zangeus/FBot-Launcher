package Waiters;

import Config.ConfigManager;
import Config.LauncherConfig;
import Processes.CloseProcess;
import Processes.EndWatcher;
import Processes.Errors.ErrorMonitoring;
import Processes.Errors.ErrorSeverity;
import Processes.StartIsHere;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static Waiters.TelegramBotSender.sendRandomMessage;

public class Main {
    private static final String LOCK_FILE = "bot_sources/app.lock";
    private static final LauncherConfig config = ConfigManager.loadConfig();
    private static volatile boolean isRunning = true;
    private static boolean done = false;
    private static boolean fatalExit = false;
    private static final int timeoutSeconds = 30;
    private static int maxAttempts = 3;

    public static void start() {
        if (!acquireLock()) {
            System.err.println("Программа уже запущена! Выход...");
            return;
        }
        isRunning = true;

        boolean isSURun = false;
        if (config.isSU_Monitoring()) {
            maxAttempts = 999;
            isSURun = true;
            Utils.ConfigJson.setWeeklyFarming(true);
        } else Utils.ConfigJson.setWeeklyFarming(false);

        try {
            ErrorMonitoring.initFromConfig(config);
            ErrorMonitoring.startAsync();
            EndWatcher.startAsync();

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                CloseProcess.closeAll();

                System.out.println("\n=== Попытка #" + attempt + " ===");
                if (!StartIsHere.start(attempt)) continue;

                System.out.println("--- Start error searching ---");
                if (ErrorMonitoring.waitForStartError(30)) {
                    restart();
                    continue;
                }
                System.out.println("--- End searching ---");

                while (isRunning) {
                    // --- Проверка на завершение ---
                    if (EndWatcher.isStoppedSuccessfully()) {
                        if (config.isSU_Monitoring() &&
                                Utils.ConfigJson.isSUCompletedThisWeek()) {
                            done = true;
                            break;
                        }
                    }

                    ErrorSeverity severity = ErrorMonitoring.waitForError(timeoutSeconds);
                    if (severity != null) {
                        switch (severity) {
                            case ROGUE_FAILED_3_TIMES:
                                System.out.println("\uD83D\uDD04 Перезаходим в виртуалку");
                                continue;

                            case RECOVERABLE:
                                System.out.println("\uD83D\uDD04 Перезапускаемся...");
                                restart();
                                break;

                            case FATAL:
                                System.out.println("❌ Фатальная ошибка → аварийное завершение");
                                fatalExit = true;
                                isRunning = false;
                                done = false;
                                return;
                        }
                    }


                    sleepOneSecond();
                }

                if (done) {
                    break;
                }
            }
        } finally {
            ErrorMonitoring.stop();
            EndWatcher.stop();

            if (!fatalExit) {
                if (done) {
                    if (isSURun)
                        completeSU();
                    else if (config.isSuccessNotification())
                        sendRandomMessage(config.getSuccessMessages());
                }
                else if (config.isFailureNotification())
                        sendRandomMessage(config.getFailureMessages());
            }
            performEmergencyShutdown();
            performFinalCleanup();
        }
    }

    private static void restart() {
        CloseProcess.closeAll();
        sendRandomMessage(config.getReportMessages());
    }

    private static void sleepOneSecond() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void performEmergencyShutdown() {
        try {
            Runtime.getRuntime().exec("shutdown -s -f -t 100");
            System.out.println("⚠ Выключение системы было запущено");
        } catch (IOException e) {
            System.err.println("Выключение было прервано: " + e.getMessage());
        }
    }

    private static void performFinalCleanup() {
        try {
            File lockFile = new File(LOCK_FILE);
            if (lockFile.exists() && !lockFile.delete()) {
                System.err.println("⚠ Не удалось удалить lock-файл: " + lockFile.getAbsolutePath());
            }

            CloseProcess.closeAll();
            System.out.println("✅ Очистка завершена");
        } catch (Exception e) {
            System.err.println("Ошибка при последней зачистке: " + e.getMessage());
        }
    }

    private static boolean acquireLock() {
        try {
            File lockFile = new File(LOCK_FILE);

            if (lockFile.exists()) {
                return false; // или добавить проверку PID
            }

            if (!lockFile.createNewFile()) {
                return false;
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (lockFile.exists() && !lockFile.delete()) {
                    System.err.println("⚠ Не удалось удалить lock-файл");
                }
            }));

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void completeSU() {
        Utils.ConfigJson.setWeeklyFarming(false);

        TelegramBotSender.sendLocalPhoto("bot_sources/SU.png");
        TelegramBotSender.sendText("Исследование Виртуальной вселенной завершено");

        config.setSU_Monitoring(false);
        ConfigManager.saveConfig(config);
        performEmergencyShutdown();
    }

    public static boolean isRunning() {
        return isRunning;
    }
}
