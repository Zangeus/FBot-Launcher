package Waiters;

import Config.ConfigManager;
import Config.LauncherConfig;
import Processes.CloseProcess;
import Processes.EndWatcher;
import Processes.Errors.ErrorMonitoring;
import Processes.Errors.ErrorSeverity;
import Processes.StartIsHere;
import Utils.CommandListener;
import Utils.YamlConfigUpdater;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static Utils.YamlConfigUpdater.setRunConfig;
import static Waiters.TelegramBotSender.sendRandomMessage;

public class Main {
    private static final String LOCK_FILE = "bot_sources/app.lock";
    private static final LauncherConfig config = ConfigManager.loadConfig();
    private static final List<String> REPORT_MESSAGES = config.getReportMessages();
    private static TelegramBotHandler botHandler;

    private static volatile boolean isRunning = true;
    private static volatile boolean done = false;
    private static boolean fatalExit = false;
    private static final int timeoutSeconds = 30;
    private static int maxAttempts = 3;

    public static void start() throws TelegramApiException {
        if (!acquireLock()) {
            System.err.println("Программа уже запущена! Выход...");
            return;
        }

        registerBot();
        hook();

        boolean isSURun = config.isSU_Monitoring();
        if (isSURun) {
            maxAttempts = 999;
            Utils.ConfigJson.setWeeklyFarming(true);
        } else {
            Utils.ConfigJson.setWeeklyFarming(false);
        }

        try {
            ErrorMonitoring.initFromConfig(config);
            EndWatcher.initFromConfig(config);

            ErrorMonitoring.startAsync();
            EndWatcher.startAsync();

            int attempt = 1;

            outerLoop:
            while (!fatalExit && !done && attempt <= maxAttempts) {
                CloseProcess.closeAll();

                System.out.println("\n=== Попытка #" + attempt + " ===");
                StartIsHere.start();

                boolean startError = waitForStartErrorWithStopCheck();
                if (startError) {
                    restart();
                    attempt++;
                    continue;
                }

                boolean resetAttempt = false;
                isRunning = true;
                while (isRunning && !done) {
                    resetAttempt = handleCommands();

                    if (done) break outerLoop;

                    if (EndWatcher.isStoppedSuccessfully()) {
                        stopProgram();
                        break outerLoop;
                    }

                    ErrorSeverity severity = ErrorMonitoring.pollError();
                    if (severity != null) {
                        handleError(severity);
                        if (done) break outerLoop;
                    }

                    sleepOneSecond();
                }

                if (!resetAttempt) {
                    attempt++;
                } else {
                    attempt = 1;
                }
            }

        } finally {
            System.out.println("🧹 Начинаем финальную очистку...");

            ErrorMonitoring.stop();
            EndWatcher.stop();
            TelegramBotSender.shutdown();

            if (botHandler != null && botHandler.isRunning()) {
                System.out.println("🛑 Останавливаем Telegram бота...");
                botHandler.stopBot();
                botHandler.forceStop();
                botHandler.onClosing();
            }

            forceShutdownTelegramThreads();

            if (!fatalExit) {
                if (done && isSURun) {
                    completeSU();
                } else if (!done) {
                    sendRandomMessage(REPORT_MESSAGES);
                }
            }

            setRunConfig("null");
            performEmergencyShutdown();
            performFinalCleanup();

            showThreads();
        }
    }

    private static boolean handleCommands() {
        if (CommandListener.isCommandRequested(CommandListener.Command.RESTART)) {
            System.out.println("♻ Команда /restart получена");
            restart();
            isRunning = false;
            return false;
        }

        if (CommandListener.isCommandRequested(CommandListener.Command.RESTART_FROM_START)) {
            System.out.println("♻ Команда /restart 0 получена");
            restart();
            isRunning = false;
            return true;
        }

        if (CommandListener.isCommandRequested(CommandListener.Command.STOP)) {
            System.out.println("⏹ Команда /stop получена");
            stopProgram();
            return false;
        }

        return false;
    }

    public static void stopProgram() {
        done = true;
        isRunning = false;
    }

    //Ошибки
    private static void handleError(ErrorSeverity severity) {
        switch (severity) {
            case RECOVERABLE:
                restart();
                break;
            case FATAL:
                System.out.println("❌ Фатальная ошибка → аварийное завершение");
                fatalExit = true;
                isRunning = false;
                done = true;
                break;
        }
    }

    private static boolean waitForStartErrorWithStopCheck() {
        for (int i = 0; i < timeoutSeconds; i++) {
            if (done) return false;
            if (ErrorMonitoring.pollError() != null) return true;
            sleepOneSecond();
        }
        return false;
    }

    public static void completeSU() {
        Utils.ConfigJson.setWeeklyFarming(false);

        TelegramBotSender.send(
                new File(config.getPicsToStartPath() + "/SU.png"),
                "Исследование Виртуальной вселенной завершено"
        );

        config.setSU_Monitoring(false);
        ConfigManager.saveConfig(config);
        performEmergencyShutdown();
    }

    //Все что связано с запуском и шатдауном
    private static void hook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 SHUTDOWN HOOK: Получен сигнал завершения программы");
            long startTime = System.currentTimeMillis();

