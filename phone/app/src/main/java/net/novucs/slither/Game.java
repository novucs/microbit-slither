package net.novucs.slither;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Game implements Runnable {

    public static final int MAP_HEIGHT = 24;
    public static final int MAP_WIDTH = 32;
    public static final int REQUIRED_PLAYER_COUNT = 2;
    public static final int INITIAL_REWARD_COUNT = 5;

    private final Random random = new Random();
    private final GameView view;
    private final List<Vector2i> rewards = new LinkedList<>();
    private final Player player1;
    private final Player player2;

    public Game(GameView view, Player player1, Player player2) {
        this.view = view;
        this.player1 = player1;
        this.player2 = player2;
    }

    public GameView getView() {
        return view;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    @Override
    public void run() {
        for (int i = 0; i < INITIAL_REWARD_COUNT; i++) {
            rewards.add(nextValidSpawn());
        }

        player1.getBody().add(nextValidSpawn());
        player2.getBody().add(nextValidSpawn());

        while (!Thread.interrupted()) {
            tick();
            view.getSnapshot().set(snapshot());
            view.invalidate();

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private GameSnapshot snapshot() {
        return new GameSnapshot(new ArrayList<>(rewards),
                new ArrayList<>(player1.getBody()),
                new ArrayList<>(player2.getBody()));
    }

    private Vector2i nextValidSpawn() {
        while (true) {
            int x = random.nextInt(MAP_WIDTH);
            int y = random.nextInt(MAP_HEIGHT);
            Vector2i location = new Vector2i(x, y);

            if (!location.collides(player1.getBody()) &&
                    !location.collides(player2.getBody()) &&
                    !location.collides(rewards)) {
                return location;
            }
        }
    }

    private void tick() {
        tickPlayer(player1, player2);
        tickPlayer(player2, player1);
    }

    private void tickPlayer(Player player, Player opponent) {
        // Player has not made a move from their starting position yet.
        Vector2i movement = player.getMovement().get();
        if (movement.isDefault()) {
            return;
        }

        for (int i = 0; i < player.getSpeed(); i++) {
            move(player, opponent, movement);
        }
    }

    private void move(Player player, Player opponent, Vector2i movement) {
        Vector2i head = player.getBody().getLast().add(movement);

        if (head.getX() > MAP_WIDTH) {
            head = head.setX(0);
        } else if (head.getX() < 0) {
            head = head.setX(MAP_WIDTH);
        }

        if (head.getY() > MAP_HEIGHT) {
            head = head.setY(0);
        } else if (head.getY() < 0) {
            head = head.setY(MAP_HEIGHT);
        }

        player.getBody().addLast(head);

        if (player.getGrowthTicks() > 0) {
            player.decrementGrowthTicks();
        } else {
            player.getBody().removeFirst();
        }

        // Reward the player when they consume a reward.
        Iterator<Vector2i> rewardIterator = rewards.iterator();
        while (rewardIterator.hasNext()) {
            Vector2i reward = rewardIterator.next();
            if (head.collides(reward)) {
                rewardIterator.remove();
                player.reward();
                rewards.add(nextValidSpawn());
                break;
            }
        }

        // Reset the player if they collide with their opponent.
        if (head.collides(opponent.getBody())) {
            player.getBody().clear();
            player.setScore(0);
            player.getBody().add(nextValidSpawn());
        }
    }
}
