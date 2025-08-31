package Waiters;

import Config.ConfigManager;
import Config.LauncherConfig;
import End.CloseProcess;
import End.EndIsNear;
import Start.StartIsHere;
import Utils.ErrorMonitoring;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class Main {
    private static final String LOCK_FILE = "bot_sources/app.lock";
    private static final LauncherConfig config = ConfigManager.loadConfig();
    private static volatile boolean isRunning = true;
    private static boolean done = false;
    private static final int maxAttempts = 3;
    private static final int timeoutSeconds = 30;

    public static void main(String[] args) {
        if (!acquireLock()) {
            System.err.println("Программа уже запущена! Выход...");
            return;
        }
        isRunning = true;

        try {
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                CloseProcess.closeAll();

                System.out.println("\n=== Попытка #" + attempt + " ===");
                if (!StartIsHere.start(attempt)) continue;

                if (waitForError()) {
                    restart();
                    continue;
                }

                sleep(config.getSleepDurationMinutes() * 60);

                if (EndIsNear.end()) {
                    System.out.println("УСПЕХ! Основной цикл завершен.");
                    done = true;
                    if (config.isSuccessNotification() || attempt > 1)
                        sendMessages(config.getSuccessMessages());
                    break;
                }
            }
        } finally {
            if (!done) {
                sendMessages(config.getFailureMessages());
            }
            isRunning = false;

            System.out.println("\n=== ЗАВЕРШЕНИЕ РАБОТЫ ===");
            performFinalCleanup();
            System.exit(0);
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
        sendMessages(config.getReportMessages());
    }

    private static void sendMessages(List<String> messages) {
        if (messages != null && !messages.isEmpty()) {
            TelegramBotSender.sendRandomMessage(messages, null);
        } else {
            System.err.println("Список сообщений пуст, отправка отменена");
        }
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
            System.out.println("Выключение было запущенно");
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
            performEmergencyShutdown();
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

            // Добавляем shutdown hook для удаления блокировки при завершении работы
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

    public static int getMaxAttempts() {
        return maxAttempts;
    }

    public static boolean isRunning() {
        return isRunning;
    }
}