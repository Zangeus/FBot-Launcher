package Waiters;

import Config.ConfigManager;
import Config.LauncherConfig;
import End.CloseProcess;
import Start.StartIsHere;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import static Utils.ClickByCoords.activateAndClick;
import static Utils.ClickByCoords.activateWindow;
import static Utils.ClickByCoords.performClick;
import static Utils.FindButtonAndPress.*;

public class Monitoring {
    private static final String MuMu = "MuMu Player 12";
    private static final String src = "src";
    private static final Point[] CLICK_POINTS = {
            new Point(970, 444),
            new Point(940, 666),
            new Point(915, 520),
    };

    private static final String MINUTES = "MINUTES";
    private static final String SECONDS = "SECONDS";
    private static final String START_FAILED = "critical_2.png";
    private static final String SU_FAIL = "critical.png";
    private static final String EMPTY_TASK_LIST = "zero.png";
    private static final String SU_BUTTON = "su_button.png";
    private static final String FARM_COMPLETED = "elites_farm.png";

    private static final LauncherConfig config = ConfigManager.loadConfig();

    public static void monitorStart() {

        while (true) {
            activateWindow(src);
            if (find(SU_FAIL)) {
                reenterIntoSU();
                sleep(20, MINUTES);
                continue;
            }

            if (find(START_FAILED)) {
                restart();
                sleep(20, MINUTES);
                continue;
            }

            if (find(EMPTY_TASK_LIST) &&
                    find(SU_BUTTON) &&
                    find(FARM_COMPLETED)) {

                sendPhoto();

                config.setMonitoringEnabled(false);
                ConfigManager.saveConfig(config);
                break;
            }

            sleep(20, MINUTES);
        }
    }

    private static boolean find(String picToFind) {
        return findAndClickScreenless(picToFind);
    }

    public static void monitor() {
        if (!isProcessRunning()) startBot();
        monitorStart();
        executeEmergencyProtocol();
    }

    private static void startBot() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            if (!StartIsHere.start()) continue;

            sleep(30, SECONDS);

            if (find(START_FAILED)) {
                CloseProcess.terminateProcesses();
            } else break;
        }
        sleep(config.getSleepDurationMinutes(), MINUTES);
    }

    private static void executeEmergencyProtocol() {
        CloseProcess.terminateProcesses();
        performEmergencyShutdown();
    }

    public static void sendPhoto() {
        String imagePath = "bot_sources/SU.png";
        TelegramBotSender.sendLocalPhoto(imagePath);

        TelegramBotSender.sendNoteMessage("Исследование Виртуальной вселенной завершено");
    }


    private static void reenterIntoSU() {
        activateAndClick(MuMu, CLICK_POINTS, 3000);
        sleep(3, "SECONDS");
        performClick(780, 675, 0);

        activateWindow(src);
        findAndClickWithOneMessage("start_button.png", "Не удалось найти кнопку запуска");
    }

    private static void sleep(int amount, String type) {
        try {
            switch (type) {
                case MINUTES:
                    TimeUnit.SECONDS.sleep(60L * amount);
                    break;
                case SECONDS:
                    TimeUnit.SECONDS.sleep(amount);
                    break;
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void performEmergencyShutdown() {
        try {
            Runtime.getRuntime().exec("shutdown -s -f -t 100");
            System.out.println("Выключение было запущенно");
        } catch (IOException e) {
            System.err.println("Выключение было прервано: " + e.getMessage());
        }
    }

    private static void restart() {
        CloseProcess.terminateProcesses();
        startBot();
    }

    private static boolean isProcessRunning() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String command = os.contains("win") ? "tasklist" : "ps -A";

            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(Monitoring.src)) {
                    return true;
                }
            }
            reader.close();
            return false;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}
