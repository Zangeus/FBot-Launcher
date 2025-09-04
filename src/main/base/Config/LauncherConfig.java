package Config;

import lombok.Data;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

@Data
public class LauncherConfig {
    private int attemptsAmount = 40;

    private boolean successNotification = true;
    private boolean failureNotification = true;
    private boolean reportNotification = true;

    private List<String> successMessages = new ArrayList<>();
    private List<String> failureMessages = new ArrayList<>();
    private List<String> reportMessages = new ArrayList<>();

    private String botToken = "";
    private String chatId = "";
    private String picsToStartPath = "";
    private String starRailCopilotPath = "";

    private String readmePath = "bot_sources/README.md";

    private boolean enableAutoRetry = false;

    private int searchDelayMs = 1000;

    private boolean debugMode = false;
    private boolean notificationsEnabled = true;
    private boolean reportWithScreenshot = false;

    private int sleepDurationMinutes = 12;

    private boolean darkThemeEnabled = true;
    private boolean SU_Monitoring = true;

    public List<String> getSuccessMessages() {
        if (!isSuccessNotification() || successMessages == null) successMessages = new ArrayList<>();
        return successMessages;
    }

    public List<String> getFailureMessages() {
        if (!isFailureNotification() || failureMessages == null) failureMessages = new ArrayList<>();
        return failureMessages;
    }

    public List<String> getReportMessages() {
        if (!isReportNotification() || reportMessages == null) reportMessages = new ArrayList<>();
        return reportMessages;
    }

    public static String getRandomMessage(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return ConfigManager.loadConfig().isDebugMode() ? "Нет доступных сообщений в конфигурации" : "";
        }
        return messages.get(new Random().nextInt(messages.size()));
    }

    @Override
    public String toString() {
        return "LauncherConfig{" +
                "successNotification=" + successNotification +
                ", failureNotification=" + failureNotification +
                ", reportNotification=" + reportNotification +
                ", sleepDurationMinutes=" + sleepDurationMinutes +
                ", darkThemeEnabled=" + darkThemeEnabled +
                ", SU_Monitoring=" + SU_Monitoring +
                ", starRailCopilotPath='" + starRailCopilotPath + '\'' +
                "}";
    }
}