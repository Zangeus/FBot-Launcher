package End;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

public class CloseProcess {
    private static final String[] windowTitles = new String[]{
            "Android Device",
            "MuMuPlayer",
            "src"
    };

    public static void closeWindow(String windowTitle) {
        User32 user32 = User32.INSTANCE;

        HWND hwnd = user32.FindWindow(null, windowTitle);
        if (hwnd != null) {
            user32.PostMessage(hwnd, 0x10, null, null);
            System.out.println("Окно \"" + windowTitle + "\" закрывается мягко...");
        } else {
            System.out.println("Окно с заголовком \"" + windowTitle + "\" не найдено!");
        }
    }

    public static void closeAll() {
        for (String title : windowTitles) {
            closeWindow(title);
        }
    }
}
