package Utils;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class ConfigJson {
    private static final Path CONFIG_PATH = Paths.get("Q:/Z-folder/Bot_time/StarRailCopilot/config/src.json");

    private ConfigJson() {}

    public static synchronized JSONObject read() throws IOException {
        try (Reader r = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            return new JSONObject(new JSONTokener(r));
        }
    }

    public static synchronized void write(JSONObject root) throws IOException {
        // атомарная запись: в temp → move
        Path tmp = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            w.write(root.toString(4));
        }
        Files.move(tmp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static JSONObject getOrCreate(JSONObject parent, String key) {
        if (parent.has(key) && parent.get(key) instanceof JSONObject) {
            return parent.getJSONObject(key);
        }
        JSONObject obj = new JSONObject();
        parent.put(key, obj);
        return obj;
    }

    public static synchronized void setWeeklyFarming(boolean value) {
        try {
            JSONObject root = read();
            JSONObject rogue = getOrCreate(root, "Rogue");
            JSONObject world = getOrCreate(rogue, "RogueWorld");
            world.put("WeeklyFarming", value);
            write(root);
            System.out.println("WeeklyFarming = " + value + " записан в src.json");
        } catch (Exception e) {
            System.err.println("Не удалось установить WeeklyFarming=" + value + ": " + e.getMessage());
        }
    }

    public static synchronized boolean isSUCompleted() {
        try {
            JSONObject root = read();
            JSONObject farm = root.getJSONObject("Rogue")
                    .getJSONObject("RogueWorld")
                    .getJSONObject("SimulatedUniverseFarm");
            int total = farm.optInt("total", 100);
            int value = farm.optInt("value", 0);
            return total > 0 && value >= total;
        } catch (Exception e) {
            System.err.println("Ошибка чтения SU прогресса: " + e.getMessage());
            return false;
        }
    }
}
