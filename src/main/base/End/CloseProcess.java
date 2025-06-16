package End;

import java.io.IOException;

public class CloseProcess {
    private static final String[] processes = new String[]{"MuMuVMMHeadless.exe", "MuMuPlayer.exe", "src" };

    public static void terminate(String processName) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("tasklist");
            Process process = processBuilder.start();

            java.util.Scanner scanner = new java.util.Scanner(process.getInputStream());
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(processName)) {
                    String[] parts = line.split("\\s+");
                    String pid = parts[1];

                    new ProcessBuilder("taskkill", "/PID", pid, "/F").start();
                    System.out.println("Процесс " + processName + " с PID " + pid + " завершен.");
                    return;
                }
            }
            scanner.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void terminateProcesses() {
        for (String processName : processes) {
            terminate(processName);
        }
    }
}
