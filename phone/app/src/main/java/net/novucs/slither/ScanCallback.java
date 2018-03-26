package net.novucs.slither;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.HashSet;
import java.util.Set;

import static net.novucs.slither.BLEAttributes.ACCELEROMETER_DATA_CHARACTERISTIC;
import static net.novucs.slither.BLEAttributes.ACCELEROMETER_SERVICE;
import static net.novucs.slither.BLEAttributes.SNAKE_MOVE_CHARACTERISTIC;
import static net.novucs.slither.BLEAttributes.SNAKE_MOVE_SERVICE;
import static net.novucs.slither.Game.REQUIRED_PLAYER_COUNT;

public class ScanCallback implements BluetoothAdapter.LeScanCallback {

    private final Set<String> connectedAddresses = new HashSet<>();
    private final MenuActivity menuActivity;
    private final BluetoothAdapter bluetoothAdapter;
    private final Game game;

    public ScanCallback(MenuActivity menuActivity, BluetoothAdapter bluetoothAdapter, Game game) {
        this.menuActivity = menuActivity;
        this.bluetoothAdapter = bluetoothAdapter;
        this.game = game;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        DeviceFoundHandler handler = new DeviceFoundHandler(device);
        menuActivity.runOnUiThread(handler);
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

            PlayerConnection connection = new PlayerConnection(menuActivity, device);
            Player player;

            if (connectedAddresses.isEmpty()) {
                player = game.getPlayer1();
                player.getAvatar().setImageResource(R.drawable.snakeblue);
            } else {
                player = game.getPlayer2();
                player.getAvatar().setImageResource(R.drawable.snakered);
            }

            player.setConnection(connection);
            MovementHandler movementHandler = new MovementHandler(player);
            AvatarPreviewHandler avatarPreview = new AvatarPreviewHandler(player);

            connectedAddresses.add(device.getAddress());

            // Connect to the device and subscribe to the accelerometer data
            // and snake move characteristics.
            connection.connect();
            connection.subscribe(avatarPreview, ACCELEROMETER_SERVICE, ACCELEROMETER_DATA_CHARACTERISTIC);
            connection.subscribe(movementHandler, SNAKE_MOVE_SERVICE, SNAKE_MOVE_CHARACTERISTIC);

            // Stop scanning if we have reached the required player count.
            if (connectedAddresses.size() == REQUIRED_PLAYER_COUNT) {
                bluetoothAdapter.stopLeScan(ScanCallback.this);
            }
        }
    }
}
