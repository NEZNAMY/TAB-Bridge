package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Outgoing message sent as a response to PlayerJoinRequest containing
 * all necessary data to initialize the player on the proxy side.
 */
@AllArgsConstructor
@ToString
public class PlayerJoinResponse implements OutgoingMessage {

    /** Name of the world the player is in */
    @NonNull
    private final String world;

    /** Permission group of the player, null if group forwarding is disabled */
    @Nullable
    private final String group;

    /** Values for all currently registered placeholders */
    @NonNull
    private final Map<String, Object> placeholders;

    /** Player's gamemode */
    private final int gameMode;

    @SuppressWarnings("unchecked")
    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeUTF(world);
        if (group != null) out.writeUTF(group);
        out.writeInt(placeholders.size());
        for (Map.Entry<String, Object> placeholder : placeholders.entrySet()) {
            out.writeUTF(placeholder.getKey());
            if (placeholder.getKey().startsWith("%rel_")) {
                Map<String, String> perPlayer = (Map<String, String>) placeholder.getValue();
                out.writeInt(perPlayer.size());
                for (Map.Entry<String, String> entry : perPlayer.entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeUTF(entry.getValue());
                }
            } else {
                out.writeUTF(placeholder.getValue().toString());
            }
        }
        out.writeInt(gameMode);
    }
}
