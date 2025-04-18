package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@AllArgsConstructor
public class PlayerJoinResponse implements OutgoingMessage {

    @NonNull
    private String world;

    @Nullable
    private String group;

    @NonNull
    private Map<String, Object> placeholders;

    private int gameMode;

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
