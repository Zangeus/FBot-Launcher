package Utils;

import Waiters.TelegramBotSender;
import com.sun.jna.platform.win32.WinDef;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Notifier {

    private static final Set<String> sentMessages = new HashSet<>();

    public static void notifyFailure(String failMessage) {
        try {
            // --- Проверка на дубли ---
            if (failMessage != null && sentMessages.contains(failMessage)) {
                System.out.println("Повторное сообщение пропущено: " + failMessage);
                return;
            }
            sentMessages.add(failMessage);

            StringBuilder message = new StringBuilder();
            if (failMessage != null) {
                message.append("❌ ").append(failMessage).append("\n\n");
            }
            message.append("Состояние окон:\n");

            // --- SRC ---
            List<WinDef.HWND> srcWindows = WindowUtils.findWindowsByTitlePart("SRC");
            if (!srcWindows.isEmpty()) {
                message.append("- Приложение 'src' активно\n");
                for (WinDef.HWND hwnd : srcWindows) {
                    WindowUtils.focusWindow(hwnd);
                    Thread.sleep(500);
                    byte[] screenshot = WindowUtils.captureWindowScreenshot(hwnd);
                    TelegramBotSender.sendPhoto(screenshot, "Скриншот окна: src");
                }
            } else {
                message.append("- Приложение 'src' не активно\n");
            }

            // --- Эмулятор ---
            List<WinDef.HWND> emulatorWindows = WindowUtils.findWindowsByTitlePart("Android Device");
            if (emulatorWindows.isEmpty()) {
                emulatorWindows = WindowUtils.findWindowsByTitlePart("MuMu");
            }

            if (!emulatorWindows.isEmpty()) {
                message.append("- Эмулятор активен\n");
                for (WinDef.HWND hwnd : emulatorWindows) {
                    WindowUtils.focusWindow(hwnd);
                    Thread.sleep(500);
                    byte[] screenshot = WindowUtils.captureWindowScreenshot(hwnd);
                    TelegramBotSender.sendPhoto(screenshot, "Скриншот окна: эмулятор");
                }
            } else {
                message.append("- Эмулятор не активен\n");
            }

            // --- Если ни одного окна ---
            if (srcWindows.isEmpty() && emulatorWindows.isEmpty()) {
                byte[] screenshot = captureScreenshot();
                TelegramBotSender.sendPhoto(screenshot, "Ни одно окно не найдено. Скриншот всего экрана.");
            }

            // --- Итог ---
            TelegramBotSender.sendText(message.toString());

        } catch (Exception e) {
            System.err.println("Ошибка Notifier: " + e.getMessage());
            TelegramBotSender.sendText("Ошибка! Не удалось отправить уведомление о сбое");
        }
    }

    public static void notifyFailureWithFolder(String failMessage, File folder) {
        notifyFailure(failMessage);

        if (folder != null && folder.exists() && folder.isDirectory()) {
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile()) {
                    if (file.getName().matches(".*\\.(png|jpg|jpeg|gif)$")) {
                        TelegramBotSender.sendLocalPhoto(file.getAbsolutePath());
                    } else {
                        TelegramBotSender.sendDocument(file);
                    }
                }
            }
        }
    }


    public static byte[] captureScreenshot() throws AWTException, IOException {
        Robot robot = new Robot();
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage screenshot = robot.createScreenCapture(screenRect);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(screenshot, "png", baos);
            return baos.toByteArray();
        }
    }
}
