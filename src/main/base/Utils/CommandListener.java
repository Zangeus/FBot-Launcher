package Utils;

import Waiters.TelegramBotSender;

import java.util.EnumMap;

import static Waiters.TelegramBotSender.sendText;

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
                sendText("- Nice - пробуем еще раз");
                break;

            case "/restart_0":
                commands.put(Command.RESTART_FROM_START, true);
                sendText("- Nice - начинаем сначала");
                break;

            case "/stop":
                commands.put(Command.STOP, true);
                sendText("- Got it - остановка запущена");
                break;

            case "/status":
                Utils.Notifier.sendReport();
                break;

            default:
                TelegramBotSender.sendText("Неизвестная команда: " + input);
                break;
        }
    }


    public static synchronized boolean isCommandRequested(Command command) {
        boolean result = commands.getOrDefault(command, false);
        commands.put(command, false); // сброс после проверки
        return result;
    }
}
