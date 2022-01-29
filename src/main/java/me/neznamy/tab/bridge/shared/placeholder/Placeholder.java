package me.neznamy.tab.bridge.shared.placeholder;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Placeholder {

    private final String identifier;
    private int refresh;
    private final AtomicInteger atomicInteger = new AtomicInteger();

    protected Placeholder(String identifier, int refresh) {
        this.identifier = identifier;
        this.refresh = refresh;
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isInPeriod() {
        return atomicInteger.addAndGet(50) % refresh == 0;
    }

    public void setRefresh(int refresh) {
        this.refresh = refresh;
    }
}
