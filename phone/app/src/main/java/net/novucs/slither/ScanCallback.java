package net.novucs.slither;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.ImageView;

import java.util.HashSet;
import java.util.Set;

import static net.novucs.slither.BLEAttributes.ACCELEROMETER_DATA_CHARACTERISTIC;
import static net.novucs.slither.BLEAttributes.ACCELEROMETER_SERVICE;
import static net.novucs.slither.BLEAttributes.SNAKE_DIRECTION_CHARACTERISTIC;
import static net.novucs.slither.BLEAttributes.SNAKE_MOVE_SERVICE;
import static net.novucs.slither.BLEAttributes.SNAKE_SPEED_CHARACTERISTIC;
import static net.novucs.slither.Game.REQUIRED_PLAYER_COUNT;

public class ScanCallback implements BluetoothAdapter.LeScanCallback {

    private final Set<String> connectedAddresses = new HashSet<>();
    private final MenuActivity context;
    private final BluetoothAdapter bluetoothAdapter;
    private final Game game;

    public ScanCallback(MenuActivity context, BluetoothAdapter bluetoothAdapter, Game game) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.game = game;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        DeviceFoundHandler handler = new DeviceFoundHandler(device);
        context.runOnUiThread(handler);
    }

    private class DeviceFoundHandler implements Runnable {

        private final BluetoothDevice device;

        private DeviceFoundHandler(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public void run() {
            if (device.getName() == null ||
                    !device.getName().contains("BBC micro:bit") ||
                    connectedAddresses.contains(device.getAddress()) ||
                    connectedAddresses.size() == REQUIRED_PLAYER_COUNT) {
                return;
            }

            PlayerConnection connection = new PlayerConnection(context, device);
            Player player;

            if (game.getPlayer1().getConnection() == null) {
                player = game.getPlayer1();
                context.runOnUiThread(new AvatarUpdater(player, R.drawable.snakeblue));
            } else {
                player = game.getPlayer2();
                context.runOnUiThread(new AvatarUpdater(player, R.drawable.snakered));
            }

            player.setConnection(connection);
            AvatarPreviewHandler avatarPreview = new AvatarPreviewHandler(context, player);
            DirectionHandler directionHandler = new DirectionHandler(player);
            SpeedHandler speedHandler = new SpeedHandler(player);

            connectedAddresses.add(device.getAddress());

            final Player finalPlayer = player;

            // Connect to the device.
            connection.connect(new Runnable() {
                @Override
                public void run() {
                    // Remove the players connection stats on disconnect.
                    connectedAddresses.remove(device.getAddress());
                    game.setState(GameState.CONNECT);
                    bluetoothAdapter.startLeScan(ScanCallback.this);
                    finalPlayer.setConnection(null);
                    context.runOnUiThread(new AvatarUpdater(finalPlayer, R.drawable.snakegrey));
                }
            });

            // Subscribe to all services essential to the game.
            connection.subscribe(avatarPreview, ACCELEROMETER_SERVICE, ACCELEROMETER_DATA_CHARACTERISTIC);
            connection.subscribe(directionHandler, SNAKE_MOVE_SERVICE, SNAKE_DIRECTION_CHARACTERISTIC);
            connection.subscribe(speedHandler, SNAKE_MOVE_SERVICE, SNAKE_SPEED_CHARACTERISTIC);

            // Stop scanning if we have reached the required player count.
            if (connectedAddresses.size() == REQUIRED_PLAYER_COUNT) {
                bluetoothAdapter.stopLeScan(ScanCallback.this);
                game.setState(GameState.PLAY);
            }
        }
    }

    private class AvatarUpdater implements Runnable {

        private final Player player;
        private final int resourceId;

        public AvatarUpdater(Player player, int resourceId) {
            this.player = player;
            this.resourceId = resourceId;
        }

        @Override
        public void run() {
            ImageView avatar = player.getAvatar();
            avatar.setImageResource(resourceId);
            avatar.setRotationX(0);
            avatar.setRotationY(0);
        }
    }
}
