package Processes.Errors;

import Config.LauncherConfig;
import Utils.ClickByCoords;
import Utils.Notifier;
import lombok.Getter;
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
import java.util.concurrent.atomic.AtomicLong;
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
    @Getter
    private static volatile File currentLog;

    private static String ERROR_DIR;
    private static String MAIN_LOG_DIR;
    private static boolean NOTIFY_ON_FAIL;
    private static boolean NOTIFY_ON_REPORT;

    private static ExecutorService executor;
    private static final ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
    private static volatile boolean running = false;

    private static volatile long lastLogTime = System.currentTimeMillis();
    //монитор тишины
    private static final long LOG_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);
    private static ScheduledExecutorService silenceExecutor;

    // Новые поля для улучшенного мониторинга
    private static volatile boolean isLogFileActive = false;
    private static volatile boolean isTailerRunning = false;
    private static final AtomicLong logLinesCount = new AtomicLong(0);
    private static volatile long lastSuccessfulRead = System.currentTimeMillis();
    private static volatile long lastKnownFileSize = 0;

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

    public static ErrorSeverity pollError() {
        return errorQueue.poll();
    }

    public static void initFromConfig(LauncherConfig config) {
        String basePath = config.getStarRailCopilotPath();
        ERROR_DIR = basePath + "/log/error";
        MAIN_LOG_DIR = basePath + "/log";

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
            if (running) Notifier.notifyMessageFailure("Ошибка в ErrorMonitoring (error/): " + e.getMessage());
        }
    }

    private static void handleErrorFolder(File folder) {
        try {
            String baseMsg = "❌ Обнаружена ошибка!\n📂 " + folder.getName();
            // Убираем проверку на дубликаты

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

            currentLog = getOrCreateTodaySrcLog(Paths.get(MAIN_LOG_DIR));
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
            if (running && NOTIFY_ON_FAIL) Notifier.notifyMessageFailure("Ошибка в мониторинге src-логов: " + e.getMessage());
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

    public static File getOrCreateTodaySrcLog(Path logsDir) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        String filename = today + "_src.txt";

        File logFile = logsDir.resolve(filename).toFile();

        try {
            if (!logFile.exists()) {
                if (logFile.createNewFile()) {
                    System.out.println("[LOG] Создан новый файл лога: " + logFile.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при создании лога: " + e.getMessage());
        }

        return logFile;
    }

    private static void tailFile(File logFile) {
        System.out.println("▶ Tailer запущен для: " + logFile.getAbsolutePath());

        isTailerRunning = true;
        isLogFileActive = true;
        updateLastKnownFileSize(logFile.length());

        Tailer tailer = Tailer.builder()
                .setFile(logFile)
                .setDelayDuration(java.time.Duration.ofSeconds(1))
                .setCharset(StandardCharsets.UTF_8)
                .setTailerListener(new TailerListenerAdapter() {
                    private final StringBuilder buffer = new StringBuilder();
                    private final Deque<String> recentLines = new ArrayDeque<>(10);

                    @Override
                    public void handle(String line) {
                        if (!running) {
                            isTailerRunning = false;
                            return;
                        }

                        // Обновляем все счетчики и флаги
                        lastLogTime = System.currentTimeMillis();
                        lastSuccessfulRead = lastLogTime;
                        logLinesCount.incrementAndGet();

                        if (recentLines.size() >= 10) {
                            recentLines.removeFirst();
                        }
                        recentLines.addLast(line);

                        ErrorSeverity severity = ErrorRules.classify(line);

                        if (severity == ErrorSeverity.RECOVERABLE && line.contains("Request human takeover")) {
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
                                    Notifier.notifyMessageFailure("Ошибка добавления в пул: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void fileNotFound() {
                        System.err.println("⚠ Лог-файл не найден: " + logFile.getAbsolutePath());
                        isLogFileActive = false;
                    }

                    @Override
                    public void handle(Exception ex) {
                        System.err.println("⚠ Ошибка в Tailer: " + ex.getMessage());
                        isTailerRunning = false;
                    }

                    private void flushError(StringBuilder buffer, ErrorSeverity severity) throws InterruptedException {
                        if (buffer.length() == 0) return;

                        String errorMsg = buffer.toString().trim();
                        if (!reportedErrors.add(errorMsg)) return;

                        boolean offered;
                        switch (severity) {
                            case ROGUE_FAILED_3_TIMES:
                                if (NOTIFY_ON_REPORT)
                                    Notifier.notifyMessageFailure("🔄 Перезаходим в виртуалку\n\n" + errorMsg);
                                singleExecutor.submit(ErrorMonitoring::reenterIntoSU);
                                offered = errorQueue.offer(ErrorSeverity.ROGUE_FAILED_3_TIMES, 2, TimeUnit.SECONDS);
                                if (!offered && NOTIFY_ON_FAIL) {
                                    Notifier.notifyMessageFailure("⚠ Очередь ошибок переполнена (ROGUE_FAILED_3_TIMES)");
                                }
                                break;

                            case RECOVERABLE:
                                if (NOTIFY_ON_REPORT)
                                    Notifier.notifyMessageFailure("🔄 Перезапускаемся...\n\n" + errorMsg);
                                offered = errorQueue.offer(ErrorSeverity.RECOVERABLE, 2, TimeUnit.SECONDS);
                                if (!offered && NOTIFY_ON_FAIL) {
                                    Notifier.notifyMessageFailure("⚠ Очередь ошибок переполнена (RECOVERABLE)");
                                }
                                break;

                            case FATAL:
                                if (NOTIFY_ON_FAIL)
                                    Notifier.notifyMessageFailure(errorMsg);

                                offered = errorQueue.offer(ErrorSeverity.FATAL, 2, TimeUnit.SECONDS);
                                if (!offered && NOTIFY_ON_FAIL) {
                                    Notifier.notifyMessageFailure("⚠ Очередь ошибок переполнена (FATAL)");
                                }
                                break;
                        }

                        buffer.setLength(0);
                    }

                })
                .setTailFromEnd(true)
                .get();

        try {
            tailer.run();
        } finally {
            isTailerRunning = false;
            isLogFileActive = false;
            System.out.println("⚠ Tailer остановлен для: " + logFile.getAbsolutePath());
        }
    }

    //     ─── Монитор тишины ───────────────────────────────
    private static void startSilenceMonitor() {
        silenceExecutor = Executors.newSingleThreadScheduledExecutor();
        silenceExecutor.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                long diffMs = now - lastLogTime;

                System.out.println("[DEBUG] SILENCE CHECK: " + new Date(now) +
                        "\n[TIME DIFF] " + formatDuration(diffMs) +
                        "\n[LOG ACTIVE] " + isLogFileActive +
                        "\n[TAILER RUNNING] " + isTailerRunning +
                        "\n[LINES COUNT] " + logLinesCount.get());

                // Дополнительные проверки перед вызовом handleSilenceTimeout
                if (shouldTriggerSilenceTimeout(diffMs, now)) {
                    handleSilenceTimeout();
                    lastLogTime = now; // Сбрасываем только после подтверждения проблемы
                }
            } catch (Exception e) {
                System.err.println("Ошибка в monitorSilence: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    // Новый метод с множественными проверками
    private static boolean shouldTriggerSilenceTimeout(long diffMs, long currentTime) {
        // 1. Проверяем основное условие времени
        if (diffMs <= LOG_TIMEOUT_MS) {
            return false;
        }

        // 2. Проверяем, что система вообще работает (не в grace period)
        if (currentTime - startTime < START_IGNORE_MS) {
            System.out.println("⚠ Игнорируем silence timeout (grace period)");
            return false;
        }

        // 3. Проверяем, что лог-файл существует и доступен
        if (currentLog == null || !currentLog.exists()) {
            System.out.println("⚠ Лог-файл не существует, пропускаем silence timeout");
            return false;
        }

        // 4. Проверяем размер файла (если файл растет, значит что-то пишется)
        try {
            long currentSize = currentLog.length();
            long lastKnownSize = getLastKnownFileSize();

            if (currentSize > lastKnownSize) {
                System.out.println("⚠ Файл растет (" + lastKnownSize + " -> " + currentSize +
                        "), возможно Tailer не обрабатывает строки");
                updateLastKnownFileSize(currentSize);
                // Обновляем время, так как файл активен
                lastLogTime = currentTime;
                return false;
            }

            updateLastKnownFileSize(currentSize);
        } catch (Exception e) {
            System.err.println("Ошибка проверки размера файла: " + e.getMessage());
        }

        // 5. Проверяем, что Tailer активен
        if (!isTailerRunning) {
            System.out.println("⚠ Tailer не запущен, возможно это причина молчания");
        }

        // 6. Проверяем время последней модификации файла
        try {
            long fileLastModified = currentLog.lastModified();
            long timeSinceModification = currentTime - fileLastModified;

            if (timeSinceModification < LOG_TIMEOUT_MS) {
                System.out.println("⚠ Файл недавно модифицирован (" +
                        formatDuration(timeSinceModification) + " назад), пропускаем timeout");
                lastLogTime = currentTime; // Обновляем время
                return false;
            }

            System.out.println("[FILE STATUS] Last modified: " +
                    formatDuration(timeSinceModification) + " ago");
        } catch (Exception e) {
            System.err.println("Ошибка проверки времени модификации: " + e.getMessage());
        }

        // 7. Ручная проверка содержимого файла
        if (isLogActiveManually()) {
            System.out.println("⚠ Ручная проверка показывает активность файла");
            lastLogTime = currentTime;
            return false;
        }

        // 8. Финальная проверка - действительно ли прошло достаточно времени
        long gracePeriod = TimeUnit.MINUTES.toMillis(1); // доп. минута на всякий случай
        if (diffMs < LOG_TIMEOUT_MS + gracePeriod) {
            System.out.println("⚠ Добавляем grace period, еще рано для timeout");
            return false;
        }

        System.out.println("🚨 Все проверки пройдены, триггерим silence timeout");
        return true;
    }

    // Метод для ручной проверки активности лога
    private static boolean isLogActiveManually() {
        if (currentLog == null || !currentLog.exists()) {
            return false;
        }

        try {
            // Читаем последние несколько строк файла
            List<String> allLines = Files.readAllLines(currentLog.toPath());
            List<String> lastLines = allLines
                    .stream()
                    .skip(Math.max(0, allLines.size() - 5))
                    .collect(Collectors.toList());

            // Если есть свежие строки, файл активен
            return !lastLines.isEmpty();
        } catch (IOException e) {
            System.err.println("Ошибка ручной проверки файла: " + e.getMessage());
            return false;
        }
    }

    // Вспомогательные методы для отслеживания размера файла
    private static long getLastKnownFileSize() {
        return lastKnownFileSize;
    }

    private static void updateLastKnownFileSize(long size) {
        lastKnownFileSize = size;
    }

    private static String formatDuration(long ms) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms);
        long minutes = seconds / 60;
        seconds %= 60;
        long hours = minutes / 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format("%d ч %d мин %d сек", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d мин %d сек", minutes, seconds);
        } else {
            return String.format("%d сек", seconds);
        }
    }

    private static void stopSilenceMonitor() {
        if (silenceExecutor != null && !silenceExecutor.isShutdown()) {
            silenceExecutor.shutdownNow();
            silenceExecutor = null;
        }
    }

    // Обновленный метод handleSilenceTimeout для более детального логирования
    private static void handleSilenceTimeout() {
        String detailedMsg = String.format(
                "🔇 SILENCE TIMEOUT TRIGGERED:\n" +
                        "📁 Current log: %s\n" +
                        "📏 File size: %d bytes\n" +
                        "🕐 Last log time: %s (%s ago)\n" +
                        "🔄 Tailer running: %s\n" +
                        "📊 Lines processed: %d\n" +
                        "⏱ Timeout threshold: %d minutes",
                (currentLog != null ? currentLog.getName() : "null"),
                (currentLog != null ? currentLog.length() : 0),
                new Date(lastLogTime),
                formatDuration(System.currentTimeMillis() - lastLogTime),
                isTailerRunning,
                logLinesCount.get(),
                TimeUnit.MILLISECONDS.toMinutes(LOG_TIMEOUT_MS)
        );

        System.out.println(detailedMsg);

        if (NOTIFY_ON_FAIL) {
            Notifier.notifyMessageFailure(detailedMsg);
        }

        //Error_change_needed
        boolean offered = errorQueue.offer(ErrorSeverity.RECOVERABLE);
        if (!offered && NOTIFY_ON_FAIL) {
            Notifier.notifyMessageFailure("⚠ Очередь ошибок переполнена (SILENCE TIMEOUT)");
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