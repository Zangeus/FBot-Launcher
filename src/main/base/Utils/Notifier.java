package Utils;

import Processes.Errors.ErrorMonitoring;
import Waiters.TelegramBotSender;

import java.io.File;


public class Notifier {

    // простой текст
    public static void notifyFailure(String message) {
        File logFile = ErrorMonitoring.getCurrentLog();
        File screenshot = new File("screenshots/last.png");

        if (logFile != null && logFile.exists() && screenshot.exists()) {
            TelegramBotSender.sendAlbum(message, logFile, screenshot);
        } else if (logFile != null && logFile.exists()) {
            TelegramBotSender.send(logFile, message);
        } else {
            TelegramBotSender.sendText(message);
        }
    }

    // текст + две фотки (например srcLog + screenshot из эмулятора)
    public static void notifyFailure(String message, File file1, File file2) {
        if (file1 != null && file1.exists() && file2 != null && file2.exists()) {
            TelegramBotSender.sendAlbum(message, file1, file2);
        } else if (file1 != null && file1.exists()) {
            TelegramBotSender.send(file1, message);
        } else {
            TelegramBotSender.sendText(message);
        }
    }

    // если надо именно с папкой (как у тебя с error/)
    public static void notifyFailureWithFolder(String message, File folder) {
        try {
            File[] files = folder.listFiles();
            if (files != null && files.length >= 2) {
                TelegramBotSender.sendAlbum(message, files[0], files[1]);
            } else if (files != null && files.length == 1) {
                TelegramBotSender.send(files[0], message);
            } else {
                TelegramBotSender.sendText(message + "\n(Файлов нет)");
            }
        } catch (Exception e) {
            TelegramBotSender.sendText(message + "\n(Ошибка при чтении папки: " + e.getMessage() + ")");
        }
    }
}
