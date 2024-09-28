package me.neznamy.tab.bridge.bukkit.platform;

import org.bukkit.plugin.java.JavaPlugin;

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
    public void cancelTasks() {
        // No tasks to cancel
    }
}
