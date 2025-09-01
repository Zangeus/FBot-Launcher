package End;

import static Utils.WindowUtils.closeWindowByTitle;

public class CloseProcess {
    private static final String[] windowTitles = new String[]{
            "Android Device",
            "MuMuPlayer",
            "src"
    };

    public static void closeAll() {
        for (String title : windowTitles) {
            closeWindowByTitle(title);
        }
    }
}
