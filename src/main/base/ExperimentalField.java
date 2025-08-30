import java.io.File;
import java.io.IOException;

public class ExperimentalField {
    public static void main(String[] args) {
        try {
            String processToStart =
                    "Q:\\Z-folder\\Bot_time\\StarRailCopilot\\src.exe";

            new ProcessBuilder(processToStart)
                    .directory(new File("Q:\\Z-folder\\Bot_time\\StarRailCopilot"))
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();

        } catch (IOException e) {
            System.err.println("Ошибка при запуске приложения: " + e.getMessage());
        }
    }
}
