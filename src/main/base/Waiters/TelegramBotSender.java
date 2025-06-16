package Waiters;

import Config.ConfigManager;
import Config.LauncherConfig;
import Utils.Extractor;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

public class TelegramBotSender {
    private static final LauncherConfig config = ConfigManager.loadConfig();
    private static final Random random = new Random();
    private static final String API_URL = "https://api.telegram.org/bot";

    public static void sendMessages(List<String> messages) {
        if (!validateConfig()) return;

        String message = getRandomMessage(messages);
        if (isInvalidMessage(message)) {
            System.out.println("Пустое сообщение - отправка отменена");
            return;
        }

        sendRequest("sendMessage", "text", message);
    }

    private static boolean validateConfig() {
        if (config.getBotToken() == null || config.getBotToken().isEmpty()) {
            System.err.println("Токен бота не настроен!");
            return false;
        }

        if (config.getChatId() == null || config.getChatId().isEmpty()) {
            System.err.println("Chat ID не настроен!");
            return false;
        }

        return true;
    }

    private static String getRandomMessage(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return config.isDebugMode()
                    ? "Нет доступных сообщений в конфигурации"
                    : "";
        }
        return messages.get(random.nextInt(messages.size()));
    }

    private static boolean isInvalidMessage(String message) {
        return message == null || message.isEmpty() || message.equals("Нет доступных сообщений");
    }

    private static void sendRequest(String method, String paramType, String content) {
        try {
            String urlString = API_URL + config.getBotToken() + "/" + method;
            String postData = String.format(
                    "chat_id=%s&%s=%s",
                    URLEncoder.encode(config.getChatId(), StandardCharsets.UTF_8),
                    paramType,
                    URLEncoder.encode(content, StandardCharsets.UTF_8)
            );

            HttpURLConnection connection = createConnection(urlString);
            sendPostData(connection, postData);
            handleResponse(connection);
        } catch (Exception e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    private static HttpURLConnection createConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        return connection;
    }

    private static void sendPostData(HttpURLConnection connection, String postData) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = postData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    }

    private static void handleResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            System.err.println("Ошибка Telegram API. Код: " + responseCode);
            readErrorResponse(connection);
        }
        connection.disconnect();
    }

    private static void readErrorResponse(HttpURLConnection connection) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8)
        )) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.err.println("Ответ сервера: " + response);
        } catch (IOException e) {
            System.err.println("Ошибка чтения ответа об ошибке: " + e.getMessage());
        }
    }

    // Метод для прямой отправки фото (используется в Extractor)
    public static void sendPhoto(byte[] imageBytes, String caption) {
        if (!config.shouldSendFailureReport()) return;

        try {
            Extractor.sendPhotoWithCaption(
                    imageBytes,
                    caption,
                    config.getBotToken(),
                    config.getChatId()
            );
        } catch (Exception e) {
            System.err.println("Ошибка отправки фото: " + e.getMessage());
        }
    }

    public static void sendExtraMessage(List<String> messages, String extraMessage) {
        if (!validateConfig()) return;

        String message = getRandomMessage(messages);
        if (isInvalidMessage(message)) {
            System.out.println("Пустое сообщение - отправка отменена");
            return;
        }
        sendRequest("sendMessage", "text", message + "\nПримечание: " + extraMessage);
    }

    public static void sendNoteMessage(String extraMessage) {
        if (!validateConfig()) return;

        sendRequest("sendMessage", "text", "Примечание: " + extraMessage);
    }

    public static void sendPhotoUrl(String imageUrl) {
        if (!validateConfig()) return;
        sendRequest("sendPhoto", "photo", imageUrl);
    }

    public static void sendLocalPhoto(String imagePath) {
        if (!validateConfig()) return;

        try {
            File imageFile = new File(imagePath);

            // Исправление: получаем Path из File
            Path filePath = imageFile.toPath();
            byte[] imageBytes = Files.readAllBytes(filePath);

            String urlString = API_URL + config.getBotToken() + "/sendPhoto";
            HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();

            // Формируем multipart-запрос
            String boundary = "Boundary-" + System.currentTimeMillis();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {

                // Параметр chat_id
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
                writer.append(config.getChatId()).append("\r\n").flush();

                // Файл с изображением
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"photo\"; filename=\"")
                        .append(imageFile.getName()).append("\"\r\n");
                writer.append("Content-Type: image/png\r\n\r\n").flush();
                os.write(imageBytes);
                os.flush();

                // Завершаем boundary
                writer.append("\r\n--").append(boundary).append("--\r\n").flush();
            }

            handleResponse(connection);
        } catch (IOException e) {
            System.err.println("Ошибка отправки локального фото: " + e.getMessage());
        }
    }
}