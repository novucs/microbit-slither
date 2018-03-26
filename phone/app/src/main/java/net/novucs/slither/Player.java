package net.novucs.slither;

import android.widget.ImageView;

import java.util.concurrent.atomic.AtomicReference;

public class Player {
    private final PlayerConnection connection;
    private final ImageView avatar;
    private final AtomicReference<Vector2i> movement = new AtomicReference<>();

    public Player(PlayerConnection connection, ImageView avatar) {
        this.connection = connection;
        this.avatar = avatar;
    }

    public PlayerConnection getConnection() {
        return connection;
    }

    public ImageView getAvatar() {
        return avatar;
    }

    public AtomicReference<Vector2i> getMovement() {
        return movement;
    }
}
