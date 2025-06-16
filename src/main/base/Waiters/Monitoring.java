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
    private static boolean picToSend = false;
    private static final LauncherConfig config = ConfigManager.loadConfig();

    public static void monitorStart() {

        while (true) {
            activateWindow(src);
            if (findAndClickScreenless("critical_2.png")) {
                restart();
                sleep(config.getSleepDurationMinutes(), MINUTES);
                continue;
            }

            if (findAndClickScreenless("tasks_done.png")) break;
            sleep(5, MINUTES);
        }

        while (true) {
            activateWindow(src);
            if (findAndClickScreenless("critical.png")) {
                refresh();
                sleep(20, MINUTES);
                continue;
            }

            if (findAndClickScreenless("critical_2.png")) {
                restart();
                sleep(20, MINUTES);
                continue;
            }

            if (check("su_button.png") && check("elites_farm.png")) {
                check(("overview.png"));
                if (picToSend) sendPhoto();

                config.setMonitoringEnabled(false);
                ConfigManager.saveConfig(config);
                break;
            }

            picToSend = true;
            check(("overview.png"));
            sleep(20, MINUTES);
        }
    }

    public static void monitor() {
        if (!isProcessRunning()) {
            StartIsHere.start();
            sleep(config.getSleepDurationMinutes(), MINUTES);
        }
        monitorStart();
        executeEmergencyProtocol();
    }

    private static boolean check(String image) {
        return findAndClickScreenless(image);
    }

    private static void executeEmergencyProtocol() {
        TakeTheMail.take();
        CloseProcess.terminateProcesses();
        performEmergencyShutdown();
    }

    public static void sendPhoto() {
        String imagePath = "bot_sources/SU.png";
        TelegramBotSender.sendLocalPhoto(imagePath);

        TelegramBotSender.sendNoteMessage("Легендарный квест 1001-ночи был завершен");
    }


    private static void refresh() {
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
        for (int i = 0; i <= 3; i++) {
            if (StartIsHere.start()) break;
            else if (i == 3) TelegramBotSender
                    .sendNoteMessage("Не удалось запустить бота");
        }
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
