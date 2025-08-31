package Utils;

import Config.ConfigManager;
import Config.LauncherConfig;
import Waiters.TelegramBotSender;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static Waiters.SU_related.reenterIntoSU;

public class ErrorMonitoring {
    private static final String ERROR_DIR = "Q:/Z-folder/Bot_time/StarRailCopilot/log/error";
    private static final String MAIN_LOG_DIR = "Q:/Z-folder/Bot_time/StarRailCopilot/log";
    private static final LauncherConfig config = ConfigManager.loadConfig();
    private static final Set<String> reportedErrors = ConcurrentHashMap.newKeySet();

    private static ExecutorService executor;
    private static volatile boolean running = false;

    public static synchronized void startAsync() {
        stop();

        running = true;
        executor = Executors.newFixedThreadPool(2);

        // мониторинг error/ директорий
        executor.submit(() -> monitorErrorDir(ERROR_DIR));

        // мониторинг src-логов (с авто-переключением на новый файл)
        executor.submit(ErrorMonitoring::monitorMainLogs);

        System.out.println("▶ ErrorMonitoring запущен для: " + ERROR_DIR + " и " + MAIN_LOG_DIR);
    }

    public static synchronized void stop() {
        running = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public static boolean waitForSingleError(int timeoutSeconds) {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path dir = Paths.get(ERROR_DIR);
            dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

            WatchKey key = watcher.poll(timeoutSeconds, TimeUnit.SECONDS);
            if (key == null) return false;

            for (WatchEvent<?> event : key.pollEvents()) {
                Path newPath = dir.resolve((Path) event.context());
                if (Files.isDirectory(newPath)) {
                    handleErrorFolder(newPath.toFile(), false); // single → без реэнтера
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Ошибка в waitForSingleError: " + e.getMessage());
            return false;
        }
    }

    // ─── Мониторинг error/ ───────────────────────────────
    private static void monitorErrorDir(String dirPath) {
        Path dir = Paths.get(dirPath);
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
            System.out.println("▶ Мониторинг error-папок: " + dir);

            while (running) {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path newPath = dir.resolve((Path) event.context());
                    if (Files.isDirectory(newPath)) {
                        handleErrorFolder(newPath.toFile(), true);
                    }
                }
                key.reset();
            }
        } catch (Exception e) {
            if (running) TelegramBotSender.sendText("Ошибка в ErrorMonitoring (error/): " + e.getMessage());
        }
    }

    private static void handleErrorFolder(File folder, boolean fromAsync) {
        try {
            String baseMsg = "❌ Обнаружена ошибка!\n📂 " + folder.getName();
            if (!reportedErrors.add(folder.getName())) return;

            TelegramBotSender.sendText(baseMsg);

            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile()) {
                    if (file.getName().matches(".*\\.(png|jpg|jpeg|gif)$")) {
                        TelegramBotSender.sendLocalPhoto(file.getAbsolutePath());
                    } else {
                        TelegramBotSender.sendDocument(file);
                    }
                }
            }

            if (fromAsync) {
                Executors.newSingleThreadExecutor().submit(() -> {
                    try {
                        System.out.println("♻ Перезапуск через reenterIntoSU() из-за ошибки");
                        reenterIntoSU();
                    } catch (Exception e) {
                        System.err.println("Ошибка при reenterIntoSU: " + e.getMessage());
                    }
                });
            }

        } catch (Exception e) {
            System.err.println("Ошибка при обработке папки: " + e.getMessage());
        }
    }

    // ─── Мониторинг src-логов ───────────────────────────────
    private static void monitorMainLogs() {
        Path dir = Paths.get(MAIN_LOG_DIR);

        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

            File currentLog = findLatestSrcLog();
            if (currentLog == null) {
                System.err.println("Не найден *_src.txt в " + dir);
                return;
            }
            System.out.println("▶ Мониторинг src-лога: " + currentLog);

            while (running) {
                tailFile(currentLog); // блокирующий tailer, выйдет только при stop()

                // ждём новый файл
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path newPath = dir.resolve((Path) event.context());
                    if (newPath.toString().endsWith("_src.txt")) {
                        currentLog = newPath.toFile();
                        System.out.println("▶ Переключение на новый src-лог: " + currentLog);
                    }
                }
                key.reset();
            }
        } catch (Exception e) {
            if (running) TelegramBotSender.sendText("Ошибка в мониторинге src-логов: " + e.getMessage());
        }
    }

    private static File findLatestSrcLog() throws IOException {
        try (Stream<Path> files = Files.list(Paths.get(MAIN_LOG_DIR))) {
            return files.filter(p -> p.getFileName().toString().endsWith("_src.txt"))
                    .map(Path::toFile)
                    .max((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()))
                    .orElse(null);
        }
    }

    private static void tailFile(File logFile) {
        Tailer tailer = Tailer.builder()
                .setFile(logFile)
                .setDelayDuration(java.time.Duration.ofSeconds(1))
                .setCharset(StandardCharsets.UTF_8)
                .setTailerListener(new TailerListenerAdapter() {
                    private final StringBuilder buffer = new StringBuilder();
                    private boolean collecting = false;

                    @Override
                    public void handle(String line) {
                        if (!running) return;

                        if (line.contains("ERROR") || line.contains("CRITICAL")
                                || line.contains("Traceback")
                                || line.contains("Game died during launch")
                                || line.contains("Request human takeover")) {
                            collecting = true;
                            buffer.setLength(0);
                            buffer.append(line).append("\n");
                        } else if (collecting) {
                            if (line.trim().isEmpty() || line.startsWith("2025-")) {
                                flushError();
                            } else {
                                buffer.append(line).append("\n");
                            }
                        }
                    }

                    private void flushError() {
                        if (buffer.length() > 0) {
                            String errorMsg = buffer.toString().trim();
                            if (reportedErrors.add(errorMsg)) {
                                TelegramBotSender.sendText("❌ Обнаружена ошибка в src-логе:\n\n" + errorMsg);
                            }
                            buffer.setLength(0);
                        }
                        collecting = false;
                    }
                })
                .setTailFromEnd(true)
                .get();

        tailer.run();
    }
}
