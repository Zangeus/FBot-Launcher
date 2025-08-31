package Waiters;

import Config.ConfigManager;
import Config.LauncherConfig;
import End.CloseProcess;
import End.EndWatcher;
import Start.StartIsHere;
import Utils.ErrorMonitoring;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static Waiters.TelegramBotSender.sendRandomMessage;


public class Main {
    private static final String LOCK_FILE = "bot_sources/app.lock";
    private static final LauncherConfig config = ConfigManager.loadConfig();
    private static volatile boolean isRunning = true;
    private static boolean done = false;
    private static final int timeoutSeconds = 30;
    private static int maxAttempts = 3;

    public static void start() {
        if (!acquireLock()) {
            System.err.println("Программа уже запущена! Выход...");
            return;
        }
        isRunning = true;

        boolean isSURun = false;
        if (config.isMonitoringEnabled()) {
            maxAttempts = 999;
            isSURun = true;
            Utils.ConfigJson.setWeeklyFarming(true);
        } else Utils.ConfigJson.setWeeklyFarming(false);

        try {
            ErrorMonitoring.startAsync();
            EndWatcher.startAsync();

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                CloseProcess.closeAll();

                System.out.println("\n=== Попытка #" + attempt + " ===");
                if (!StartIsHere.start(attempt)) continue;

                if (waitForError()) {
                    restart();
                    continue;
                }

                while (isRunning) {
                    if (EndWatcher.isStoppedSuccessfully() || Utils.ConfigJson.isSUCompleted()) {
                        done = true;
                        break;
                    }
                    if (waitForError()) {
                        restart();
                        break;
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

            if (done) {
                if (!isSURun) {
                    if (config.isSuccessNotification()) sendRandomMessage(config.getSuccessMessages());
                    performEmergencyShutdown();
                } else completeSU();

            } else {
                if (config.isFailureNotification()) sendRandomMessage(config.getFailureMessages());
                performEmergencyShutdown();
            }

            performFinalCleanup();
        }
    }

    private static boolean waitForError() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(() -> ErrorMonitoring.waitForSingleError(timeoutSeconds));
        try {
            return future.get(timeoutSeconds + 5, TimeUnit.SECONDS); // с запасом
        } catch (Exception e) {
            return false;
        } finally {
            executor.shutdownNow();
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
            RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
            FileChannel channel = raf.getChannel();
            FileLock lock = channel.tryLock();

            if (lock == null) {
                channel.close();
                raf.close();
                return false;
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    lock.release();
                    channel.close();
                    raf.close();
                    if (!lockFile.delete()) {
                        System.err.println("⚠ Не удалось удалить lock-файл при завершении");
                    }
                } catch (IOException ignored) {
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

        config.setMonitoringEnabled(false);
        ConfigManager.saveConfig(config);
        performEmergencyShutdown();
    }

    public static boolean isRunning() {
        return isRunning;
    }
}
