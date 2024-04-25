package me.neznamy.tab.bridge.shared.placeholder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public abstract class Placeholder {

    protected static final boolean PRINT_WARNS = true;

    protected final String identifier;
    @Setter private int refresh;
}
