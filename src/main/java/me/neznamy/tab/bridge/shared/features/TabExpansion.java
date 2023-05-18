package me.neznamy.tab.bridge.shared.features;

public interface TabExpansion {

    boolean isRegistered();

    boolean register();

    boolean unregister();

    void setValue(Object player, String identifier, String value);
}
