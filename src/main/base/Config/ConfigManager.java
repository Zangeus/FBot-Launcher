package Config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private static final String CONFIG_FILE = "bot_sources/app_config.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static LauncherConfig loadConfig() {
        File configFile = new File(CONFIG_FILE);
        try {
            if (configFile.exists()) {
                LauncherConfig loaded = mapper.readValue(configFile, LauncherConfig.class);
                System.out.println("Loaded config: " + loaded);
                return loaded;
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки конфига: " + e.getMessage());
        }
        return new LauncherConfig();
    }

    public static void saveConfig(LauncherConfig config) {
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(CONFIG_FILE), config);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения конфига: " + e.getMessage());
        }
    }
}