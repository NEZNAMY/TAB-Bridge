package me.neznamy.tab.bridge.shared;

import me.neznamy.tab.bridge.shared.placeholder.Placeholder;

import java.util.UUID;

public interface Platform {

    boolean isOnline(Object player);

    UUID getUniqueId(Object player);

    void scheduleSyncRepeatingTask(Runnable task, int intervalTicks);

    void runTask(Runnable task);

    BridgePlayer newPlayer(Object player);

    Placeholder createPlaceholder(String publicIdentifier, String privateIdentifier, int refresh);

    void cancelTasks();
}
