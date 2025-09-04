package Utils;

import Waiters.TelegramBotSender;

import java.io.File;

public class Notifier {

    // простой текст
    public static void notifyFailure(String message) {
        TelegramBotSender.sendText(message);
    }

    // текст + две фотки (например srcLog + screenshot из эмулятора)
    public static void notifyFailure(String message, File file1, File file2) {
        if (file1 != null && file2 != null) {
            TelegramBotSender.sendAlbum(message, file1, file2);
        } else {
            // если фотки не передали – шлём обычный текст
            TelegramBotSender.sendText(message);
        }
    }

    // если надо именно с папкой (как у тебя с error/)
    public static void notifyFailureWithFolder(String message, File folder) {
        try {
            File[] files = folder.listFiles();
            if (files != null && files.length >= 2) {
                // берём первые 2 файла из папки и шлём альбом
                TelegramBotSender.sendAlbum(message, files[0], files[1]);
            } else {
                TelegramBotSender.sendText(message + "\n(Файлов нет или меньше 2)");
            }
        } catch (Exception e) {
            TelegramBotSender.sendText(message + "\n(Ошибка при чтении папки: " + e.getMessage() + ")");
        }
    }
}
