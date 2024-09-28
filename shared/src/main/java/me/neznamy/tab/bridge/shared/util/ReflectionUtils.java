package me.neznamy.tab.bridge.shared.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class storing methods working with reflection
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReflectionUtils {

    /**
     * Returns {@code true} if class with given full name exists,
     * {@code false} if not.
     *
     * @param   path
     *          Full class path and name
     * @return  {@code true} if exists, {@code false} if not
     */
    public static boolean classExists(@NotNull String path) {
        try {
            Class.forName(path);
            return true;
        } catch (ClassNotFoundException | NullPointerException e) {
            return false;
        }
    }
}