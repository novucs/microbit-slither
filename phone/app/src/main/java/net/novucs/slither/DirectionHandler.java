package net.novucs.slither;

public class DirectionHandler implements PlayerConnection.OnNotification {

    private final Player player;

    public DirectionHandler(Player player) {
        this.player = player;
    }

    @Override
    public void onNotification(byte[] data) {
        int x = data[0];
        int y = data[1];
        Vector2i movement = new Vector2i(x, y);
        player.getDirection().set(movement);
    }
}
