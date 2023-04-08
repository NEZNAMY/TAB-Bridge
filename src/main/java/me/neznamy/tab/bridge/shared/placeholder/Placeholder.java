package me.neznamy.tab.bridge.shared.placeholder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
public abstract class Placeholder {

    @Getter protected final String identifier;
    @Setter private int refresh;
    private final AtomicInteger atomicInteger = new AtomicInteger();

    public boolean isInPeriod() {
        return atomicInteger.addAndGet(50) % refresh == 0;
    }
}
