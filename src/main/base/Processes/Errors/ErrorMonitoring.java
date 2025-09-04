package Processes.Errors;

import Config.LauncherConfig;
import Utils.ClickByCoords;
import Utils.Notifier;
import Waiters.TelegramBotSender;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static Utils.ClickByCoords.activateAndClick;
import static Utils.ClickByCoords.performClick;
import static Utils.FindButtonAndPress.findAndClickWithMessage;

public class ErrorMonitoring {
    private static final Set<String> reportedErrors = ConcurrentHashMap.newKeySet();
    private static final BlockingQueue<ErrorSeverity> errorQueue = new LinkedBlockingQueue<>();
    private static final long START_IGNORE_MS = TimeUnit.SECONDS.toMillis(10); // 10 секунд игнор в начале запуска
    private static long startTime = System.currentTimeMillis();

    private static String ERROR_DIR;
    private static String MAIN_LOG_DIR;
    private static List<String> FAILURE_MESSAGES;
    private static boolean NOTIFY_ON_FAIL;
    private static boolean NOTIFY_ON_REPORT;

    private static ExecutorService executor;
    private static final ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
    private static volatile boolean running = false;

    private static volatile long lastLogTime = System.currentTimeMillis();
    private static final long LOG_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);
    private static ScheduledExecutorService silenceExecutor;

    public static synchronized void startAsync() {
        stop();

        startTime = System.currentTimeMillis();
        running = true;
        executor = Executors.newFixedThreadPool(3);

        executor.submit(() -> monitorErrorDir(ERROR_DIR));
        executor.submit(ErrorMonitoring::monitorMainLogs);
        startSilenceMonitor();

        System.out.println("▶ ErrorMonitoring запущен для: " + ERROR_DIR + " и " + MAIN_LOG_DIR);
    }

    public static void initFromConfig(LauncherConfig config) {
        String basePath = config.getStarRailCopilotPath();
        ERROR_DIR = basePath + "/log/error";
        MAIN_LOG_DIR = basePath + "/log";

        FAILURE_MESSAGES = config.getFailureMessages();
        NOTIFY_ON_FAIL = config.isFailureNotification();
        NOTIFY_ON_REPORT = config.isReportNotification();
    }

    public static synchronized void stop() {
        running = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor = null;
        }
        stopSilenceMonitor();
    }

    public static ErrorSeverity waitForError(int timeoutSeconds) {
        try {
            return errorQueue.poll(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public static boolean waitForStartError(int timeoutSeconds) {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path dir = Paths.get(ERROR_DIR);
            dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

            WatchKey key = watcher.poll(timeoutSeconds, TimeUnit.SECONDS);
            if (key == null) return false;

            for (WatchEvent<?> event : key.pollEvents()) {
                Path newPath = dir.resolve((Path) event.context());
                if (Files.isDirectory(newPath)) {
                    handleErrorFolder(newPath.toFile());
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Ошибка в waitForStartError: " + e.getMessage());
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
                        handleErrorFolder(newPath.toFile());
                    }
                }
                key.reset();
            }
        } catch (Exception e) {
            if (running) Notifier.notifyFailure("Ошибка в ErrorMonitoring (error/): " + e.getMessage());
        }
    }

    private static void handleErrorFolder(File folder) {
        try {
            String baseMsg = "❌ Обнаружена ошибка!\n📂 " + folder.getName();
            if (!reportedErrors.add(folder.getName())) return;

            Notifier.notifyFailureWithFolder(baseMsg, folder);

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
                tailFile(currentLog);

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
            if (running && NOTIFY_ON_FAIL) Notifier.notifyFailure("Ошибка в мониторинге src-логов: " + e.getMessage());
        }
    }

    private static File findLatestSrcLog() throws IOException {
        long now = System.currentTimeMillis();
        long freshnessLimit = TimeUnit.HOURS.toMillis(1); // свежие ≤ 1 часа

        try (Stream<Path> files = Files.list(Paths.get(MAIN_LOG_DIR))) {
            List<File> logs = files
                    .filter(p -> p.getFileName().toString().endsWith("_src.txt"))
                    .map(Path::toFile)
                    .collect(Collectors.toList()); // ✅ вместо .toList()

            if (logs.isEmpty()) {
                System.err.println("❌ В папке " + MAIN_LOG_DIR + " не найдено ни одного *_src.txt");
                return null;
            }

            System.out.println("▶ Найдены src-логи:");
            for (File f : logs) {
                System.out.printf("   - %s | modified=%s | size=%d bytes%n",
                        f.getName(),
                        new Date(f.lastModified()),
                        f.length());
            }

            // фильтруем свежие
            List<File> freshLogs = logs.stream()
                    .filter(f -> now - f.lastModified() <= freshnessLimit)
                    .collect(Collectors.toList()); // ✅ вместо .toList()

            File latest;
            if (!freshLogs.isEmpty()) {
                latest = freshLogs.stream()
                        .max(Comparator.comparingLong(File::lastModified))
                        .orElse(null);
                System.out.println("▶ Выбран свежий src-лог: " + latest.getName());
            } else {
                latest = logs.stream()
                        .max(Comparator.comparingLong(File::lastModified))
                        .orElseThrow(); // уже точно не пусто
                System.out.println("⚠ Нет свежих логов. Используем самый новый из старых: " + latest.getName());
            }

            return latest;
        }
    }

    private static void tailFile(File logFile) {
        System.out.println("▶ Tailer запущен для: " + logFile.getAbsolutePath());

        Tailer tailer = Tailer.builder()
                .setFile(logFile)
                .setDelayDuration(java.time.Duration.ofSeconds(1))
                .setCharset(StandardCharsets.UTF_8)
                .setTailerListener(new TailerListenerAdapter() {
                    private final StringBuilder buffer = new StringBuilder();
                    private final Deque<String> recentLines = new ArrayDeque<>(10);

                    @Override
                    public void handle(String line) {
                        if (!running) return;
                        lastLogTime = System.currentTimeMillis();

                        if (recentLines.size() >= 10) {
                            recentLines.removeFirst();
                        }
                        recentLines.addLast(line);

                        ErrorSeverity severity = ErrorRules.classify(line);

                        if (severity == ErrorSeverity.FATAL && line.contains("Request human takeover")) {
                            if (System.currentTimeMillis() - startTime < START_IGNORE_MS) {
                                System.out.println("⚠ Игнорируем Request human takeover (grace period)");
                                return;
                            }

                            boolean rogueNearby = recentLines.stream()
                                    .anyMatch(l -> l.contains("Task `Rogue` failed 3 or more times."));
                            if (rogueNearby) {
                                severity = ErrorSeverity.ROGUE_FAILED_3_TIMES;
                            }
                        }

                        if (severity != null) {
                            buffer.append(line).append("\n");
                            try {
                                flushError(buffer, severity);
                            } catch (InterruptedException e) {
                                if (NOTIFY_ON_FAIL)
                                    TelegramBotSender.sendText("Ошибка добавления в пул: " + e.getMessage());
                            }
                        }
                    }

                    private void flushError(StringBuilder buffer, ErrorSeverity severity) throws InterruptedException {
                        if (buffer.length() == 0) return;

                        String errorMsg = buffer.toString().trim();
                        if (!reportedErrors.add(errorMsg)) return;

                        boolean offered;
                        switch (severity) {
                            case ROGUE_FAILED_3_TIMES:
                                if (NOTIFY_ON_REPORT)
                                    Notifier.notifyFailure("🔄 Перезаходим в виртуалку\n\n" + errorMsg);
                                singleExecutor.submit(ErrorMonitoring::reenterIntoSU);
                                offered = errorQueue.offer(ErrorSeverity.ROGUE_FAILED_3_TIMES, 2, TimeUnit.SECONDS);
                                if (!offered && NOTIFY_ON_FAIL) {
                                    TelegramBotSender.sendText("⚠ Очередь ошибок переполнена (ROGUE_FAILED_3_TIMES)");
                                }
                                break;

                            case RECOVERABLE:
                                if (NOTIFY_ON_REPORT)
                                    Notifier.notifyFailure("🔄 Перезапускаемся...\n\n" + errorMsg);
                                offered = errorQueue.offer(ErrorSeverity.RECOVERABLE, 2, TimeUnit.SECONDS);
                                if (!offered && NOTIFY_ON_FAIL) {
                                    TelegramBotSender.sendText("⚠ Очередь ошибок переполнена (RECOVERABLE)");
                                }
                                break;

                            case FATAL:
                                List<String> kaomojis = Arrays.asList(
                                        "(ノ_<。)",
                                        "(x_x)",
                                        "(；￣Д￣)",
                                        "(╯°□°）╯︵ ┻━┻",
                                        "(≧д≦ヾ)",
                                        "(｡•́︿•̀｡)"
                                );
                                String face = kaomojis.get(new Random().nextInt(kaomojis.size()));

                                if (NOTIFY_ON_FAIL)
                                    Notifier.notifyFailure(face + " " + FAILURE_MESSAGES + "\n\n" + errorMsg);

                                offered = errorQueue.offer(ErrorSeverity.FATAL, 2, TimeUnit.SECONDS);
                                if (!offered && NOTIFY_ON_FAIL) {
                                    TelegramBotSender.sendText("⚠ Очередь ошибок переполнена (FATAL)");
                                }
                                break;
                        }

                        buffer.setLength(0);
                    }

                })
                .setTailFromEnd(true)
                .get();

        tailer.run();
    }

    // ─── Монитор тишины ───────────────────────────────
    private static void startSilenceMonitor() {
        silenceExecutor = Executors.newSingleThreadScheduledExecutor();
        silenceExecutor.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                if (now - lastLogTime > LOG_TIMEOUT_MS) {
                    System.out.println("[DEBUG] Проверка тишины: lastLogTime=" + new Date(lastLogTime));

                    handleSilenceTimeout();
                    lastLogTime = now;
                }
            } catch (Exception e) {
                System.err.println("Ошибка в monitorSilence: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES); // старт через 1 мин, потом каждые 1 мин
    }

    private static void stopSilenceMonitor() {
        if (silenceExecutor != null && !silenceExecutor.isShutdown()) {
            silenceExecutor.shutdownNow();
            silenceExecutor = null;
        }
    }

    private static void handleSilenceTimeout() {
        String msg = "⚠ В лог не писалось более 5 минут!";
        if (NOTIFY_ON_FAIL)
            Notifier.notifyFailure(msg);

        boolean offered = errorQueue.offer(ErrorSeverity.FATAL);
        if (!offered && NOTIFY_ON_FAIL) {
            TelegramBotSender.sendText("⚠ Очередь ошибок переполнена (SILENCE TIMEOUT)");
        }

        if (!FAILURE_MESSAGES.isEmpty()) {
            TelegramBotSender.sendRandomMessage(FAILURE_MESSAGES);
        }
    }

    // ─── reenterIntoSU ───────────────────────────────
    private static final String MuMu = "Android Device";
    private static final String src = "src";
    private static final Point[] CLICK_POINTS = {
            new Point(970, 444),
            new Point(940, 666),
            new Point(915, 520),
    };

    private static void reenterIntoSU() {
        activateAndClick(MuMu, CLICK_POINTS, 3000);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        performClick(780, 675, 0);

        ClickByCoords.focusWindow(src);
        findAndClickWithMessage("start_button.png", "Не удалось найти кнопку запуска");
    }
}
