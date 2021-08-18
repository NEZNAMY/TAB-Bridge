package me.neznamy.tab.bridge.shared;

import java.io.File;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import me.neznamy.tab.bridge.shared.config.YamlConfigurationFile;

public abstract class DataBridge {

	private YamlConfigurationFile config;
	protected boolean exceptionThrowing;
	
	protected void loadConfig() {
		try {
			config = new YamlConfigurationFile(getClass().getClassLoader().getResourceAsStream("config.yml"), new File(getDataFolder(), "config.yml"));
			exceptionThrowing = config.getBoolean("throw-placeholderapi-exceptions", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public abstract File getDataFolder();
	
	public abstract boolean isDisguised(Object player);

	public abstract boolean isVanished(Object player);
	
	public abstract void sendPluginMessage(Object player, ByteArrayDataOutput message);
	
	public abstract String parsePlaceholder(Object player, String placeholder);
	
	public abstract boolean hasPermission(Object player, String permission);
	
	public abstract String getWorld(Object player);
	
	public abstract String getGroup(Object player);
	
	public abstract boolean hasInvisibilityPotion(Object player);
	
	public void processPluginMessage(Object player, byte[] bytes) {
		ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
		String subChannel = in.readUTF();
		if (subChannel.equalsIgnoreCase("Placeholder")){
			String identifier = in.readUTF();
			long start = System.nanoTime();
			String output = parsePlaceholder(player, identifier);
			long time = System.nanoTime() - start;
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Placeholder");
			out.writeUTF(identifier);
			out.writeUTF(output);
			out.writeLong(time);
			sendPluginMessage(player, out);
		}
		if (subChannel.equalsIgnoreCase("Attribute")){
			String attribute = in.readUTF();
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Attribute");
			out.writeUTF(attribute);
			if (attribute.startsWith("hasPermission:")) {
				String permission = attribute.substring(attribute.indexOf(":") + 1);
				out.writeUTF(hasPermission(player, permission)+"");
				sendPluginMessage(player, out);
				return;
			}
			if (attribute.equals("invisible")) {
				out.writeUTF(hasInvisibilityPotion(player)+"");
				sendPluginMessage(player, out);
				return;
			}
			if (attribute.equals("disguised")) {
				out.writeUTF(isDisguised(player)+"");
				sendPluginMessage(player, out);
				return;
			}
			if (attribute.equals("vanished")) {
				out.writeUTF(isVanished(player)+"");
				sendPluginMessage(player, out);
				return;
			}
			if (attribute.equals("world")) {
				out.writeUTF(getWorld(player));
				sendPluginMessage(player, out);
				return;
			}
		}
		if (subChannel.equalsIgnoreCase("Group")) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Group");
			out.writeUTF(getGroup(player));
			sendPluginMessage(player, out);
		}
	}
}
