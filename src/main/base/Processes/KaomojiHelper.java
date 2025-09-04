package Processes;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KaomojiHelper {
    private static final List<String> KAOMOJIS = List.of(
            "(•̀ᴗ•́)و ̑̑",
            "(≧◡≦)",
            "(¬‿¬)",
            "(✿◕‿◕)",
            "(づ｡◕‿‿◕｡)づ",
            "(＾▽＾)"
    );

    public static String randomKaomoji() {
        int idx = ThreadLocalRandom.current().nextInt(KAOMOJIS.size());
        return KAOMOJIS.get(idx);
    }
}

