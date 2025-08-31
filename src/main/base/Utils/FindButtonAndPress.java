package Utils;

import Config.ConfigManager;
import Config.LauncherConfig;
import Waiters.TelegramBotSender;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.DataBufferByte;

public class FindButtonAndPress {
    private static final LauncherConfig config = ConfigManager.loadConfig();
    public static final Robot robot;
    private static final double MATCH_THRESHOLD = 0.8;
    private static final Object searchLock = new Object();

    static {
        try {
            robot = new Robot();
            nu.pattern.OpenCV.loadLocally();
        } catch (AWTException e) {
            throw new RuntimeException("Ошибка инициализации Robot: " + e.getMessage());
        }
    }

    public static boolean findAndClickFull(String imagePath) {
        synchronized (searchLock) {
            if (findAndClickLogic(imagePath)) return true;
            handleFailure();
            return false;
        }
    }

    private static Point findButton(String buttonImagePath) {
        Mat buttonImage = Imgcodecs.imread(buttonImagePath);
        if (buttonImage.empty()) {
            System.err.println("Изображение не найдено: " + buttonImagePath);
            return null;
        }

        try {
            Mat screen = captureScreen();
            Mat result = new Mat();
            Imgproc.matchTemplate(screen, buttonImage, result, Imgproc.TM_CCOEFF_NORMED);

            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            if (mmr.maxVal > MATCH_THRESHOLD) {
                int x = (int) (mmr.maxLoc.x + buttonImage.cols() / 2.0);
                int y = (int) (mmr.maxLoc.y + buttonImage.rows() / 2.0);
                return new Point(x, y);
            }
        } catch (Exception e) {
            System.err.println("Ошибка поиска: " + e.getMessage());
        }
        return null;
    }

    private static Mat captureScreen() {
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage screenImage = robot.createScreenCapture(screenRect);
        return convertToMat(screenImage);
    }

    private static Mat convertToMat(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            BufferedImage converted = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_3BYTE_BGR
            );
            Graphics g = converted.getGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            image = converted;
        }

        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);

        return mat;
    }

    private static void performClick(Point location) {
        robot.mouseMove((int) location.x, (int) location.y);
        robot.delay(50);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(100);
        System.out.printf("Клик в [X: %d, Y: %d]%n", (int) location.x, (int) location.y);
    }

    private static void sleepSafe(int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Прервано ожидание: " + e.getMessage());
        }
    }

    private static void handleFailure() {
        if (config.isFailureNotification() && config.isNotificationsEnabled()) {
            try {
                StringBuilder message = new StringBuilder("Ошибка в работе приложения! Состояние окон:\n");

                List<WinDef.HWND> srcWindows = findWindowsByTitlePart("SRC");
                if (!srcWindows.isEmpty()) {
                    message.append("- Приложение 'src' активно\n");
                    for (WinDef.HWND hwnd : srcWindows) {
                        focusWindow(hwnd);
                        byte[] screenshot = captureWindowScreenshot(hwnd);
                        TelegramBotSender.sendPhoto(screenshot, "Скриншот окна: src");
                    }
                } else {
                    message.append("- Приложение 'src' не активно\n");
                }

                List<WinDef.HWND> emulatorWindows = findWindowsByTitlePart("Android Device");
                if (emulatorWindows.isEmpty()) {
                    emulatorWindows = findWindowsByTitlePart("MuMu");
                }

                if (!emulatorWindows.isEmpty()) {
                    message.append("- Эмулятор активен\n");
                    for (WinDef.HWND hwnd : emulatorWindows) {
                        focusWindow(hwnd);
                        byte[] screenshot = captureWindowScreenshot(hwnd);
                        TelegramBotSender.sendPhoto(screenshot, "Скриншот окна: эмулятор");
                    }
                } else {
                    message.append("- Эмулятор не активен\n");
                }

                // Если ничего не нашли — общий скрин
                if (srcWindows.isEmpty() && emulatorWindows.isEmpty()) {
                    byte[] screenshot = Extractor.captureScreenshot();
                    TelegramBotSender.sendPhoto(screenshot, "Ни одно окно не найдено. Скриншот всего экрана.");
                }

                // Итоговое текстовое сообщение
                TelegramBotSender.sendText(message.toString());

            } catch (Exception e) {
                System.err.println("Ошибка обработки сбоя: " + e.getMessage());
                TelegramBotSender.sendText("Ошибка! Не удалось обработать сбой");
            }
        }
    }

    private static List<WinDef.HWND> findWindowsByTitlePart(String part) {
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

    private static void focusWindow(WinDef.HWND hwnd) {
        if (User32.INSTANCE.GetForegroundWindow() != hwnd) {
            User32.INSTANCE.ShowWindow(hwnd, WinUser.SW_RESTORE);
            User32.INSTANCE.SetForegroundWindow(hwnd);
            robot.delay(500);
        }
    }

    private static byte[] captureWindowScreenshot(WinDef.HWND hwnd) {
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;

        // DC окна
        WinDef.HDC windowDC = User32.INSTANCE.GetDC(hwnd);
        WinDef.HDC memDC = GDI32.INSTANCE.CreateCompatibleDC(windowDC);
        WinDef.HBITMAP outputBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(windowDC, width, height);

        WinNT.HANDLE oldBitmap = GDI32.INSTANCE.SelectObject(memDC, outputBitmap);

        // ⚡ рисуем окно в наш DC
        User32.INSTANCE.PrintWindow(hwnd, memDC, 0);

        // Настройка BITMAPINFO
        WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
        bmi.bmiHeader.biSize = bmi.bmiHeader.size();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = -height; // отрицательный => без переворота
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        // Буфер под пиксели
        int bufferSize = width * height * 4; // 4 байта на пиксель (ARGB)
        com.sun.jna.Memory buffer = new com.sun.jna.Memory(bufferSize);

        // Считываем данные
        GDI32.INSTANCE.GetDIBits(memDC, outputBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

        // Перегоняем в int[]
        int[] pixels = buffer.getIntArray(0, width * height);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);

        // Чистим за собой
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
            System.err.println("Ошибка конвертации изображения: " + e.getMessage());
            return new byte[0];
        }
    }

    public static boolean findAndClick(String image) {
        String path = config.getPicsToStartPath() + "/" + image;
        System.out.println("Проверка: " + path);

        boolean result = findAndClickFull(path);
        System.out.println(result ? "✓ Обнаружено" : "✗ Не найдено");
        return result;
    }

    public static boolean findAndClickScreenless(String image) {
        synchronized (searchLock) {
            String imagePath = config.getPicsToStartPath() + "/" + image;
            return findAndClickLogic(imagePath);
        }
    }

    private static boolean findAndClickLogic(String imagePath) {
        final int maxAttempts = config.getAttemptsAmount();
        final int delayMs = config.getSearchDelayMs();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.printf("Attempt %d/%d for %s%n", attempt, maxAttempts, imagePath);

            try {
                Point location = findButton(imagePath);
                if (location != null) {
                    performClick(location);
                    return true;
                }
                sleepSafe(delayMs);
            } catch (Exception e) {
                System.err.println("Search error: " + e.getMessage());
            }
        }
        return false;
    }

    public static boolean findAndClickWithOneMessage(String image, String message) {
        boolean result = findAndClick(image);

        if (!result)
            if (config.isFailureNotification())
                TelegramBotSender.sendText(message);

        return result;
    }
}
