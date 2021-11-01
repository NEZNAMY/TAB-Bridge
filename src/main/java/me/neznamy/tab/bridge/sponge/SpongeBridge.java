package me.neznamy.tab.bridge.sponge;

import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.network.ChannelBinding.RawDataChannel;
import org.spongepowered.api.network.PlayerConnection;
import org.spongepowered.api.plugin.Plugin;

import me.neznamy.tab.bridge.shared.DataBridge;

@Plugin(id = "tab-bridge", name = "TAB-Bridge", version = "1.5.3", description = "Bridge to extend features with TAB on bungeecord", authors = {"NEZNAMY"})
public class SpongeBridge {

	private final String CHANNEL_NAME = "tab:placeholders";
	
	private RawDataChannel rawDataChannel;
	private DataBridge data;
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		rawDataChannel = Sponge.getChannelRegistrar().createRawChannel(this, CHANNEL_NAME);
		rawDataChannel.addListener(Platform.Type.SERVER, (channelBuf, remoteConnection, type) -> {

			if (remoteConnection instanceof PlayerConnection) {
				byte[] bytes = new byte[channelBuf.available()];
				int count = channelBuf.available();
				for (int i=0; i<count; i++) {
					bytes[i] = channelBuf.readByte();
				}
				data.processPluginMessage(((PlayerConnection) remoteConnection).getPlayer(), bytes);
			}
		});
		data = new SpongeDataBridge(this, rawDataChannel);
	}
	
	@Listener
	public void onServerStop(GameStoppingServerEvent event) {
		Sponge.getChannelRegistrar().unbindChannel(rawDataChannel);
	}
}