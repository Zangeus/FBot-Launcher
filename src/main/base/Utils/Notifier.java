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

    private static File captureAndSave(String windowPart, String filename) {
        try {
            List<WinDef.HWND> windows = WindowUtils.findWindowsByTitlePart(windowPart);
            if (windows.isEmpty()) {
                System.out.println("[WARN] Окно не найдено: " + windowPart);
                return null;
            }

            WinDef.HWND hwnd = windows.get(0);
            System.out.println("[INFO] Нашли окно \"" + windowPart + "\" → " + hwnd);

            WindowUtils.focusWindow(hwnd);

            byte[] screenshot = WindowUtils.captureWindowAltPrintScreen(hwnd);
            if (screenshot.length == 0) {
                TelegramBotSender.sendText("[ERROR] Скриншот пустой для окна: " + windowPart);
                return null;
            }
            System.out.println("[INFO] Скриншот сделан (" + screenshot.length + " байт)");

            File outFile = new File(filename);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(screenshot);
            }
            System.out.println("[INFO] Скриншот сохранён: " + outFile.getAbsolutePath());

            return outFile;

        } catch (Exception e) {
            TelegramBotSender.sendText("[EXCEPTION] Ошибка при скриншоте окна \"" + windowPart + "\": " + e.getMessage());
            return null;
        }
    }


    public static void notifyFailure(String message) {
        String finalMessage = prepareMessage(message);

        // Скриншоты окон
        File srcScreenshot = captureAndSave("SRC", "src.png");
        File androidScreenshot = captureAndSave("Android Device", "android.png");

        // Лог: последние 400 строк во временный файл
        File tempLog = null;
        try {
            File logFile = ErrorMonitoring.getCurrentLog();
            if (logFile != null && logFile.exists()) {
                tempLog = LogUtils.getLastLines(logFile, 400);
            }

            // Собираем скриншоты и фильтруем только валидные
            List<File> screenshots = new ArrayList<>();
            if (isValidImage(srcScreenshot)) screenshots.add(srcScreenshot);
            if (isValidImage(androidScreenshot)) screenshots.add(androidScreenshot);

            // Отправка скриншотов
            if (screenshots.size() > 1) {
                TelegramBotSender.sendAlbum(finalMessage, screenshots.toArray(new File[0]));
            } else if (screenshots.size() == 1) {
                TelegramBotSender.send(screenshots.get(0), finalMessage);
            }

            // Отправка лога отдельно
            if (tempLog != null && tempLog.exists() && tempLog.length() > 0) {
                TelegramBotSender.sendDocument(tempLog);
            }

            // Если вообще нет ни скринов, ни лога, просто текст
            if (screenshots.isEmpty() && (tempLog == null || !tempLog.exists())) {
                TelegramBotSender.sendText(finalMessage);
            }

        } catch (Exception e) {
            TelegramBotSender.sendText(finalMessage + "\n(Ошибка при подготовке файлов: " + e.getMessage() + ")");
        } finally {
            if (tempLog != null && tempLog.exists() && !tempLog.delete()) {
                System.err.println("⚠ Не удалось удалить временный файл: " + tempLog.getAbsolutePath());
            }
            if (srcScreenshot != null && srcScreenshot.exists() && !srcScreenshot.delete()) {
                System.err.println("⚠ Не удалось удалить скриншот: " + srcScreenshot.getAbsolutePath());
            }
            if (androidScreenshot != null && androidScreenshot.exists() && !androidScreenshot.delete()) {
                System.err.println("⚠ Не удалось удалить скриншот: " + androidScreenshot.getAbsolutePath());
            }
        }
    }

    private static boolean isValidImage(File f) {
        if (f == null || !f.exists() || f.length() == 0) return false;
        String name = f.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
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
