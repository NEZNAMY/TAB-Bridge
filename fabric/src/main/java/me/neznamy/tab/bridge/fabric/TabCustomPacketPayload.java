package me.neznamy.tab.bridge.fabric;

import me.neznamy.tab.bridge.shared.TABBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Custom packet payload for TAB's custom plugin message channel for 1.20.5+.
 *
 * @param   data
 *          The payload data
 */
public record TabCustomPacketPayload(byte[] data) implements CustomPacketPayload {

    /** Resource location for the custom payload channel */
    @NotNull
    public static final Identifier ID = Objects.requireNonNull(Identifier.tryParse(TABBridge.CHANNEL_NAME));

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
}