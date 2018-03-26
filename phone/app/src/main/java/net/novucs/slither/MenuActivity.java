package net.novucs.slither;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.novucs.slither.BLEAttributes.ACCELEROMETER_DATA_CHARACTERISTIC;
import static net.novucs.slither.BLEAttributes.ACCELEROMETER_SERVICE;

public class MenuActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final int REQUIRED_PLAYER_COUNT = 2;

    private final Map<String, Player> players = new HashMap<>();
    private final AtomicBoolean requestingPermissions = new AtomicBoolean(false);
    private final Handler handler = new Handler();
    private BluetoothAdapter bluetoothAdapter;
    private boolean displayMovements = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        ensurePermissionsGranted(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION);

        // Initializes Bluetooth adapter.
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            throw new RuntimeException("Bluetooth service not found!");
        }

        bluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        // Scan until all players have been found.
        bluetoothAdapter.startLeScan(scanCallback);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (players.size() != REQUIRED_PLAYER_COUNT) {
                    alert("This game requires two players to be connected!");
                    return;
                }

                displayMovements = false;
                System.out.println("POSTING");

                for (Player player : players.values()) {
                    player.getConnection().getGatt().disconnect();
                }

                System.out.println("SWITCHING");
                Intent gameActivity = new Intent(MenuActivity.this, GameActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBinder("players", new BinderWrapper<>(players));
                gameActivity.putExtras(bundle);
                startActivity(gameActivity);
            }
        });
//        findViewById(R.id.button).setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (players.size() != REQUIRED_PLAYER_COUNT) {
//                    alert("This game requires two players to be connected!");
//                    return false;
//                }
//
//                displayMovements = false;
//                System.out.println("POSTING");
//
//                for (Player player : players.values()) {
//                    player.getConnection().getGatt().disconnect();
//                }
//
//                new Timer().schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        System.out.println("SWITCHING");
//                        Intent gameActivity = new Intent(MenuActivity.this, GameActivity.class);
//                        Bundle bundle = new Bundle();
//                        bundle.putBinder("players", new BinderWrapper<>(players));
//                        gameActivity.putExtras(bundle);
//                        startActivity(gameActivity);
//                    }
//                }, 5000);
//                return false;
//            }
//        });
    }

    public void alert(final String message) {
        new AlertDialog.Builder(MenuActivity.this)
                .setTitle("Alert")
                .setMessage(message)
                .setPositiveButton("Okay!",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Ignore.
                            }
                        }).show();
    }

    private void ensurePermissionsGranted(String... permissions) {
        for (String permission : permissions) {
            while (!hasPermission(permission)) {
                if (!requestingPermissions.get()) {
                    requestingPermissions.set(true);
                    requestPermissions(permission);
                }
                Thread.yield();
            }
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions(String... permissions) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        requestingPermissions.set(false);
    }

    private BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (device.getName() == null ||
                            !device.getName().contains("BBC micro:bit") ||
                            players.containsKey(device.getAddress()) ||
                            players.size() == REQUIRED_PLAYER_COUNT) {
                        return;
                    }

                    System.out.println(device.getName() + device.getAddress());

                    ImageView avatar;
                    if (players.isEmpty()) {
                        avatar = findViewById(R.id.playerImage1);
                        avatar.setImageResource(R.drawable.snakeblue);
                    } else {
                        avatar = findViewById(R.id.playerImage2);
                        avatar.setImageResource(R.drawable.snakered);
                    }

                    PlayerConnection connection = new PlayerConnection(MenuActivity.this, device);
                    Player player = new Player(connection, avatar);
                    AvatarPreview avatarPreview = new AvatarPreview(player);
                    connection.connect();
                    players.put(device.getAddress(), player);
                    connection.subscribe(avatarPreview, ACCELEROMETER_SERVICE,
                            ACCELEROMETER_DATA_CHARACTERISTIC);

                    if (players.size() == REQUIRED_PLAYER_COUNT) {
                        bluetoothAdapter.stopLeScan(scanCallback);
                    }
                }
            });
        }
    };

    private class AvatarPreview implements PlayerConnection.OnNotification {

        private final Player player;
        private double[] previousRotation = null;

        private AvatarPreview(Player player) {
            this.player = player;
        }

        @Override
        public void onNotification(byte[] data) {
            if (!displayMovements) {
                return;
            }

            double x = ((data[1] << 8) + data[0]) / 1000f;
            double y = ((data[3] << 8) + data[2]) / 1000f;
            double z = ((data[5] << 8) + data[4]) / 1000f;
            previousRotation = lowPass(new double[]{x, y, z}, previousRotation);
            x = previousRotation[0];
            y = previousRotation[1];
            z = previousRotation[2];

            double radian = 180 / Math.PI;
            double pitch = Math.atan(x / Math.sqrt(Math.pow(y, 2) + Math.pow(z, 2))) * radian;
            double roll = -Math.atan(y / Math.sqrt(Math.pow(x, 2) + Math.pow(z, 2))) * radian;

            player.getAvatar().setRotationX((float) roll);
            player.getAvatar().setRotationY((float) pitch);
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
}
