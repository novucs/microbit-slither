package net.novucs.slither

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView

import java.util.concurrent.atomic.AtomicBoolean

class MenuActivity : AppCompatActivity() {

    private val requestingPermissions = AtomicBoolean(false)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val avatar1 = findViewById<ImageView>(R.id.playerImage1)
        val avatar2 = findViewById<ImageView>(R.id.playerImage2)
        val player1 = Player(avatar1)
        val player2 = Player(avatar2)
        val gameView = findViewById<GameView>(R.id.game)
        val game = Game(gameView, player1, player2)

        ensurePermissionsGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

        // Initializes Bluetooth adapter.
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        var bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            bluetoothAdapter = bluetoothManager.adapter
        }

        // Scan until all players have been found.
        val scanCallback = ScanCallback(this, bluetoothAdapter!!, game)
        bluetoothAdapter.startLeScan(scanCallback)

        val gameThread = Thread(game)
        gameThread.start()
    }

    private fun ensurePermissionsGranted(vararg permissions: String) {
        for (permission in permissions) {
            while (!hasPermission(permission)) {
                if (!requestingPermissions.get()) {
                    requestingPermissions.set(true)
                    requestPermissions(permission)
                }
                Thread.yield()
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions(vararg permissions: String) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        requestingPermissions.set(false)
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val PERMISSION_REQUEST_CODE = 2
    }
}
