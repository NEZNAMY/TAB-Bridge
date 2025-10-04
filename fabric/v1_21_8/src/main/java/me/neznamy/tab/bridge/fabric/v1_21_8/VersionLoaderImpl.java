package me.neznamy.tab.bridge.fabric.v1_21_8;

import com.mojang.authlib.GameProfile;
import lombok.NonNull;
import me.neznamy.tab.bridge.fabric.VersionLoader;
import me.neznamy.tab.bridge.shared.TABBridge;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * CustomPayloadManager implementation for 1.20.5 - 1.21.8.
 */
@SuppressWarnings("unused") // Used via reflection
public class VersionLoaderImpl implements VersionLoader {

    @Override
    public void registerListeners() {
        // Client to server, makes perfect sense to me registering this on server side
        PayloadTypeRegistry.playC2S().register(TabCustomPacketPayload.TYPE, TabCustomPacketPayload.codec(Integer.MAX_VALUE));
        PayloadTypeRegistry.configurationC2S().register(TabCustomPacketPayload.TYPE, TabCustomPacketPayload.codec(Integer.MAX_VALUE));

        // This is needed, otherwise sending packet will cause ClassCastException
        PayloadTypeRegistry.playS2C().register(TabCustomPacketPayload.TYPE, TabCustomPacketPayload.codec(Integer.MAX_VALUE));
        PayloadTypeRegistry.configurationS2C().register(TabCustomPacketPayload.TYPE, TabCustomPacketPayload.codec(Integer.MAX_VALUE));

        ServerPlayNetworking.registerGlobalReceiver(TabCustomPacketPayload.TYPE, TabCustomPacketPayload::handle);
        ServerConfigurationNetworking.registerGlobalReceiver(TabCustomPacketPayload.TYPE, TabCustomPacketPayload::handle);
    }

    @Override
    public void sendCustomPayload(@NonNull ServerPlayer player, byte @NonNull [] message) {
        ServerPlayNetworking.send(player, new TabCustomPacketPayload(message));
    }

    @Override
    @NotNull
    public Level getLevel(@NonNull ServerPlayer player) {
        return player.level();
    }

    @Override
    @NotNull
    public Component newTextComponent(@NonNull String text) {
        return Component.literal(text);
    }

    @Override
    @NotNull
    public String getName(@NonNull GameProfile profile) {
        return profile.getName();
    }

    public record TabCustomPacketPayload(byte[] data) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<TabCustomPacketPayload> TYPE = new CustomPacketPayload.Type<>(ID);

        public static <T extends FriendlyByteBuf> StreamCodec<T, TabCustomPacketPayload> codec(int maxSize) {
            return CustomPacketPayload.codec((value, output) -> {
                output.writeBytes(value.data);
            }, (buffer) -> {
                int i = buffer.readableBytes();
                if (i >= 0 && i <= maxSize) {
                    byte[] data = new byte[i];
                    buffer.readBytes(data);
                    return new TabCustomPacketPayload(data);
                } else {
                    throw new IllegalArgumentException("Payload may not be larger than " + maxSize + " bytes");
                }
            });
        }

        @Override
        @NotNull
        public CustomPacketPayload.Type<TabCustomPacketPayload> type() {
            return TYPE;
        }

        public void handle(@NonNull ServerPlayNetworking.Context context) {
            TABBridge.getInstance().submitTask(
                    () -> TABBridge.getInstance().getDataBridge().processPluginMessage(context.player(), context.player().getUUID(), data, false));
        }

        public void handle(@NonNull ServerConfigurationNetworking.Context context) {
            TABBridge.getInstance().submitTask(
                    () -> TABBridge.getInstance().getDataBridge().processPluginMessage(context.networkHandler().getOwner().getId(), data, false));
        }
    }
}
