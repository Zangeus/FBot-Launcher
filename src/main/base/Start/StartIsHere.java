package Start;

import java.io.File;

import static Utils.FindButtonAndPress.*;
import static java.lang.Thread.sleep;

public class StartIsHere {

    public static boolean start() {
        try {
            String processToStart =
                    "Q:\\Z-folder\\Bot_time\\StarRailCopilot\\src.exe";

            new ProcessBuilder(processToStart)
                    .directory(new File("Q:\\Z-folder\\Bot_time\\StarRailCopilot"))
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();

            sleep(6000);
            return findAndClickWithOneMessage("start_button.png", "Не удалось найти кнопку запуска");

        } catch (Exception e) {
            System.err.println("Ошибка при запуске приложения: " + e.getMessage());
            return false;
        }
    }
}