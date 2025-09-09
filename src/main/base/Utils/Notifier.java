package Utils;

import Config.ConfigManager;
import Config.LauncherConfig;
import Processes.Errors.ErrorMonitoring;
import Waiters.TelegramBotSender;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static Utils.WindowUtils.captureWindowScreenshot;


public class Notifier {
    private static String prepareMessage(String message) {
        String randomMessage = LauncherConfig.getRandomMessage(
                ConfigManager.loadConfig().getFailureMessages()
        );
        if (message == null || message.isBlank()) {
            return randomMessage;
        } else {
            return message + "\n\n" + randomMessage;
        }
    }

    public static void notifyFailure(String message) {
        String finalMessage = prepareMessage(message);

        byte[] srcScreenshot = captureWindowScreenshot("SRC");
        byte[] androidScreenshot = captureWindowScreenshot("Android Device");

        List<byte[]> screenshots = new ArrayList<>();
        if (srcScreenshot != null && srcScreenshot.length > 0) screenshots.add(srcScreenshot);
        if (androidScreenshot != null && androidScreenshot.length > 0) screenshots.add(androidScreenshot);

        File tempLog = null;
        try {
            File logFile = ErrorMonitoring.getCurrentLog();
            if (logFile != null && logFile.exists()) {
                tempLog = LogUtils.getLastLines(logFile, 400);
            }

            if (screenshots.size() > 1) {
                TelegramBotSender.sendAlbum(finalMessage, screenshots);
            } else if (screenshots.size() == 1) {
                TelegramBotSender.sendImageBytes(screenshots.get(0), finalMessage);
            } else TelegramBotSender.sendText(finalMessage + "\n\n「エクス・ログ」 Окна не были найдены, スクショなし");

            if (tempLog != null && tempLog.exists() && tempLog.length() > 0) {
                TelegramBotSender.sendDocument(tempLog);
            }
            if (screenshots.isEmpty() && (tempLog == null || !tempLog.exists())) {
                TelegramBotSender.sendText(finalMessage);
            }

        } catch (Exception e) {
            TelegramBotSender.sendText(finalMessage + "\n(Ошибка при подготовке файлов: " + e.getMessage() + ")");
        } finally {
            if (tempLog != null && tempLog.exists() && !tempLog.delete()) {
                System.err.println("⚠ Не удалось удалить временный файл: " + tempLog.getAbsolutePath());
            }
        }
    }

    public static void notifyFailureWithFolder(String message, File folder) {
        String randomMessage = LauncherConfig.getRandomMessage(
                ConfigManager.loadConfig().getFailureMessages());

        if (message == null || message.isBlank()) message = randomMessage;
        else message = message + "\n\n" + randomMessage;

        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            TelegramBotSender.sendText(message + "\n(Папка не найдена)");
            return;
        }

        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            TelegramBotSender.sendText(message + "\n(Файлов нет)");
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
            TelegramBotSender.send(f, "Файл из папки: " + f.getName());
        }

        if (images.isEmpty() && otherFiles.isEmpty()) {
            TelegramBotSender.sendText(message + "\n(Нет валидных файлов для отправки)");
        }
    }


}
