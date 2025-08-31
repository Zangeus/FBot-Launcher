package Waiters;

import Config.ConfigManager;
import Config.LauncherConfig;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelegramBotSender {
    private static final String API_URL = "https://api.telegram.org/bot";
    private static final LauncherConfig config = ConfigManager.loadConfig();
    private static final Random random = new Random();

    // Пул потоков для асинхронных задач
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    // -----------------------
    // Асинхронная отправка
    // -----------------------
    public static void sendAsync(Object content, String captionOrExtra) {
        executor.submit(() -> send(content, captionOrExtra));
    }

    // -----------------------
    // Универсальный метод отправки (синхронный)
    // -----------------------
    public static void send(Object content, String captionOrExtra) {
        if (validateConfig()) return;

        try {
            if (content instanceof String) {
                String text = (String) content;
                sendTextInternal(text + (captionOrExtra != null ? ("\n" + captionOrExtra) : ""));
            } else if (content instanceof byte[]) {
                byte[] bytes = (byte[]) content;
                sendMultipart("sendPhoto", "photo", bytes, "image.png", "image/png", captionOrExtra);
            } else if (content instanceof File) {
                File file = (File) content;
                if (!file.exists()) {
                    System.err.println("Файл не найден: " + file.getAbsolutePath());
                    return;
                }
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String mime = Files.probeContentType(file.toPath());
                if (mime == null) mime = "application/octet-stream";
                String method = file.getName().matches("(?i).+\\.(png|jpg|jpeg|gif)$") ? "sendPhoto" : "sendDocument";
                String fieldName = method.equals("sendPhoto") ? "photo" : "document";
                sendMultipart(method, fieldName, fileBytes, file.getName(), mime, captionOrExtra);
            } else if (content instanceof List) {
                List<?> list = (List<?>) content;
                int index = 1;
                for (Object item : list) {
                    String caption = (captionOrExtra != null ? captionOrExtra + " (" + index + ")" : null);
                    send(item, caption);
                    index++;
                }
            } else {
                System.err.println("Неподдерживаемый тип: " + content.getClass());
            }
        } catch (IOException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }

    // -----------------------
    // Упрощённые обёртки
    // -----------------------
    public static void send(File file, String caption) {
        send((Object) file, caption);
    }

    public static void sendText(String text) {
        send(text, null);
    }

    public static void sendPhoto(byte[] imageBytes, String caption) {
        send(imageBytes, caption);
    }

    public static void sendDocument(File file) {
        send(file, null);
    }

    public static void sendLocalPhoto(String path) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(path));
            send(bytes, null);
        } catch (IOException e) {
            System.err.println("Не удалось прочитать фото: " + e.getMessage());
        }
    }

    // -----------------------
    // Внутренние реализации
    // -----------------------
    private static void sendTextInternal(String text) throws IOException {
        String urlString = API_URL + config.getBotToken() + "/sendMessage";
        String data = "chat_id=" + URLEncoder.encode(config.getChatId(), StandardCharsets.UTF_8)
                + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(data.getBytes(StandardCharsets.UTF_8));
        }

        handleResponse(conn);
    }

    private static void sendMultipart(String method, String fieldName, byte[] fileBytes,
                                      String fileName, String mimeType, String caption) throws IOException {
        String urlString = API_URL + config.getBotToken() + "/" + method;
        String boundary = "Boundary-" + System.currentTimeMillis();

        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream os = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

            // chat_id
            writer.append("--").append(boundary).append("\r\n")
                    .append("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n")
                    .append(config.getChatId()).append("\r\n");

            // caption
            if (caption != null && !caption.isEmpty()) {
                writer.append("--").append(boundary).append("\r\n")
                        .append("Content-Disposition: form-data; name=\"caption\"\r\n\r\n")
                        .append(caption).append("\r\n");
            }

            // сам файл
            writer.append("--").append(boundary).append("\r\n")
                    .append("Content-Disposition: form-data; name=\"").append(fieldName)
                    .append("\"; filename=\"").append(fileName).append("\"\r\n")
                    .append("Content-Type: ").append(mimeType).append("\r\n\r\n").flush();

            os.write(fileBytes);
            os.flush();

            writer.append("\r\n--").append(boundary).append("--\r\n").flush();
        }

        handleResponse(conn);
    }

    // -----------------------
    // Случайное сообщение
    // -----------------------
    public static void sendRandomMessage(List<String> messages, String extra) {
        String baseMessage = getRandomMessage(messages);
        if (isInvalidMessage(baseMessage)) return;
        send(baseMessage, extra);
    }

    private static String getRandomMessage(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return config.isDebugMode() ? "Нет доступных сообщений в конфигурации" : "";
        }
        return messages.get(random.nextInt(messages.size()));
    }

    private static boolean isInvalidMessage(String message) {
        return message == null || message.isEmpty()
                || message.equals("Нет доступных сообщений в конфигурации");
    }

    private static boolean validateConfig() {
        if (config.getBotToken() == null || config.getChatId() == null) {
            System.err.println("Ошибка: не указан токен или chatId");
            return true;
        }
        return false;
    }

    private static void handleResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        if (code != 200) {
            try (InputStream err = conn.getErrorStream()) {
                if (err != null) {
                    String errorMsg = new String(err.readAllBytes(), StandardCharsets.UTF_8);
                    System.err.println("Ошибка ответа Telegram API: " + errorMsg);
                }
            }
        }
    }

    // -----------------------
    // Завершение пула потоков
    // -----------------------
    public static void shutdown() {
        executor.shutdown();
    }
}
