package net.novucs.slither;

import java.util.Collection;

public class GameSnapshot {

    private final Collection<Vector2i> rewards;
    private final Collection<Vector2i> player1;
    private final Collection<Vector2i> player2;

    public GameSnapshot(Collection<Vector2i> rewards, Collection<Vector2i> player1, Collection<Vector2i> player2) {
        this.rewards = rewards;
        this.player1 = player1;
        this.player2 = player2;
    }

    public Collection<Vector2i> getRewards() {
        return rewards;
    }

    public Collection<Vector2i> getPlayer1() {
        return player1;
    }

    public Collection<Vector2i> getPlayer2() {
        return player2;
    }
}
