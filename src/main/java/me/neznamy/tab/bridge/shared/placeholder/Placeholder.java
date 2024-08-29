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

    /** Placeholder's identifier starting and ending with % */
    protected final String identifier;

    /** Placeholder's refresh interval, must be divisible by 50 */
    @Setter private int refresh;
}
