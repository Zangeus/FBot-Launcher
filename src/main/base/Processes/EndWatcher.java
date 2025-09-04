package Processes;

import Config.ConfigManager;
import Config.LauncherConfig;
import lombok.Getter;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.json.JSONObject;
import org.json.JSONTokener;
import Waiters.TelegramBotSender;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class EndWatcher {
    private static String CONFIG_PATH;
    private static String MAIN_LOG_DIR;

    private static String lastEventTime = "";
    private static ExecutorService executor;
    private static volatile boolean running = false;
    @Getter
    private static volatile boolean stoppedSuccessfully = false;
    private static final boolean notifyOnSuccess = ConfigManager.loadConfig().isSuccessNotification();

    public static void initFromConfig(LauncherConfig config) {
        String basePath = config.getStarRailCopilotPath();
        CONFIG_PATH = basePath + "/config/src.json";
        MAIN_LOG_DIR = basePath + "/log";
    }

    public static synchronized void startAsync() {
        stop();
        running = true;
        stoppedSuccessfully = false;
        executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            try {
                File logFile;
                try (Stream<Path> files = Files.list(Paths.get(MAIN_LOG_DIR))) {
                    logFile = files
                            .filter(p -> p.getFileName().toString().endsWith("_src.txt"))
                            .map(Path::toFile)
                            .max(Comparator.comparingLong(File::lastModified))
                            .orElseThrow(() -> new RuntimeException("Не найден файл *_src.txt в " + MAIN_LOG_DIR));
                }

                System.out.println("▶ EndWatcher запущен для файла: " + logFile);

                Tailer tailer = Tailer.builder()
                        .setFile(logFile)
                        .setDelayDuration(java.time.Duration.ofSeconds(1))
                        .setCharset(StandardCharsets.UTF_8)
                        .setTailerListener(new LogTailerListener())
                        .setTailFromEnd(true)
                        .get();

                tailer.run();
            } catch (IOException e) {
                TelegramBotSender.sendText("Ошибка в EndWatcher: " + e.getMessage());
            }
        });
    }

    public static synchronized void stop() {
        running = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor = null;
        }
    }

    static class LogTailerListener extends TailerListenerAdapter {
        @Override
        public void handle(String line) {
            if (!running) return;

            if (line.contains("Emulator stopped successfully")) {
                stoppedSuccessfully = true;
                String ts = extractTimestamp(line);
                if (!ts.equals(lastEventTime)) {
                    lastEventTime = ts;

                    String successMessage = LauncherConfig.getRandomMessage(
                            ConfigManager.loadConfig().getSuccessMessages());

                    if (notifyOnSuccess) {
                        String msg = KaomojiHelper.randomKaomoji() + " " +
                                successMessage + "\n" + readProgressFromJson();
                        TelegramBotSender.sendText(msg);
                    }

                }
            }
        }
    }

    private static String extractTimestamp(String line) {
        return line.length() >= 23 ? line.substring(0, 23) : "???";
    }

    private static ProgressInfo readProgressFromJson() {
        try (FileReader reader = new FileReader(CONFIG_PATH)) {
            JSONObject root = new JSONObject(new JSONTokener(reader));

            JSONObject daily = root.getJSONObject("DailyQuest")
                    .getJSONObject("DailyStorage")
                    .getJSONObject("DailyActivity");
            int dailyVal = daily.getInt("value");
            int dailyTotal = daily.getInt("total");

            long creditVal = 0;
            int jadeVal = 0;

            JSONObject dataUpdate = root.optJSONObject("DataUpdate");
            if (dataUpdate != null) {
                JSONObject itemStorage = dataUpdate.optJSONObject("ItemStorage");
                if (itemStorage != null) {
                    JSONObject credit = itemStorage.optJSONObject("Credit");
                    if (credit != null) {
                        creditVal = credit.optLong("value", 0);
                    }

                    JSONObject jade = itemStorage.optJSONObject("StallerJade");
                    if (jade != null) {
                        jadeVal = jade.optInt("value", 0);
                    }
                }
            }

            return new ProgressInfo(dailyVal, dailyTotal, creditVal, jadeVal);

        } catch (Exception e) {
            return new ProgressInfo(0, 0, 0, 0);
        }
    }

}
