package Utils;

import Waiters.TelegramBotSender;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HWND;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WindowUtils {

    public static List<HWND> findWindowsByTitlePart(String part) {
        List<WinDef.HWND> result = new ArrayList<>();
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String title = Native.toString(windowText);

            if (title.contains(part) && User32.INSTANCE.IsWindowVisible(hwnd)) {
                result.add(hwnd);
            }
            return true;
        }, null);
        return result;
    }

    /**
     * Фокусировка окна
     */
    public static void focusWindow(WinDef.HWND hwnd) {
        if (User32.INSTANCE.GetForegroundWindow() != hwnd) {
            User32.INSTANCE.ShowWindow(hwnd, WinUser.SW_RESTORE);
            User32.INSTANCE.SetForegroundWindow(hwnd);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void closeWindowByTitle(String windowTitle) {
        User32 user32 = User32.INSTANCE;

        HWND hwnd = user32.FindWindow(null, windowTitle);
        if (hwnd != null) {
            user32.PostMessage(hwnd, 0x10, null, null);
            System.out.println("Окно \"" + windowTitle + "\" закрывается мягко...");
        } else {
            System.out.println("Окно с заголовком \"" + windowTitle + "\" не найдено!");
        }
    }

    static byte[] captureWindowScreenshot(String windowPart) {
        try {
            List<WinDef.HWND> windows = WindowUtils.findWindowsByTitlePart(windowPart);
            if (windows.isEmpty()) {
                System.out.println("[WARN] Окно не найдено: " + windowPart);
                return null;
            }

            WinDef.HWND hwnd = windows.get(0);
            System.out.println("[INFO] Нашли окно \"" + windowPart + "\" → " + hwnd);

            BufferedImage img = captureWindowAltPrintScreen(hwnd);
            if (img == null) return null;

            return bufferedImageToPngBytes(img);

        } catch (Exception e) {
            TelegramBotSender.sendText("[EXCEPTION] Ошибка при скриншоте окна \"" + windowPart + "\": " + e.getMessage());
            return null;
        }
    }


    public static byte[] bufferedImageToPngBytes(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
    }

    public static BufferedImage captureWindowAltPrintScreen(WinDef.HWND hwnd) {
        try {
            if (hwnd == null) return null;

            WindowUtils.focusWindow(hwnd);

            Robot robot = new Robot();

            robot.keyPress(KeyEvent.VK_ALT);
            robot.keyPress(KeyEvent.VK_PRINTSCREEN);
            robot.keyRelease(KeyEvent.VK_PRINTSCREEN);
            robot.keyRelease(KeyEvent.VK_ALT);

            Thread.sleep(200); // ждём буфер обмена

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable content = clipboard.getContents(null);
            if (content != null && content.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                return (BufferedImage) content.getTransferData(DataFlavor.imageFlavor);
            } else {
                System.err.println("Буфер обмена не содержит изображение");
                return null;
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }


}
