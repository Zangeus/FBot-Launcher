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

    public static void start() {
        start(3);
    }

    public static void start(int maxAttempts) {
        if (!acquireLock()) {
            System.err.println("Программа уже запущена! Выход...");
            return;
        }

        isRunning = true;
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
                    if (EndWatcher.isStoppedSuccessfully()) {
                        done = true;
                        break;
                    }
                    if (waitForError()) {
                        restart();
                        break;
                    }
                    sleep(1);
                }

                if (done) {
                    break;
                }
            }
        } finally {
            ErrorMonitoring.stop();
            EndWatcher.stop();

            if (done) {
                //Indicator of Su
                if (maxAttempts < 50) {
                    if (config.isSuccessNotification()) sendRandomMessage(config.getSuccessMessages());
                    performEmergencyShutdown();
                } else sendPhotoOf_SU();
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

    private static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
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
            if (lockFile.exists()) {
                lockFile.delete();
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
            FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();
            FileLock lock = channel.tryLock();

            if (lock == null) {
                channel.close();
                return false;
            }

            // hook для очистки при выходе
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    lock.release();
                    channel.close();
                    lockFile.delete();
                } catch (IOException ignored) {
                }
            }));

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void sendPhotoOf_SU() {
        TelegramBotSender.sendLocalPhoto("bot_sources/SU.png");
        TelegramBotSender.sendText("Исследование Виртуальной вселенной завершено");
    }

    public static boolean isRunning() {
        return isRunning;
    }
}
