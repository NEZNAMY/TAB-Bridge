package me.neznamy.tab.bridge.fabric.v1_20_4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.NonNull;
import me.neznamy.tab.bridge.fabric.VersionLoader;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * CustomPayloadManager implementation for 1.20 - 1.20.4.
 */
@SuppressWarnings("unused") // Used via reflection
public class VersionLoaderImpl implements VersionLoader {

    @Override
    public void registerListeners() {
        ServerPlayNetworking.registerGlobalReceiver(ID, this::receive);
        if (ReflectionUtils.classExists("net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking")) {
            // 1.20.2+
            ServerConfigurationNetworking.registerGlobalReceiver(ID, this::receive);
        }
    }

    private void receive(MinecraftServer var1, ServerPlayer var2, ServerGamePacketListenerImpl var3, FriendlyByteBuf var4, PacketSender var5) {
        byte[] data = new byte[var4.readableBytes()];
        // Duplicate because FriendlyByteBuf#readBytes went from deobfuscated to obfuscated between 1.20.1 and 1.20.2
        // But duplicate method is the same and so is the returned ByteBuf
        var4.duplicate().readBytes(data);
        TABBridge.getInstance().submitTask(
                () -> TABBridge.getInstance().getDataBridge().processPluginMessage(var2, var2.getUUID(), data, false));
    }

    private void receive(MinecraftServer var1, ServerConfigurationPacketListenerImpl var2, FriendlyByteBuf var3, PacketSender var4) {
        byte[] data = new byte[var3.readableBytes()];
        var3.duplicate().readBytes(data);
        TABBridge.getInstance().submitTask(
                () -> TABBridge.getInstance().getDataBridge().processPluginMessage(var2.getOwner().getId(), data, false));
    }

    @Override
    public void sendCustomPayload(@NonNull ServerPlayer player, byte @NonNull [] message) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(message);
        ServerPlayNetworking.send(player, ID, new FriendlyByteBuf(buf));
    }

    @Override
    @NotNull
    public Level getLevel(@NonNull ServerPlayer player) {
        return player.level();
    }
}
