package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@AllArgsConstructor
public class PlayerJoinResponse implements OutgoingMessage {

    private String world;
    private String group;
    private Map<String, Object> placeholders;
    private int gameMode;
    
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public ByteArrayDataOutput write() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerJoinResponse");
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
        return out;
    }
}
