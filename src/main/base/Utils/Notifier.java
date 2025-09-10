package Utils;

import Config.ConfigManager;
import Config.LauncherConfig;
import Processes.Errors.ErrorMonitoring;
import Waiters.TelegramBotSender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static Utils.WindowUtils.captureWindowScreenshot;


public class Notifier {

    private static final String REPORT = "REPORT";
    private static final String FAILURE = "FAILURE";
    private static File log;

    public static void getLog() {
        try {
            log = LogUtils.getLastLines(ErrorMonitoring.getCurrentLog(), 400);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void notifyFailure() {
        sendPackage(null, FAILURE);
    }

    public static void notifyMessageFailure(String message) {
        sendPackage(message, FAILURE);
    }

    public static void sendMessageReport(String message) {
        sendPackage(message, REPORT);
    }

    public static void sendReport() {
        sendPackage(null, REPORT);
    }

    public static void sendPackage(String message, String type) {
        String finalMessage = prepareMessage(message, type);
        getLog();

        byte[] androidScreenshot = captureWindowScreenshot("Android Device");

        try {
            if (androidScreenshot != null) {
                TelegramBotSender.sendAlbum(finalMessage, Collections.singletonList(androidScreenshot));
            } else TelegramBotSender.sendText(finalMessage + "\n\n「エクス・ログ」 Окно не было найдено, スクショなし");

            if (log != null && log.exists() && log.length() > 0) {
                TelegramBotSender.sendDocument(log);
            }
            if (androidScreenshot == null && (log == null || !log.exists())) {
                TelegramBotSender.sendText(finalMessage);
            }

        } catch (Exception e) {
            TelegramBotSender.sendText(finalMessage + "\n(Ошибка при подготовке файлов: " + e.getMessage() + ")");
        } finally {
            if (log != null && log.exists() && !log.delete()) {
                System.err.println("⚠ Не удалось удалить временный файл: " + log.getAbsolutePath());
            }
        }
    }

    private static String prepareMessage(String message, String type) {
        List<String> pool = type.equals(REPORT)
                ? ConfigManager.loadConfig().getReportMessages()
                : ConfigManager.loadConfig().getFailureMessages();

        String randomMessage = LauncherConfig.getRandomMessage(pool);

        if (message == null || message.isBlank()) {
            return randomMessage;
        } else {
            return message + "\n\n" + randomMessage;
        }
    }

    public static void notifyFailureWithFolder(String message, File folder) {
        String finalMessage = prepareMessage(message, FAILURE);
        getLog();

        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            TelegramBotSender.sendText(finalMessage + "\n(Папка не найдена)");
            return;
        }

        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            TelegramBotSender.sendText(finalMessage + "\n(Файлов нет)");
            return;
        }

        List<File> images = new ArrayList<>();
        List<File> otherFiles = new ArrayList<>();

        for (File f : files) {
            if (f == null || !f.exists() || f.length() == 0) continue;
            String name = f.getName().toLowerCase();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")) {
                images.add(f);
            } else {
                otherFiles.add(f);
            }
        }

        if (!images.isEmpty()) {
            TelegramBotSender.sendAlbum(message, images.toArray(new File[0]));
        }

        for (File f : otherFiles) {
            TelegramBotSender.sendDocument(f);
        }

        if (images.isEmpty() && otherFiles.isEmpty()) {
            TelegramBotSender.sendText(message + "\n(Файлы 404)");
        }
    }

}
