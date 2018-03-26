package net.novucs.slither;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.concurrent.atomic.AtomicBoolean;

public class MenuActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;

    private final AtomicBoolean requestingPermissions = new AtomicBoolean(false);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        GameView gameView = findViewById(R.id.game);
        Game game = new Game(gameView);

        ensurePermissionsGranted(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION);

        // Initializes Bluetooth adapter.
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            throw new RuntimeException("Bluetooth service not found!");
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        // Scan until all players have been found.
        ScanCallback scanCallback = new ScanCallback(this, bluetoothAdapter, game);
        bluetoothAdapter.startLeScan(scanCallback);

        Thread gameThread = new Thread(game);
        gameThread.start();
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
}
