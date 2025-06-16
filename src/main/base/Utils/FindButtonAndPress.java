package Utils;

import Config.ConfigManager;
import Config.LauncherConfig;
import Waiters.TelegramBotSender;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;

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
                byte[] screenshot = Extractor.captureScreenshot();
                String message = "";

                TelegramBotSender.sendPhoto(screenshot, message);
            } catch (Exception e) {
                System.err.println("Ошибка отправки скриншота: " + e.getMessage());
                TelegramBotSender.sendMessages(List.of("Ошибка! Не удалось сделать скриншот"));
            }
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