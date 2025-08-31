package Utils;

import Config.ConfigManager;
import Config.LauncherConfig;
import Waiters.TelegramBotSender;

import java.io.File;
import java.nio.file.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ErrorMonitoring {
    private static final String ERROR_DIR = "Q:/Z-folder/Bot_time/StarRailCopilot/log/error";
    private static final LauncherConfig config = ConfigManager.loadConfig();

    public static void start() throws Exception {
        Path dir = Paths.get(ERROR_DIR);
        WatchService watcher = FileSystems.getDefault().newWatchService();
        dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

        System.out.println("▶ Мониторинг ошибок в " + dir);

        while (true) {
            WatchKey key = watcher.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                Path newPath = dir.resolve((Path) event.context());
                if (Files.isDirectory(newPath)) {
                    handleErrorFolder(newPath.toFile());
                }
            }
            key.reset();
        }
    }

    public static boolean waitForSingleError(int timeoutSeconds) {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path dir = Paths.get(ERROR_DIR);
            dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

            WatchKey key = watcher.poll(timeoutSeconds, TimeUnit.SECONDS);
            if (key == null) {
                return false;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                Path newPath = dir.resolve((Path) event.context());
                if (Files.isDirectory(newPath)) {
                    handleErrorFolder(newPath.toFile());
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Ошибка в мониторинге: " + e.getMessage());
            return false;
        }
    }

    private static void handleErrorFolder(File folder) {
        try {
            // Сообщение об ошибке
            TelegramBotSender.sendRandomMessage(config.getReportMessages(),
                    "❌ Обнаружена ошибка!\n📂 " + folder.getName());

            // Перебираем файлы
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile()) {
                    if (file.getName().matches(".*\\.(png|jpg)$")) {
                        TelegramBotSender.sendLocalPhoto(file.getAbsolutePath());
                    } else {
                        TelegramBotSender.sendDocument(file);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обработке папки: " + e.getMessage());
        }
    }
}
