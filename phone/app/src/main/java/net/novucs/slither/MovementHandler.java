package net.novucs.slither;

public class MovementHandler implements PlayerConnection.OnNotification {

    private final Player player;

    public MovementHandler(Player player) {
        this.player = player;
    }

    @Override
    public void onNotification(byte[] data) {
        int x = (data[1] << 8) + (data[0] & 0xFF);
        int y = (data[3] << 8) + (data[2] & 0xFF);
        Vector2i movement = new Vector2i(x, y);
        player.getMovement().set(movement);
    }
}
