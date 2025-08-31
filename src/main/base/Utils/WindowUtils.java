package Utils;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;

public class WindowUtils {

    public static String getWindowTitleByPID(long pid) {
        final StringBuilder windowTitle = new StringBuilder();

        User32.INSTANCE.EnumWindows((HWND hWnd, Pointer data) -> {
            IntByReference lpdwProcessId = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hWnd, lpdwProcessId);

            if (lpdwProcessId.getValue() == (int) pid) {
                char[] buffer = new char[1024];
                User32.INSTANCE.GetWindowText(hWnd, buffer, 1024);
                String title = Native.toString(buffer);
                if (!title.isEmpty()) {
                    windowTitle.append(title);
                    return false; // нашли → стоп
                }
            }
            return true;
        }, null);

        return windowTitle.toString();
    }

    public static boolean closeWindowByTitle(String windowTitle) {
        HWND hWnd = User32.INSTANCE.FindWindow(null, windowTitle);
        if (hWnd == null) {
            System.err.println("Окно не найдено: " + windowTitle);
            return false;
        }

        int WM_CLOSE = 0x0010;
        User32.INSTANCE.PostMessage(hWnd, WM_CLOSE, null, null);
        return true;
    }
}
