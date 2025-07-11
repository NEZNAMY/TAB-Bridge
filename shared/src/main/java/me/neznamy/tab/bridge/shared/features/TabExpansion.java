package me.neznamy.tab.bridge.shared.features;

import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is an interface representing TAB's PlaceholderAPI expansion.
 */
public interface TabExpansion {

    /** Pattern to match placeholders using %% */
    Pattern placeholderPattern = Pattern.compile("%([^%]*)%");

    /**
     * Registers the expansion.
     *
     * @return  {@code true} if the expansion was unregistered successfully, {@code false} otherwise
     */
    boolean unregister();

    /**
     * Sets the value of specified placeholder for the specified player.
     *
     * @param   player
     *          Player to set the value for
     * @param   identifier
     *          TAB placeholder
     * @param   value
     *          Value of the placeholder
     */
    void setValue(@NonNull BridgePlayer player, @NonNull String identifier, @NonNull String value);

    /**
     * Returns a list of all placeholders detected in the specified text.
     *
     * @param   text
     *          Text to detect placeholders in
     * @return  List of all detected placeholders
     */
    @NotNull
    default List<String> detectPlaceholders(@NonNull String text) {
        if (!text.contains("%")) return Collections.emptyList();
        List<String> placeholders = new ArrayList<>();
        Matcher m = placeholderPattern.matcher(text);
        while (m.find()) {
            placeholders.add(m.group());
        }
        return placeholders;
    }
}
