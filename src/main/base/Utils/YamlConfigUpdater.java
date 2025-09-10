package Utils;

import Config.ConfigManager;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class YamlConfigUpdater {
    private static final String CONFIG_PATH = ConfigManager.loadConfig().getStarRailCopilotPath() + "\\config";
    private static final String DEPLOY_FILE = "deploy.yaml";

    public void setRunConfig(String configName) throws IOException {
        String value = configName == null ? "null" : "[\"" + configName + "\"]";
        updateRunLine(value);
    }

    private void updateRunLine(String newValue) throws IOException {
        Path deployFile = Paths.get(CONFIG_PATH, DEPLOY_FILE);

        if (!Files.exists(deployFile)) {
            throw new FileNotFoundException("Файл deploy.yaml не найден по пути: " + deployFile);
        }

        List<String> lines = Files.readAllLines(deployFile);
        List<String> updatedLines = new ArrayList<>();
        boolean updated = false;

        for (String line : lines) {
            if (line.trim().startsWith("Run:")) {
                String indent = line.substring(0, line.indexOf("Run:"));
                String newLine = indent + "Run: " + newValue;
                updatedLines.add(newLine);
                updated = true;
                System.out.println("Заменено: '" + line.trim() + "' -> '" + newLine.trim() + "'");
            } else {
                updatedLines.add(line);
            }
        }

        if (!updated) {
            throw new RuntimeException("Строка 'Run:' не найдена в файле");
        }

        Files.write(deployFile, updatedLines);
    }

}
