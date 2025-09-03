package Processes.Errors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ErrorRules {
    private static final Map<String, ErrorSeverity> ERROR_RULES = new HashMap<>();

    static {
        ERROR_RULES.put("EmulatorNotRunningError", ErrorSeverity.RECOVERABLE);
        ERROR_RULES.put("Request human takeover", ErrorSeverity.FATAL);
        ERROR_RULES.put("Task `Rogue` failed 3 or more times.", ErrorSeverity.ROGUE_FAILED_3_TIMES);
    }

    public static Map<String, ErrorSeverity> getRules() {
        return Collections.unmodifiableMap(ERROR_RULES);
    }

    public static ErrorSeverity classify(String line) {
        return ERROR_RULES.entrySet().stream()
                .filter(entry -> line.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public static void addRule(String keyword, ErrorSeverity severity) {
        ERROR_RULES.put(keyword, severity);
    }
}
