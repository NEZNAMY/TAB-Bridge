package me.neznamy.tab.bridge.shared.placeholder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for all placeholder types.
 */
@Getter
@AllArgsConstructor
public abstract class Placeholder {

    /** Flag deciding whether warns about placeholders taking too long to respond should be printed or not */
    protected static final boolean PRINT_WARNS = true;

    /** Placeholder's identifier starting and ending with % */
    protected final String identifier;

    /** Placeholder's refresh interval, must be divisible by 50 */
    @Setter private int refresh;
}
