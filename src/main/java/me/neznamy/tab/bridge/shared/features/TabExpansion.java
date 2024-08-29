package me.neznamy.tab.bridge.shared.features;

import me.neznamy.tab.bridge.shared.BridgePlayer;

public interface TabExpansion {

    boolean unregister();

    void setValue(BridgePlayer player, String identifier, String value);
}
