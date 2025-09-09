package Utils;

import Config.ConfigManager;
import Config.LauncherConfig;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
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

    // === Публичные методы для вызова ===

    // Просто найти и кликнуть (с логами)
    public static boolean findAndClick(String image) {
        return findAndClick(image, true, false, null);
    }

    // Найти без логов
    public static boolean findAndClickSilent(String image) {
        return findAndClick(image, false, false, null);
    }

    // Найти и при фейле кинуть одно сообщение
    public static boolean findAndClickWithMessage(String image, String message) {
        return findAndClick(image, true, true, message);
    }

    // === Найти и при полном фейле кинуть сообщение и скрин ===
    public static boolean findAndClickWithReport(String image, String failMessage) {
        return findAndClick(image, true, true, failMessage);
    }

    // === Универсальная реализация ===
    private static boolean findAndClick(String image, boolean log, boolean reportOnFail, String failMessage) {
        synchronized (searchLock) {
            String path = config.getPicsToStartPath() + "/" + image;
            if (log) System.out.println("Проверка: " + path);

            boolean result = findAndClickLogic(path);

            if (log) System.out.println(result ? "✓ Обнаружено" : "✗ Не найдено");

            // если полностью провалились — дергаем Notifier
            if (!result && reportOnFail && config.isFailureNotification()) {
                Notifier.notifyMessageFailure(failMessage != null ? failMessage : "Кнопка не найдена: " + image);
            }


            return result;
        }
    }


    // === Логика поиска кнопки ===
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
}
