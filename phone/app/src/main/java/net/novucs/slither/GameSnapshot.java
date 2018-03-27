package net.novucs.slither;

import java.util.Deque;
import java.util.List;

public interface GameSnapshot {

    GameState getState();

    class Connect implements GameSnapshot {

        private final String message;

        public Connect(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public GameState getState() {
            return GameState.CONNECT;
        }
    }

    class Play implements GameSnapshot {

        private final List<Vector2i> rewards;
        private final Deque<Vector2i> player1;
        private final Deque<Vector2i> player2;

        public Play(List<Vector2i> rewards, Deque<Vector2i> player1, Deque<Vector2i> player2) {
            this.rewards = rewards;
            this.player1 = player1;
            this.player2 = player2;
        }

        public List<Vector2i> getRewards() {
            return rewards;
        }

        public Deque<Vector2i> getPlayer1() {
            return player1;
        }

        public Deque<Vector2i> getPlayer2() {
            return player2;
        }

        @Override
        public GameState getState() {
            return GameState.PLAY;
        }
    }

    class Complete implements GameSnapshot {

        private final String message;

        public Complete(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public GameState getState() {
            return GameState.COMPLETE;
        }
    }
}
