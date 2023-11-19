package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class ScoreboardTeam implements IncomingMessage {

    private String name;
    private int action;
    private String prefix;
    private String suffix;
    private int options;
    private String visibility;
    private String collision;
    private int color;
    private Collection<String> players;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        name = in.readUTF();
        action = in.readInt();
        if (action == 0 || action == 2) {
            prefix = in.readUTF();
            suffix = in.readUTF();
            options = in.readInt();
            visibility = in.readUTF();
            collision = in.readUTF();
            color = in.readInt();
        }
        if (action == 0) {
            int playerCount = in.readInt();
            players = new ArrayList<>();
            for (int i=0; i<playerCount; i++) {
                players.add(in.readUTF());
            }
        }
    }

    @Override
    public void process(@NotNull BridgePlayer player) {
        if (action == 0) {
            player.getScoreboard().registerTeam(name, prefix, suffix, visibility, collision, players, options, color);
        } else if (action == 1) {
            player.getScoreboard().unregisterTeam(name);
        } else if (action == 2) {
            player.getScoreboard().updateTeam(name, prefix, suffix, visibility, collision, options, color);
        }
    }
}
