package Processes;

import Config.ConfigManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class StartIsHere {
    public static void start() {
        try {
            String basePath = ConfigManager.loadConfig().getStarRailCopilotPath();
            String processToStart = basePath + "\\src.exe";
            ProcessBuilder pb = new ProcessBuilder(processToStart)
                    .directory(new File(basePath))
                    .redirectErrorStream(true);

            Process startedProcess = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(startedProcess.getInputStream()))) {
                String line;
                long timeout = System.currentTimeMillis() + 20000;

                while ((line = reader.readLine()) != null && System.currentTimeMillis() < timeout) {
                    if (line.contains("Application startup complete")) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка при запуске приложения: " + e.getMessage());
        }
    }
}
