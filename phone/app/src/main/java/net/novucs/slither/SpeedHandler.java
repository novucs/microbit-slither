package net.novucs.slither;

public class SpeedHandler implements PlayerConnection.OnNotification {

    private final Player player;

    public SpeedHandler(Player player) {
        this.player = player;
    }

    @Override
    public void onNotification(byte[] data) {
        int speed = data[0];
        player.getSpeed().set(speed);
    }
}
