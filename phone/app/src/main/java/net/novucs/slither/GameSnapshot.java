package net.novucs.slither;

import java.util.List;

public class GameSnapshot {

    private final List<Vector2i> rewards;
    private final List<Vector2i> player1;
    private final List<Vector2i> player2;

    public GameSnapshot(List<Vector2i> rewards, List<Vector2i> player1, List<Vector2i> player2) {
        this.rewards = rewards;
        this.player1 = player1;
        this.player2 = player2;
    }

    public List<Vector2i> getRewards() {
        return rewards;
    }

    public List<Vector2i> getPlayer1() {
        return player1;
    }

    public List<Vector2i> getPlayer2() {
        return player2;
    }
}
