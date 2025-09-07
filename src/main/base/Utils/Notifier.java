package Utils;

import Config.ConfigManager;
import Config.LauncherConfig;
import Processes.Errors.ErrorMonitoring;
import Waiters.TelegramBotSender;
import com.sun.jna.platform.win32.WinDef;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class Notifier {
    // простой текст
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

    private static File captureAndSave(String windowPart, String filename) {
        try {
            // ищем окно
            List<WinDef.HWND> windows = WindowUtils.findWindowsByTitlePart(windowPart);
            if (windows.isEmpty()) {
                System.out.println("[WARN] Окно не найдено: " + windowPart);
                return null;
            }

            WinDef.HWND hwnd = windows.get(0);
            System.out.println("[INFO] Нашли окно \"" + windowPart + "\" → " + hwnd);

            // фокусируем окно (чтобы PrintWindow сработал надёжнее)
            WindowUtils.focusWindow(hwnd);

            // пробуем сделать скриншот
            byte[] screenshot = WindowUtils.captureWindowScreenshot(hwnd);
            if (screenshot == null || screenshot.length == 0) {
                System.out.println("[ERROR] Скриншот пустой для окна: " + windowPart);
                return null;
            }
            System.out.println("[INFO] Скриншот сделан (" + screenshot.length + " байт)");

            // сохраняем во временный файл
            File outFile = new File(filename);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(screenshot);
            }
            System.out.println("[INFO] Скриншот сохранён: " + outFile.getAbsolutePath());

            return outFile;

        } catch (Exception e) {
            System.out.println("[EXCEPTION] Ошибка при скриншоте окна \"" + windowPart + "\": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    public static void notifyFailure(String message) {
        String finalMessage = prepareMessage(message);

        // Скриншоты окон
        File srcScreenshot = captureAndSave("SRC", "src.png");
        File androidScreenshot = captureAndSave("Android Device", "android.png");

        // Лог-файл
        File logFile = ErrorMonitoring.getCurrentLog();

        // Отправляем альбом только со скриншотами
        List<File> screenshots = new ArrayList<>();
        if (srcScreenshot != null && srcScreenshot.exists() && srcScreenshot.length() > 0) {
            screenshots.add(srcScreenshot);
        }
        if (androidScreenshot != null && androidScreenshot.exists() && androidScreenshot.length() > 0) {
            screenshots.add(androidScreenshot);
        }

        if (!screenshots.isEmpty()) {
            TelegramBotSender.sendAlbum(finalMessage, screenshots.toArray(new File[0]));
        }

        // Отправляем лог-файл отдельным сообщением, если он есть
        if (logFile != null && logFile.exists() && logFile.length() > 0) {
            TelegramBotSender.send(logFile, "Лог ошибки");
        }

        // Если ничего нет, просто отправляем текст
        if (screenshots.isEmpty() && (logFile == null || !logFile.exists())) {
            TelegramBotSender.sendText(finalMessage);
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

        // Отправляем альбом с картинками
        if (!images.isEmpty()) {
            TelegramBotSender.sendAlbum(message, images.toArray(new File[0]));
        }

        // Отправляем все остальные файлы как документы
        for (File f : otherFiles) {
            TelegramBotSender.send(f, "Файл из папки: " + f.getName());
        }

        // Если вообще ничего не отправлено
        if (images.isEmpty() && otherFiles.isEmpty()) {
            TelegramBotSender.sendText(message + "\n(Нет валидных файлов для отправки)");
        }
    }


}
