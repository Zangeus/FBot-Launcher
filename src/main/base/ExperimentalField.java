import java.io.IOException;

public class ExperimentalField {
    public static void main(String[] args) {
        try {
            new ProcessBuilder("C:\\Program Files (x86)\\MuMuPlayerGlobal\\nx_main\\MuMuNxMain.exe")
                    .start();

        } catch (IOException e) {
            System.err.println("Ошибка при запуске приложения: " + e.getMessage());
        }
    }
}