            try {
                System.out.println("1️⃣ Останавливаем основной цикл...");
                stopProgram();

                System.out.println("2️⃣ Останавливаем мониторинг...");
                try {
                    ErrorMonitoring.stop();
                    EndWatcher.stop();
                } catch (Exception e) {
                    System.err.println("⚠ Ошибка остановки мониторинга: " + e.getMessage());
                }

                if (botHandler != null) {
                    System.out.println("3️⃣ Останавливаем Telegram бота...");
                    botHandler.stopBot();

                    if (botHandler.isRunning()) {
                        System.out.println("⚠ Graceful shutdown не сработал, применяем принудительную остановку");
                        botHandler.forceStop();
                    }

                    botHandler.onClosing();
                }

                System.out.println("4️⃣ Останавливаем TelegramBotSender...");
                try {
                    TelegramBotSender.shutdown();
                } catch (Exception e) {
                    System.err.println("⚠ Ошибка остановки TelegramBotSender: " + e.getMessage());
                }

                System.out.println("5️⃣ Закрываем все процессы...");
                try {
                    CloseProcess.closeAll();
                } catch (Exception e) {
                    System.err.println("⚠ Ошибка закрытия процессов: " + e.getMessage());
                }

                System.out.println("6️⃣ Завершаем Telegram потоки...");
                forceShutdownTelegramThreads();

                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("✅ SHUTDOWN HOOK завершен за " + elapsed + "мс");

            } catch (Exception e) {
                System.err.println("❌ Ошибка в SHUTDOWN HOOK: " + e.getMessage());
            }

            long totalTime = System.currentTimeMillis() - startTime;
            if (totalTime > 10000) {
                System.err.println("⚠ Shutdown занял слишком много времени (" + totalTime + "мс)");
                System.err.println("🔥 Принудительное завершение через System.exit()");
                System.exit(1);
            }
        }, "ShutdownHook-TelegramBot"));
    }

    private static void restart() {
        CloseProcess.closeAll();
        sendRandomMessage(REPORT_MESSAGES);
    }

    private static void forceShutdownTelegramThreads() {
        System.out.println("🔥 Принудительное завершение Telegram threads...");

        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        int processed = 0;

        for (Thread thread : threads.keySet()) {
            if (processed > 10) break; // Ограничиваем обработку

            String threadName = thread.getName().toLowerCase();

            if (threadName.contains("telegram") ||
                    threadName.contains("bot") ||
                    threadName.contains("okhttp") ||
                    (threadName.contains("pool-") && !thread.isDaemon())) {

                if (thread != Thread.currentThread() && thread.isAlive()) {
                    System.out.println("🔪 Прерываем поток: " + thread.getName());
                    try {
                        thread.interrupt();

                        thread.join(500);

                        if (thread.isAlive()) {
                            System.out.println("⚠ Поток " + thread.getName() + " не завершился (игнорируем)");
                        } else {
                            System.out.println("✅ Поток " + thread.getName() + " завершен");
                        }
                        processed++;
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        System.out.println("🔪 Обработано потоков: " + processed);
    }

    private static void performEmergencyShutdown() {
        try {
            Runtime.getRuntime().exec("shutdown -s -f -t 100");
            System.out.println("⚠ Выключение системы было запущено");
        } catch (IOException e) {
            System.err.println("Выключение было прервано: " + e.getMessage());
        }
    }

    private static void performFinalCleanup() {
        try {
            File lockFile = new File(LOCK_FILE);
            if (lockFile.exists() && !lockFile.delete()) {
                System.err.println("⚠ Не удалось удалить lock-файл: " + lockFile.getAbsolutePath());
            }
            CloseProcess.closeAll();
            System.out.println("✅ Очистка завершена");
        } catch (Exception e) {
            System.err.println("Ошибка при последней зачистке: " + e.getMessage());
        }
    }

    private static boolean acquireLock() {
        try {
            File lockFile = new File(LOCK_FILE);
            if (lockFile.exists()) return false;
            if (!lockFile.createNewFile()) return false;

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (lockFile.exists() && !lockFile.delete()) {
                    System.err.println("⚠ Не удалось удалить lock-файл");
                }
            }));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    //Процессы
    public static boolean isRunning() {
        return isRunning;
    }

    private static void sleepOneSecond() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void showThreads() {
        System.out.println("---------------------------------------------");
        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        for (Thread t : threads.keySet()) {
            if (t != Thread.currentThread()) {
                System.out.println("Alive thread: " + t.getName() +
                        ", daemon=" + t.isDaemon() +
                        ", state=" + t.getState() +
                        ", alive=" + t.isAlive());

                // Показываем stack trace для Telegram потоков
                if (t.getName().toLowerCase().contains("telegram") ||
                        t.getName().toLowerCase().contains("bot")) {
                    StackTraceElement[] stackTrace = threads.get(t);
                    if (stackTrace.length > 0) {
                        System.out.println("  -> " + stackTrace[0]);
                    }
                }
            }
        }
        System.out.println("---------------------------------------------");
    }

    private static void registerBot() throws TelegramApiException {
        System.out.println("🤖 Регистрация Telegram бота...");

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botHandler = new TelegramBotHandler(
                ConfigManager.loadConfig().getBotToken(),
                ConfigManager.loadConfig().getChatId()
        );

        org.telegram.telegrambots.meta.generics.BotSession botSession = botsApi.registerBot(botHandler);
        botHandler.setBotSession(botSession);

        System.out.println("✅ Telegram бот зарегистрирован");
    }
}
