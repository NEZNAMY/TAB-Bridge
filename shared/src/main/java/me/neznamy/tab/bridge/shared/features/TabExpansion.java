package me.neznamy.tab.bridge.shared.features;

import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;

public interface TabExpansion {

    boolean unregister();

    void setValue(@NonNull BridgePlayer player, @NonNull String identifier, @NonNull String value);
}
