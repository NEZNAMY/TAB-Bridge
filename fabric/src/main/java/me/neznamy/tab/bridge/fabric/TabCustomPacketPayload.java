package me.neznamy.tab.bridge.fabric;

import lombok.NonNull;
import me.neznamy.tab.bridge.shared.TABBridge;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TabCustomPacketPayload(byte[] data) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.tryParse(TABBridge.CHANNEL_NAME);
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
