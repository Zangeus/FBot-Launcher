package Utils;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;

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

    public static String getWindowTitleByPID(long pid) {
        final StringBuilder windowTitle = new StringBuilder();

        User32.INSTANCE.EnumWindows((HWND hWnd, Pointer data) -> {
            IntByReference lpdwProcessId = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hWnd, lpdwProcessId);

            if (lpdwProcessId.getValue() == (int) pid) {
                char[] buffer = new char[1024];
                User32.INSTANCE.GetWindowText(hWnd, buffer, 1024);
                String title = Native.toString(buffer);
                if (!title.isEmpty()) {
                    windowTitle.append(title);
                    return false; // нашли → стоп
                }
            }
            return true;
        }, null);

        return windowTitle.toString();
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

    public static byte[] captureWindowScreenshot(WinDef.HWND hwnd) {
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;

        WinDef.HDC windowDC = User32.INSTANCE.GetDC(hwnd);
        WinDef.HDC memDC = GDI32.INSTANCE.CreateCompatibleDC(windowDC);
        WinDef.HBITMAP outputBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(windowDC, width, height);

        WinNT.HANDLE oldBitmap = GDI32.INSTANCE.SelectObject(memDC, outputBitmap);
        User32.INSTANCE.PrintWindow(hwnd, memDC, 0);

        WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
        bmi.bmiHeader.biSize = bmi.bmiHeader.size();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = -height; // отрицательный => без переворота
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        int bufferSize = width * height * 4;
        com.sun.jna.Memory buffer = new com.sun.jna.Memory(bufferSize);

        GDI32.INSTANCE.GetDIBits(memDC, outputBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);
        int[] pixels = buffer.getIntArray(0, width * height);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);

        GDI32.INSTANCE.SelectObject(memDC, oldBitmap);
        GDI32.INSTANCE.DeleteObject(outputBitmap);
        GDI32.INSTANCE.DeleteDC(memDC);
        User32.INSTANCE.ReleaseDC(hwnd, windowDC);

        return convertToByteArray(image);
    }

    private static byte[] convertToByteArray(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("Ошибка конвертации скриншота окна: " + e.getMessage());
            return new byte[0];
        }
    }

    public static byte[] captureWindowAltPrintScreen(WinDef.HWND hwnd) {
        try {
            if (hwnd == null) return null;

            // Фокусируем окно
            WindowUtils.focusWindow(hwnd);

            Robot robot = new Robot();

            // Alt + PrintScreen
            robot.keyPress(KeyEvent.VK_ALT);
            robot.keyPress(KeyEvent.VK_PRINTSCREEN);
            robot.keyRelease(KeyEvent.VK_PRINTSCREEN);
            robot.keyRelease(KeyEvent.VK_ALT);

            Thread.sleep(200); // ждём буфер обмена

            // Получаем картинку из буфера обмена
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable content = clipboard.getContents(null);
            if (content != null && content.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                BufferedImage image = (BufferedImage) content.getTransferData(DataFlavor.imageFlavor);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                return baos.toByteArray();
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
