package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import org.jetbrains.annotations.NotNull;

public class ScoreboardScore implements IncomingMessage {

    private String objective;
    private int action;
    private String scoreHolder;
    private int score;
    private String displayName;
    private String numberFormat;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        objective = in.readUTF();
        action = in.readInt();
        scoreHolder = in.readUTF();
        if (action == 0) {
            score = in.readInt();
            if (in.readBoolean()) displayName = in.readUTF();
            if (in.readBoolean()) numberFormat = in.readUTF();
        }
    }

    @Override
    public void process(@NotNull BridgePlayer player) {
        if (action == 0) {
            player.getScoreboard().setScore(objective, scoreHolder, score, displayName, numberFormat);
        } else {
            player.getScoreboard().removeScore(objective, scoreHolder);
        }
    }
}
