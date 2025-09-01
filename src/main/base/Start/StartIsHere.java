package Start;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import static Utils.FindButtonAndPress.*;

public class StartIsHere {

    public static boolean start(int attempt) {
        try {
            String processToStart =
                    "Q:\\Z-folder\\Bot_time\\StarRailCopilot\\src.exe";

            ProcessBuilder pb = new ProcessBuilder(processToStart)
                    .directory(new File("Q:\\Z-folder\\Bot_time\\StarRailCopilot"))
                    .redirectErrorStream(true);

            Process startedProcess = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(startedProcess.getInputStream()))) {
                String line;
                long timeout = System.currentTimeMillis() + 20000;
                boolean ready = false;

                while ((line = reader.readLine()) != null && System.currentTimeMillis() < timeout) {
                    if (line.contains("Application startup complete")) {
                        ready = true;
                        break;
                    }
                }

                if (!ready) {
                    System.err.println("Приложение не успело загрузиться за отведённое время");
                    return false;
                }
            }

            return attempt != 3 ? findAndClick("start.png") :
                    findAndClickWithMessage("start.png", "Не удалось найти кнопку Start!");


        } catch (Exception e) {
            System.err.println("Ошибка при запуске приложения: " + e.getMessage());
            return false;
        }
    }
}
