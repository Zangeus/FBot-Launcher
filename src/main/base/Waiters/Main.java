package Waiters;

import Config.ConfigManager;
import Config.LauncherConfig;
import End.CloseProcess;
import End.EndIsNear;
import Start.StartIsHere;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static Utils.FindButtonAndPress.findAndClickScreenless;

public class Main {
    private static final String LOCK_FILE = "bot_sources/app.lock";
    private static final LauncherConfig config = ConfigManager.loadConfig();
    public static volatile boolean isRunning = true;
    private static boolean done = false;

    public static void main(String[] args) {
        if (!acquireLock()) {
            System.err.println("Программа уже запущена! Выход...");
            return;
        }
        isRunning = true;

        try {
            final int maxAttempts = 3;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {

                System.out.println("\nУбираем окна, чтобы не отслеживать ошибки");
                CloseProcess.terminateProcesses();

                System.out.println("\n=== Попытка #" + attempt + " ===");
                if (!StartIsHere.start()) continue;

                sleep(30);
                if (findAndClickScreenless("critical_2.png")) {
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
            if (!done) sendMessages(config.getFailureMessages());
            isRunning = false;

            System.out.println("\n=== ЗАВЕРШЕНИЕ РАБОТЫ ===");
            performFinalCleanup();
            System.exit(0);
        }
    }

    private static void restart() {
        CloseProcess.terminateProcesses();
        sendMessages(config.getReportMessages());
    }

    private static void sendMessages(List<String> messages) {
        if (messages != null && !messages.isEmpty()) {
            TelegramBotSender.sendMessages(messages);
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

            CloseProcess.terminateProcesses();
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

    public static boolean isRunning() {
        return isRunning;
    }
}