package Waiters;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.BotSession;
import Utils.CommandListener;

@Data
@EqualsAndHashCode(callSuper = true)
public class TelegramBotHandler extends TelegramLongPollingBot {

    private BotSession botSession;
    private final String botUsername;
    private boolean running = true;

    public TelegramBotHandler(String botToken, String botUsername) {
        super(botToken);
        this.botUsername = botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!running) return;

        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            CommandListener.handleCommand(text);
        }
    }

    public void stopBot() {
        System.out.println("🛑 Начинаем остановку Telegram бота...");
        running = false;

        try {
            this.clearWebhook();
            System.out.println("✅ Webhook очищен");
        } catch (Exception e) {
            System.err.println("⚠ Ошибка при очистке webhook: " + e.getMessage());
        }

        try {
            if (botSession != null && botSession.isRunning()) {
                botSession.stop();
                System.out.println("✅ BotSession остановлена");
            }
        } catch (Exception e) {
            System.err.println("⚠ Ошибка при остановке BotSession: " + e.getMessage());
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void forceStop() {
        System.out.println("🔥 Принудительная остановка Telegram бота...");
        running = false;

        if (botSession != null) {
            try {
                if (!botSession.isRunning()) {
                    System.out.println("✅ BotSession уже остановлена");
                    return;
                }

                botSession.stop();
                Thread.sleep(1000); // Даем время на остановку
                System.out.println("✅ Команда остановки BotSession отправлена");

            } catch (Exception e) {
                System.out.println("⚠ Ошибка при остановке (игнорируется): " + e.getMessage());
            }
        }
    }

    public void onClosing() {
        System.out.println("🧹 Очистка ресурсов TelegramBotHandler...");
        running = false;

        try {
            System.out.println("✅ Ресурсы TelegramBotHandler очищены");
        } catch (Exception e) {
            System.err.println("⚠ Ошибка при очистке ресурсов: " + e.getMessage());
        }
    }
}