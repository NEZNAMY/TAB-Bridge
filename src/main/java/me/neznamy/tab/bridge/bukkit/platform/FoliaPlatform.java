package me.neznamy.tab.bridge.bukkit.platform;

import lombok.SneakyThrows;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

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

    @SneakyThrows
    @SuppressWarnings("JavaReflectionMemberAccess")
    private void runSync(Entity entity, Runnable task) {
        Object entityScheduler = Entity.class.getMethod("getScheduler").invoke(entity);
        Consumer<?> consumer = $ -> task.run(); // Reflection and lambdas don't go together
        entityScheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class)
                .invoke(entityScheduler, BukkitBridge.getInstance(), consumer, null);
    }

    @Override
    public void runEntityTask(Entity entity, Runnable task) {
        runSync(entity, task);
    }
}
