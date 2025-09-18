package Utils;

import Waiters.TelegramBotSender;
import java.util.EnumMap;
import java.util.concurrent.CompletableFuture;

import static Waiters.TelegramBotSender.sendText;

public class CommandListener {

    public enum Command {RESTART, RESTART_FROM_START, STOP, NONE}

    private static final EnumMap<Command, Boolean> commands = new EnumMap<>(Command.class);

    // Флаг для прерывания основного цикла при получении команды
    private static volatile boolean commandReceived = false;

    static {
        for (Command cmd : Command.values()) {
            commands.put(cmd, false);
        }
    }

    public static synchronized void handleCommand(String input) {
        if (input == null || input.isBlank()) return;
        String cmd = input.toLowerCase();

        switch (cmd) {
            case "/restart":
                commands.put(Command.RESTART, true);
                sendText("- Nice - пробуем еще раз");

                // Немедленная обработка restart
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(300); // Время для отправки сообщения
                        triggerCommandProcessing(Command.RESTART);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                break;

            case "/restart_0":
                commands.put(Command.RESTART_FROM_START, true);
                sendText("- Nice - начинаем сначала");

                // Немедленная обработка restart_0
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(300);
                        triggerCommandProcessing(Command.RESTART_FROM_START);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                break;

            case "/stop":
                commands.put(Command.STOP, true);
                sendText("- Got it - остановка запущена");

                // Немедленная обработка stop
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(300);
                        triggerCommandProcessing(Command.STOP);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                break;

            case "/status":
                // Status обрабатывается мгновенно
                CompletableFuture.runAsync(() -> {
                    try {
                        Utils.Notifier.sendReport();
                    } catch (Exception e) {
                        sendText("Ошибка при получении статуса: " + e.getMessage());
                    }
                });
                break;

            default:
                TelegramBotSender.sendText("Неизвестная команда: " + input);
                break;
        }

        // Сигнализируем основному циклу, что пришла команда
        commandReceived = true;
    }

    private static void triggerCommandProcessing(Command command) {
        System.out.println("🚀 Немедленная обработка команды: " + command);

        try {
            switch (command) {
                case RESTART:
                    System.out.println("♻ Выполняем restart...");
                    // Можно вызвать restart напрямую или через Main
                    notifyMainLoop();
                    break;

                case RESTART_FROM_START:
                    System.out.println("♻ Выполняем restart from start...");
                    notifyMainLoop();
                    break;

                case STOP:
                    System.out.println("🛑 Выполняем stop...");
                    Waiters.Main.stopProgram();
                    break;
            }
        } catch (Exception e) {
            System.err.println("Ошибка при выполнении команды " + command + ": " + e.getMessage());
        }
    }

    private static void notifyMainLoop() {
        // Прерываем сон основного цикла
        commandReceived = true;
    }

    public static synchronized boolean isCommandRequested(Command command) {
        boolean result = commands.getOrDefault(command, false);
        commands.put(command, false); // сброс после проверки
        return result;
    }

    // Метод для проверки, была ли получена команда (для прерывания сна)
    public static boolean wasCommandReceived() {
        boolean result = commandReceived;
        commandReceived = false;
        return result;
    }

    // Сброс флага команды
    public static void resetCommandFlag() {
        commandReceived = false;
    }
}