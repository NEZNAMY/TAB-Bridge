package me.neznamy.tab.bridge.bukkit;

import me.neznamy.tab.bridge.shared.features.TabExpansion;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class FoliaPlatform extends BukkitPlatform {

    public FoliaPlatform(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public void scheduleSyncRepeatingTask(Runnable task, int intervalTicks) {
        // Do not refresh sync placeholders on folia
    }

    @Override
    public void runTask(Runnable task) {
        // Do not initialize sync placeholders
    }

    @Override
    public void registerExpansion(@NotNull TabExpansion expansion) {
        expansion.register();
    }

    @Override
    public void cancelTasks() {
        // No tasks to cancel
    }
}
