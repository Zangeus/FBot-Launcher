package Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private static final String CONFIG_FILE = "bot_sources/config.json";
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static LauncherConfig config;

    public static LauncherConfig loadConfig() {
        if (config != null) return config;

        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            config = new LauncherConfig();
            saveConfig(config);
            return config;
        }

        try {
            config = mapper.readValue(file, LauncherConfig.class);

            // fallback если поле starRailCopilotPath отсутствует в старом json
            if (config.getStarRailCopilotPath() == null) {
                config.setStarRailCopilotPath("");
            }

            return config;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при загрузке конфига", e);
        }
    }

    public static void saveConfig(LauncherConfig config) {
        try {
            mapper.writeValue(new File(CONFIG_FILE), config);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении конфигурации", e);
        }
    }
}
