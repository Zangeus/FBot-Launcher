package Utils;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class LogUtils {
    public static File getLastLines(File logFile, int numLines) throws IOException {
        List<String> allLines = Files.readAllLines(logFile.toPath());
        int start = Math.max(allLines.size() - numLines, 0);
        List<String> lastLines = allLines.subList(start, allLines.size());

        // создаём временный файл
        File tempLog = new File("log.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempLog))) {
            for (String line : lastLines) {
                writer.write(line);
                writer.newLine();
            }
        }
        return tempLog;
    }
}
