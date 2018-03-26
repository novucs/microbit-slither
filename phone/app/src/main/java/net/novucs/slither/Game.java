package net.novucs.slither;

import java.util.LinkedList;
import java.util.List;

public class Game implements Runnable {
    public static final int MAP_HEIGHT = 24;
    public static final int MAP_WIDTH = 32;
    public static final int REQUIRED_PLAYER_COUNT = 2;

    private final GameView view;
    private Player player1;
    private Player player2;

    public Game(GameView view) {
        this.view = view;
    }

    public GameView getView() {
        return view;
    }

    public Player getPlayer1() {
        return player1;
    }

    public void setPlayer1(Player player1) {
        this.player1 = player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    @Override
    public void run() {
        List<Vector2i> rewards = new LinkedList<>();
        rewards.add(new Vector2i(3, 10));
        List<Vector2i> player1 = new LinkedList<>();
        player1.add(new Vector2i(5, 16));
        List<Vector2i> player2 = new LinkedList<>();
        player2.add(new Vector2i(16, 5));
        GameSnapshot snapshot = new GameSnapshot(rewards, player1, player2);
        view.getSnapshot().set(snapshot);

        while (!Thread.interrupted()) {
            tick();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void tick() {
        // TODO: Implement game logic.
    }
}
