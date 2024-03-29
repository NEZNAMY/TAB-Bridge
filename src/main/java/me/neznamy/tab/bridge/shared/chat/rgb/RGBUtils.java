package me.neznamy.tab.bridge.shared.chat.rgb;

import lombok.Getter;
import me.neznamy.tab.bridge.shared.chat.rgb.format.*;
import me.neznamy.tab.bridge.shared.chat.rgb.gradient.CMIGradient;
import me.neznamy.tab.bridge.shared.chat.rgb.gradient.CommonGradient;
import me.neznamy.tab.bridge.shared.chat.rgb.gradient.GradientPattern;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * A helper class to reformat all RGB formats into the default #RRGGBB and apply gradients
 */
public class RGBUtils {

    /** Instance of the class */
    @Getter private static final RGBUtils instance = new RGBUtils();

    /** Registered RGB formatters */
    private final RGBFormatter[] formats;

    /** Registered gradient patterns */
    private final GradientPattern[] gradients;

    /**
     * Constructs new instance and loads all RGB patterns and gradients
     */
    public RGBUtils() {
        List<RGBFormatter> list = new ArrayList<>();
        if (ReflectionUtils.classExists("net.kyori.adventure.text.minimessage.MiniMessage")) {
            list.add(new MiniMessageFormat());
        }
        list.add(new BukkitFormat());
        list.add(new CMIFormat());
        list.add(new UnnamedFormat1());
        list.add(new HtmlFormat());
        list.add(new KyoriFormat());
        formats = list.toArray(new RGBFormatter[0]);

        gradients = new GradientPattern[] {
                //{#RRGGBB>}text{#RRGGBB<}
                new CMIGradient(),
                //<#RRGGBB>Text</#RRGGBB>
                new CommonGradient(Pattern.compile("<#[0-9a-fA-F]{6}>[^<]*</#[0-9a-fA-F]{6}>"),
                        Pattern.compile("<#[0-9a-fA-F]{6}\\|.>[^<]*</#[0-9a-fA-F]{6}>"),
                        "<#", 9, 2, 9, 7),
                //<$#RRGGBB>Text<$#RRGGBB>
                new CommonGradient(Pattern.compile("<\\$#[0-9a-fA-F]{6}>[^<]*<\\$#[0-9a-fA-F]{6}>"),
                        Pattern.compile("<\\$#[0-9a-fA-F]{6}\\|.>[^<]*<\\$#[0-9a-fA-F]{6}>"),
                        "<$", 10, 3, 10, 7)
        };
    }

    /**
     * Applies all RGB formats and gradients to text and returns it.
     *
     * @param   text
     *          original text
     * @return  text where everything is converted to #RRGGBB
     */
    public @NotNull String applyFormats(@NotNull String text) {
        String replaced = text;
        for (GradientPattern pattern : gradients) {
            replaced = pattern.applyPattern(replaced, false);
        }
        for (RGBFormatter formatter : formats) {
            replaced = formatter.reformat(replaced);
        }
        return replaced;
    }

    /**
     * Returns true if entered string is a valid 6-digit combination of
     * hexadecimal numbers, false if not
     *
     * @param   string
     *          string to check
     * @return  {@code true} if valid, {@code false} if not
     */
    public boolean isHexCode(@NotNull String string) {
        if (string.length() != 6) return false;
        for (int i=0; i<6; i++) {
            char c = string.charAt(i);
            if (c < 48 || (c > 57 && c < 65) || (c > 70 && c < 97) || c > 102) return false;
        }
        return true;
    }
}