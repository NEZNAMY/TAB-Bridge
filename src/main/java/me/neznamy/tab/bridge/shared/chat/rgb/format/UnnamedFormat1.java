package me.neznamy.tab.bridge.shared.chat.rgb.format;

import me.neznamy.tab.bridge.shared.chat.EnumChatFormat;
import org.jetbrains.annotations.NotNull;

/**
 * Formatter for &amp;#RRGGBB
 */
public class UnnamedFormat1 implements RGBFormatter {

    private final String format = EnumChatFormat.COLOR_STRING + "#";

    @Override
    public @NotNull String reformat(@NotNull String text) {
        return text.contains(format) ? text.replace(format, "#") : text;
    }
}