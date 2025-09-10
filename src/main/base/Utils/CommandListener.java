package Utils;

import Waiters.TelegramBotSender;

import java.util.EnumMap;

public class CommandListener {

    public enum Command {RESTART, RESTART_FROM_START, STOP, NONE}

    private static final EnumMap<Command, Boolean> commands = new EnumMap<>(Command.class);

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
                TelegramBotSender.sendText("♻ Перезапуск запрошен");
                break;

            case "/restart_0":
                commands.put(Command.RESTART_FROM_START, true);
                TelegramBotSender.sendText("♻ Перезапуск сначала запрошен");
                break;

            case "/stop":
                commands.put(Command.STOP, true);
                TelegramBotSender.sendText("⏹ Остановка запрошена");
                break;

            case "/status":
                Utils.Notifier.sendReport();
                break;

            default:
                TelegramBotSender.sendText("❌ Неизвестная команда: " + input);
                break;
        }
    }


    public static synchronized boolean isCommandRequested(Command command) {
        boolean result = commands.getOrDefault(command, false);
        commands.put(command, false); // сброс после проверки
        return result;
    }
}
