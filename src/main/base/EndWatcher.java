import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.json.JSONObject;
import org.json.JSONTokener;

import static Waiters.TelegramBotSender.sendText;

public class EndWatcher {

    private static final String LOG_DIR = "Q:/Z-folder/Bot_time/StarRailCopilot/log";
    private static final String CONFIG_PATH = "Q:/Z-folder/Bot_time/StarRailCopilot/config/src.json";

    private static String lastEventTime = "";

    public static void main(String[] args) throws Exception {
        File logFile;
        try (Stream<Path> files = Files.list(Paths.get(LOG_DIR))) {
            logFile = files
                    .filter(p -> p.getFileName().toString().endsWith("_src.txt"))
                    .map(Path::toFile)
                    .max((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()))
                    .orElseThrow(() -> new RuntimeException("Не найден файл *_src.txt в " + LOG_DIR));
        }

        System.out.println("▶ Запущен мониторинг (tail -f) файла: " + logFile);

        Tailer tailer = Tailer.builder()
                .setFile(logFile)
                .setDelayDuration(java.time.Duration.ofSeconds(1)) // проверка раз в сек
                .setCharset(StandardCharsets.UTF_8)
                .setTailerListener(new LogTailerListener())
                .setTailFromEnd(true) // начинать с конца файла
                .get();

        Thread thread = new Thread(tailer);
        thread.setDaemon(true);
        thread.start();

        Thread.currentThread().join();
    }


    static class LogTailerListener extends TailerListenerAdapter {
        private boolean stopDetected = false;
        private String ts = null;

        @Override
        public void handle(String line) {
            if (line.contains("EMULATOR STOP")) {
                ts = extractTimestamp(line);
                stopDetected = true;
            }
            if (stopDetected && line.contains("Emulator stopped successfully")) {
                if (!ts.equals(lastEventTime)) {
                    lastEventTime = ts;
                    String progress = readProgressFromJson();
                    sendText("✅ Эмулятор остановлен успешно!\n⏱ " + ts + "\n" + progress);
                }
                stopDetected = false;
            }
        }
    }

    private static String extractTimestamp(String line) {
        return line.length() >= 23 ? line.substring(0, 23) : "???";
    }

    private static String readProgressFromJson() {
        try (FileReader reader = new FileReader(CONFIG_PATH)) {
            JSONObject root = new JSONObject(new JSONTokener(reader));

            // --- DailyActivity ---
            JSONObject daily = root.getJSONObject("DailyQuest")
                    .getJSONObject("DailyStorage")
                    .getJSONObject("DailyActivity");
            int dailyVal = daily.getInt("value");
            int dailyTotal = daily.getInt("total");

            // --- Credit / Jade ---
            String creditStr = "- Credit: нет данных";
            String jadeStr = "- Jade: нет данных";
            JSONObject dataUpdate = root.optJSONObject("DataUpdate");
            if (dataUpdate != null) {
                JSONObject itemStorage = dataUpdate.optJSONObject("ItemStorage");
                if (itemStorage != null) {
                    JSONObject credit = itemStorage.optJSONObject("Credit");
                    if (credit != null) {
                        long creditVal = credit.optLong("value", 0);
                        creditStr = "- Credit: " + creditVal;
                    }

                    JSONObject jade = itemStorage.optJSONObject("StallerJade"); // проверь опечатку
                    if (jade != null) {
                        int jadeVal = jade.optInt("value", 0);
                        jadeStr = "- Jade: " + jadeVal;
                    }
                }
            }

            return "📊 Прогресс:\n" +
                    "- DailyActivity: " + dailyVal + "/" + dailyTotal + "\n" +
                    creditStr + "\n" +
                    jadeStr;
        } catch (Exception e) {
            System.out.println("Ошибка JSON: " + e.getMessage());
            return "⚠️ Не удалось прочитать прогресс";
        }
    }


}
