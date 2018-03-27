package net.novucs.slither;

public class AvatarPreviewHandler implements PlayerConnection.OnNotification {

    private final MenuActivity menuActivity;
    private final Player player;
    private double[] previousRotation = null;

    public AvatarPreviewHandler(MenuActivity menuActivity, Player player) {
        this.menuActivity = menuActivity;
        this.player = player;
    }

    @Override
    public void onNotification(byte[] data) {
        double x = ((data[1] << 8) + data[0]) / 1000f;
        double y = ((data[3] << 8) + data[2]) / 1000f;
        double z = ((data[5] << 8) + data[4]) / 1000f;
        previousRotation = lowPass(new double[]{x, y, z}, previousRotation);
        x = previousRotation[0];
        y = previousRotation[1];
        z = previousRotation[2];

        double radian = 180 / Math.PI;
        final double pitch = Math.atan(x / Math.sqrt(Math.pow(y, 2) + Math.pow(z, 2))) * radian;
        final double roll = -Math.atan(y / Math.sqrt(Math.pow(x, 2) + Math.pow(z, 2))) * radian;

        menuActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                player.getAvatar().setRotationX((float) roll);
                player.getAvatar().setRotationY((float) pitch);
            }
        });
    }

    private double[] lowPass(double[] a, double[] b) {
        if (b == null) {
            return a;
        }

        for (int i = 0; i < a.length; i++) {
            b[i] += 0.1 * (a[i] - b[i]);
        }

        return b;
    }
}
