package net.novucs.slither;

import android.widget.ImageView;

public class Player {
    private final PlayerConnection connection;
    private final ImageView avatar;

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
}
