package me.neznamy.tab.bridge.sponge;

import java.io.File;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.TargetWorldEvent;
import org.spongepowered.api.network.ChannelBinding.RawDataChannel;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import me.neznamy.tab.bridge.shared.DataBridge;
import me.rojo8399.placeholderapi.impl.PlaceholderServiceImpl;

public class SpongeDataBridge extends DataBridge {

	private RawDataChannel channel;
	
	public SpongeDataBridge(Object plugin, RawDataChannel channel) {
		this.channel = channel;
		loadConfig();
		Sponge.getEventManager().registerListeners(plugin, this);
	}
	
	@Listener
	public void onWorldChange(TargetWorldEvent e) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Attribute");
		out.writeUTF("world");
		out.writeUTF(getWorld(e.getSource()));
		sendPluginMessage(e.getSource(), out);
	}
	
	@Override
	public boolean isDisguised(Object player) {
		return false;
	}

	@Override
	public boolean isVanished(Object player) {
		return false;
	}

	@Override
	public void sendPluginMessage(Object player, ByteArrayDataOutput message) {
		channel.sendTo((Player) player, buf -> buf.writeBytes(message.toByteArray()));
	}

	@Override
	public File getDataFolder() {
		return new File("mods" + File.separatorChar + "TAB-Bridge");
	}

	@Override
	public String parsePlaceholder(Object player, String placeholder) {
		try {
			if (Sponge.getPluginManager().getPlugin("placeholderapi").isPresent()) {
				return PlaceholderServiceImpl.get().parse(placeholder, player, null).toString();
			} else {
				return "<PlaceholderAPI is not installed>";
			}
		} catch (Throwable e) {
			if (exceptionThrowing) {
				System.out.println("[TAB-Bridge] Placeholder " + placeholder + " threw an exception when parsing for player " + ((Player) player).getName());
				e.printStackTrace();
			}
			return "<PlaceholderAPI ERROR>";
		}
	}

	@Override
	public boolean hasPermission(Object player, String permission) {
		return ((Player)player).hasPermission(permission);
	}

	@Override
	public String getWorld(Object player) {
		return ((Player)player).getWorld().getName();
	}

	@Override
	public String getGroup(Object player) {
		return "TODO";
	}

	@Override
	public boolean hasInvisibilityPotion(Object player) {
		return false; //TODO
	}
}