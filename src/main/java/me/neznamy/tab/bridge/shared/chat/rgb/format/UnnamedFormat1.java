package me.neznamy.tab.bridge.shared.chat.rgb.format;

import me.neznamy.tab.bridge.shared.chat.EnumChatFormat;
import org.jetbrains.annotations.NotNull;

/**
 * Formatter for &amp;#RRGGBB
 */
public class UnnamedFormat1 implements RGBFormatter {

    private final String formatNew = EnumChatFormat.COLOR_STRING + "#"; // 4.1.3+
    private final String formatOld = "&#"; // 4.1.2-

    @Override
    public @NotNull String reformat(@NotNull String text) {
        return text.contains(formatNew) ? text.replace(formatNew, "#") :
                text.contains(formatOld) ? text.replace(formatOld, "#") : text;
    }
}