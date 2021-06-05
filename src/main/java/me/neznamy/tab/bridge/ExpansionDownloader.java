package me.neznamy.tab.bridge;

import java.io.File;
import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Utility to execute /papi ecloud download <expansion> on expansions which are used but not installed
 * If an expansion was successfully downloaded (new expansion count is higher than previous in expansions folder),
 * /papi reload is executed.
 * Since this feature was added into the plugin amount of non-working placeholders caused by missing expansion went down rapidly.
 * I'm not sure why, must be a coinscidence.
 */
public class ExpansionDownloader {

	private JavaPlugin plugin;
	private ExecutorService exe = Executors.newCachedThreadPool();
	
	public ExpansionDownloader(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	
	public void download(String expansion) {
		if (expansion.equals("rel")) return;
		exe.submit(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
					if (PlaceholderAPI.getRegisteredIdentifiers().contains(expansion)) return;
					File expansionsFolder = new File(Bukkit.getPluginManager().getPlugin("PlaceholderAPI").getDataFolder(), "expansions");
					int oldExpansionDownloadedCount = expansionsFolder.listFiles().length;
					Bukkit.getConsoleSender().sendMessage("\u00a7d[TAB] Expansion \u00a7e" + expansion + "\u00a7d is used but not installed. Installing!");
					runSyncCommand("papi ecloud download " + expansion);
					Thread.sleep(5000);
					if (expansionsFolder.listFiles().length > oldExpansionDownloadedCount) {
						Bukkit.getConsoleSender().sendMessage("\u00a7d[TAB] Reloading PlaceholderAPI for the changes to take effect.");
						runSyncCommand("papi reload");
					}
				} catch (InterruptedException | ConcurrentModificationException e) {
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void download(Set<String> expansions) {
		//starting the task once the server is fully loaded (including PlaceholderAPI expansions)
		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

			@Override
			public void run() {
				//to not freeze the server with Thread.sleep
				exe.submit(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(2000);
							expansions.removeAll(PlaceholderAPI.getRegisteredIdentifiers());
							if (!expansions.isEmpty()) {
								File expansionsFolder = new File(Bukkit.getPluginManager().getPlugin("PlaceholderAPI").getDataFolder(), "expansions");
								int oldExpansionDownloadedCount = expansionsFolder.listFiles().length;
								for (String expansion : expansions) {
									Bukkit.getConsoleSender().sendMessage("\u00a7d[TAB] Expansion \u00a7e" + expansion + "\u00a7d is used but not installed. Installing!");
									runSyncCommand("papi ecloud download " + expansion);
									Thread.sleep(5000);
								}
								if (expansionsFolder.listFiles().length > oldExpansionDownloadedCount) {
									Bukkit.getConsoleSender().sendMessage("\u00a7d[TAB] Reloading PlaceholderAPI for the changes to take effect.");
									runSyncCommand("papi reload");
								}
							}
						} catch (InterruptedException | ConcurrentModificationException e) {
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				});
			}
		}, 1);
	}
	public void runSyncCommand(String command) {
		//back to main thread as commands need to be ran in it
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

			@Override
			public void run() {
				try {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
				} catch (Exception e) {
					//papi ecloud is disabled
				}
			}
		});
	}
}