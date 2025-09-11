package Processes;

import Config.ConfigManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import static Utils.YamlConfigUpdater.setRunConfig;

public class StartIsHere {
    public static void start() {
        try {
            String MuMuPlayerPath = ConfigManager.loadConfig()
                    .getMuMuPlayerPath() + "/nx_main/MuMuNxMain.exe";
            new ProcessBuilder(MuMuPlayerPath, "-v", "vm_0").start();

            setRunConfig("SRC");

            String basePath = ConfigManager.loadConfig().getStarRailCopilotPath();
            String processToStart = basePath + "\\src.exe";
            ProcessBuilder pb = new ProcessBuilder(processToStart)
                    .directory(new File(basePath))
                    .redirectErrorStream(true);

            Process src = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(src.getInputStream()))) {
                String line;
                long timeout = System.currentTimeMillis() + 20000;

                while ((line = reader.readLine()) != null && System.currentTimeMillis() < timeout) {
                    if (line.contains("Application startup complete")) {
                        setRunConfig("null");
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка при запуске приложения: " + e.getMessage());
        }
    }
}
