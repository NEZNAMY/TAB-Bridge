package me.neznamy.tab.bridge.shared.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

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
    public static boolean classExists(@NonNull String path) {
        try {
            Class.forName(path);
            return true;
        } catch (ClassNotFoundException | NullPointerException e) {
            return false;
        }
    }
}