package net.novucs.slither;

import android.widget.ImageView;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

public class Player {
    private final ImageView avatar;
    private final AtomicReference<Vector2i> movement = new AtomicReference<>(new Vector2i());
    private final Deque<Vector2i> body = new LinkedList<>();
    private PlayerConnection connection;
    private int growthTicks = 0;
    private int score = 0;
    private int speed = 1;

    public Player(ImageView avatar) {
        this.connection = connection;
        this.avatar = avatar;
    }

    public PlayerConnection getConnection() {
        return connection;
    }

    public void setConnection(PlayerConnection connection) {
        this.connection = connection;
    }

    public ImageView getAvatar() {
        return avatar;
    }

    public AtomicReference<Vector2i> getMovement() {
        return movement;
    }

    public Deque<Vector2i> getBody() {
        return body;
    }

    public int getGrowthTicks() {
        return growthTicks;
    }

    public void setGrowthTicks(int growthTicks) {
        this.growthTicks = growthTicks;
    }

    public void decrementGrowthTicks() {
        this.growthTicks--;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void reward() {
        score += 1;
        growthTicks += 3;
    }
}
