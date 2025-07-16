package Config;

import java.util.List;
import java.util.ArrayList;

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
    private String readmePath = "bot_sources/README.md";

    private boolean enableAutoRetry = false;
    private int searchDelayMs = 1000;
    private boolean debugMode = false;

    private boolean notificationsEnabled = true;
    private boolean reportWithScreenshot = false;
    private int sleepDurationMinutes = 12;
    //Виртуалка на 14к
    private boolean weekSUEnabled = true;
    private boolean monitoringEnabled = true;

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public void setMonitoringEnabled(boolean monitoringEnabled) {
        this.monitoringEnabled = monitoringEnabled;
    }

    public boolean isWeekSUEnabled() {
        return weekSUEnabled;
    }

    public void setWeekSUEnabled(boolean weekSU_IsEnabled) {
        this.weekSUEnabled = weekSU_IsEnabled;
    }

    public int getSleepDurationMinutes() {
        return sleepDurationMinutes;
    }

    public void setSleepDurationMinutes(int sleepDurationMinutes) {
        this.sleepDurationMinutes = sleepDurationMinutes;
    }


    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean isReportWithScreenshot() {
        return reportWithScreenshot;
    }

    public boolean shouldSendFailureReport() {
        return failureNotification && notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public void setReportWithScreenshot(boolean reportWithScreenshot) {
        this.reportWithScreenshot = reportWithScreenshot;
    }


    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public boolean isEnableAutoRetry() {
        return enableAutoRetry;
    }

    public void setEnableAutoRetry(boolean enableAutoRetry) {
        this.enableAutoRetry = enableAutoRetry;
    }

    public int getSearchDelayMs() {
        return searchDelayMs;
    }

    public void setSearchDelayMs(int searchDelayMs) {
        this.searchDelayMs = searchDelayMs;
    }

    public int getAttemptsAmount() {
        return attemptsAmount;
    }

    public void setAttemptsAmount(int attemptsAmount) {
        this.attemptsAmount = attemptsAmount;
    }

    public boolean isSuccessNotification() {
        return successNotification;
    }

    public void setSuccessNotification(boolean successNotification) {
        this.successNotification = successNotification;
    }

    public boolean isFailureNotification() {
        return failureNotification;
    }

    public void setFailureNotification(boolean failureNotification) {
        this.failureNotification = failureNotification;
    }

    public boolean isReportNotification() {
        return reportNotification;
    }

    public void setReportNotification(boolean reportNotification) {
        this.reportNotification = reportNotification;
    }

    public List<String> getSuccessMessages() {
        if (successMessages == null) successMessages = new ArrayList<>();
        return successMessages;
    }

    public void setSuccessMessages(List<String> successMessages) {
        this.successMessages = successMessages;
    }

    public List<String> getFailureMessages() {
        if (failureMessages == null) failureMessages = new ArrayList<>();
        return failureMessages;
    }

    public void setFailureMessages(List<String> failureMessages) {
        this.failureMessages = failureMessages;
    }

    public List<String> getReportMessages() {
        if (reportMessages == null) reportMessages = new ArrayList<>();
        return reportMessages;
    }

    public void setReportMessages(List<String> reportMessages) {
        this.reportMessages = reportMessages;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getPicsToStartPath() {
        return picsToStartPath;
    }

    public void setPicsToStartPath(String picsToStartPath) {
        this.picsToStartPath = picsToStartPath;
    }

    public String getReadmePath() {
        return readmePath;
    }

    public void setReadmePath(String readmePath) {
        this.readmePath = readmePath;
    }

    @Override
    public String toString() {
        return "LauncherConfig{" +
                "successNotification=" + successNotification +
                ", failureNotification=" + failureNotification +
                ", reportNotification=" + reportNotification +
                ", sleepDurationMinutes=" + sleepDurationMinutes +
                ", mondayCheckEnabled=" + weekSUEnabled +
                ", monitoringEnabled=" + monitoringEnabled +
                "}";
    }
}