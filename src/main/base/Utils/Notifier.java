package Utils;

import Waiters.TelegramBotSender;
import com.sun.jna.platform.win32.WinDef;

import java.util.HashSet;
import java.util.List;
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
                    byte[] screenshot = WindowUtils.captureWindowScreenshot(hwnd);
                    TelegramBotSender.sendPhoto(screenshot, "Скриншот окна: эмулятор");
                }
            } else {
                message.append("- Эмулятор не активен\n");
            }

            // --- Если ни одного окна ---
            if (srcWindows.isEmpty() && emulatorWindows.isEmpty()) {
                byte[] screenshot = Extractor.captureScreenshot();
                TelegramBotSender.sendPhoto(screenshot, "Ни одно окно не найдено. Скриншот всего экрана.");
            }

            // --- Итог ---
            TelegramBotSender.sendText(message.toString());

        } catch (Exception e) {
            System.err.println("Ошибка Notifier: " + e.getMessage());
            TelegramBotSender.sendText("Ошибка! Не удалось отправить уведомление о сбое");
        }
    }
}
