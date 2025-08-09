package Utils;

import Config.ConfigManager;
import Config.LauncherConfig;
import Waiters.TelegramBotSender;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
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
                // Корректируем координаты на центр кнопки
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
        // Конвертируем в правильный формат если нужно
        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            BufferedImage converted = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_3BYTE_BGR
            );
            converted.getGraphics().drawImage(image, 0, 0, null);
            image = converted;
        }

        // Получаем данные изображения
        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        // Создаем Mat и заполняем данными
        Mat mat = new Mat(
                image.getHeight(),
                image.getWidth(),
                CvType.CV_8UC3
        );
        mat.put(0, 0, pixels); // Возвращаем Mat, а не результат put()

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
                List<byte[]> screenshots = new ArrayList<>();
                StringBuilder message = new StringBuilder("Ошибка в работе приложения! Состояние окон:\n");

                // Проверяем и обрабатываем окно приложения "src"
                WinDef.HWND appHwnd = User32.INSTANCE.FindWindow(null, "src");
                if (appHwnd != null && User32.INSTANCE.IsWindowVisible(appHwnd)) {
                    focusWindow(appHwnd);
                    screenshots.add(captureWindowScreenshot(appHwnd));
                    message.append("- Приложение 'src' активно\n");
                } else {
                    message.append("- Приложение 'src' не активно\n");
                }

                // Проверяем и обрабатываем окно эмулятора "MuMu Player 12"
                WinDef.HWND emulatorHwnd = User32.INSTANCE.FindWindow(null, "MuMu Player 12");
                if (emulatorHwnd != null && User32.INSTANCE.IsWindowVisible(emulatorHwnd)) {
                    focusWindow(emulatorHwnd);
                    screenshots.add(captureWindowScreenshot(emulatorHwnd));
                    message.append("- Эмулятор 'MuMu Player 12' активен\n");
                } else {
                    message.append("- Эмулятор 'MuMu Player 12' не активен\n");
                }

                // Если оба окна не найдены, делаем скриншот всего экрана
                if (screenshots.isEmpty()) {
                    screenshots.add(Extractor.captureScreenshot());
                    message.append("\nНи одно из целевых окон не найдено. Скриншот всего экрана.");
                }

                TelegramBotSender.sendPhotos(screenshots, message.toString());

            } catch (Exception e) {
                System.err.println("Ошибка обработки сбоя: " + e.getMessage());
                TelegramBotSender.sendMessages(List.of("Ошибка! Не удалось обработать сбой"));
            }
        }
    }

    private static void focusWindow(WinDef.HWND hwnd) {
        if (User32.INSTANCE.GetForegroundWindow() != hwnd) {
            User32.INSTANCE.ShowWindow(hwnd, WinUser.SW_RESTORE);
            User32.INSTANCE.SetForegroundWindow(hwnd);
            robot.delay(500); // Даем время для фокусировки
        }
    }

    private static byte[] captureWindowScreenshot(WinDef.HWND hwnd) {
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);

        Rectangle area = new Rectangle(
                rect.left,
                rect.top,
                rect.right - rect.left,
                rect.bottom - rect.top
        );

        BufferedImage image = robot.createScreenCapture(area);
        return convertToByteArray(image);
    }

    // Конвертация BufferedImage в byte[]
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

    public static boolean findAndClickWithMessage(String image, String message) {
        boolean result = findAndClick(image);

        if (!result)
            if (config.isFailureNotification())
                TelegramBotSender.sendExtraMessage(config.getReportMessages(), message);

        return result;
    }

    public static boolean findAndClickForTasks(String image) {
        boolean result = findAndClick(image);

        if (!result)
            if (config.isFailureNotification())
                TelegramBotSender.sendMessages(config.getReportMessages());

        return result;
    }

    public static boolean findAndClickWithOneMessage(String image, String message) {
        boolean result = findAndClick(image);

        if (!result)
            if (config.isFailureNotification())
                TelegramBotSender.sendNoteMessage(message);

        return result;
    }

    public static boolean findAndClickWithDelay(String image, int delayMs) {
        boolean result = findAndClick(image);
        if (result)
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        return result;
    }

    public static boolean findAndClickWithOneMessageAndDelay(String image, String message, int delayMs) {
        boolean result = findAndClick(image);

        if (result)
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        else if (config.isFailureNotification())
            TelegramBotSender.sendNoteMessage(message);

        return result;
    }

    public static boolean findAndClickWithMessageAndDelay(String image, String message, int delayMs) {
        boolean result = findAndClick(image);

        if (result)
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        else if (config.isFailureNotification())
            TelegramBotSender.sendExtraMessage(config.getReportMessages(), message);

        return result;
    }


}