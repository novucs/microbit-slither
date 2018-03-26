package net.novucs.slither;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final long SCAN_PERIOD = 10000;

    private final Map<String, PlayerConnection> connections = new HashMap<>();
    private final AtomicBoolean requestingPermissions = new AtomicBoolean(false);
    private Handler handler = new Handler();
    private BluetoothAdapter bluetoothAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        }

        scan();
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

    private void scan() {
        // Begin the scan.
        bluetoothAdapter.startLeScan(mLeScanCallback);

        // Stop the scan after the given period.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }, SCAN_PERIOD);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (device.getName() == null || !device.getName().contains("BBC micro:bit") ||
                            connections.containsKey(device.getAddress())) {
                        return;
                    }

                    System.out.println(device.getName() + device.getAddress());

                    ImageView image;
                    if (connections.isEmpty()) {
                        image = findViewById(R.id.playerImage1);
                    } else {
                        image = findViewById(R.id.playerImage2);
                    }

                    PlayerConnection connection = new PlayerConnection(MainActivity.this, device, image);
                    connection.connect();
                    connections.put(device.getAddress(), connection);
                }
            });
        }
    };
}
