package net.novucs.slither

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.concurrent.atomic.AtomicBoolean


class SlitherActivity : AppCompatActivity() {

    private val connectedAddresses = mutableSetOf<String>()

    private val requestingPermissions = AtomicBoolean(false)

    private val scanner = BluetoothLeScannerCompat.getScanner()

    private val game: Game by lazy {
        val player1 = Player(findViewById(R.id.playerImage1))
        val player2 = Player(findViewById(R.id.playerImage2))
        val gameView = findViewById<GameView>(R.id.game)
        return@lazy Game(gameView, player1, player2)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            // Attempt to connect to all valid found devices via the UI thread.
            result?.device?.let { device -> runOnUiThread { connect(device) } }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        ensurePermissionsGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

        startScan()

        val gameThread = Thread(game)
        gameThread.start()
    }

    private fun startScan() {
        // BLE Scanning does not find all services UUIDs advertised, we're
        // disconnecting from devices found invalid when connecting. It appears
        // this is down to an unfixed Android bug for scanning 128bit UUIDs.
//        val settings = ScanSettings.Builder().build()
//        val filters = BLEAttributes.REQUIRED_SERVICES.map { serviceId ->
//            ScanFilter.Builder().setServiceUuid(ParcelUuid(serviceId)).build()
//        }.toMutableList()
//        scanner.startScan(filters, settings, scanCallback)
        scanner.startScan(scanCallback)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        requestingPermissions.set(false)
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
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION)
    }

    private fun connect(device: BluetoothDevice) {
        // Do not connect to this device if its not a micro:bit, it's not
        // already connected, or if we've reached the player cap.
        if (device.name == null ||
                !device.name.contains("BBC micro:bit") ||
                connectedAddresses.contains(device.address) ||
                connectedAddresses.size == Game.PLAYER_COUNT) {
            return
        }

        // Find the next player profile to connect and set their avatar.
        val player: Player
        if (game.player1.connection == null) {
            player = game.player1
            player.avatar.setImageResource(R.drawable.snakeblue)
        } else {
            player = game.player2
            player.avatar.setImageResource(R.drawable.snakered)
        }

        // Update player connection status.
        val connection = PlayerConnection(this, device)
        player.connection = connection
        connectedAddresses.add(device.address)

        // Stop scanning and play the game once we have reached the required
        // player count.
        if (connectedAddresses.size == Game.PLAYER_COUNT) {
            scanner.stopScan(scanCallback)
            game.state = GameState.PLAY
        }

        // Register a disconnect callback.
        connection.onDisconnect {
            // Update player and device status to disconnected.
            connectedAddresses.remove(device.address)
            player.connection = null

            // Update the game state to connecting.
            game.state = GameState.CONNECT

            // Begin scanning again.
            startScan()

            // Set the players avatar back to unconnected.
            runOnUiThread {
                val avatar = player.avatar
                avatar.setImageResource(R.drawable.snakegrey)
                avatar.rotationX = 0f
                avatar.rotationY = 0f
            }
        }

        // Connect to the device.
        connection.connect()

        // Subscribe to the accelerometer characteristic.
        // Updates the player avatar rotation to match with the micro:bit.
        val rotation: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
        connection.subscribe({ data ->
            // Decode the three rotations as little-endian shorts.
            var x = ((data[1].toInt() shl 8) + data[0]) / 1000.0
            var y = ((data[3].toInt() shl 8) + data[2]) / 1000.0
            var z = ((data[5].toInt() shl 8) + data[4]) / 1000.0

            // Apply low pass filtering to smooth the rotation transition.
            doubleArrayOf(x, y, z).withIndex().forEach { (i, value) ->
                rotation[i] += AVATAR_ROTATION_SMOOTH_RATE * (value - rotation[i])
            }

            // Update each of the rotations with the filtered values.
            x = rotation[0]
            y = rotation[1]
            z = rotation[2]

            // Calculate the roll and pitch.
            val radian = 180 / Math.PI
            val pitch = Math.atan(x / Math.sqrt(Math.pow(y, 2.0) + Math.pow(z, 2.0))) * radian
            val roll = -Math.atan(y / Math.sqrt(Math.pow(x, 2.0) + Math.pow(z, 2.0))) * radian

            // Display micro:bit rotation on the players avatar.
            runOnUiThread {
                player.avatar.rotationX = roll.toFloat()
                player.avatar.rotationY = pitch.toFloat()
            }
        }, BLEAttributes.ACCELEROMETER_SERVICE, BLEAttributes.ACCELEROMETER_DATA_CHARACTERISTIC)

        // Subscribe to the snake direction characteristic.
        // Updates the player direction on notification.
        connection.subscribe({ data ->
            val x = data[0].toInt()
            val y = data[1].toInt()
            val movement = Vector2i(x, y)
            player.direction.set(movement)
        }, BLEAttributes.SNAKE_MOVE_SERVICE, BLEAttributes.SNAKE_DIRECTION_CHARACTERISTIC)

        // Subscribe to the snake speed characteristic.
        // Updates the player speed on notification.
        connection.subscribe({ data ->
            val speed = data[0].toInt()
            player.speed.set(speed)
        }, BLEAttributes.SNAKE_MOVE_SERVICE, BLEAttributes.SNAKE_SPEED_CHARACTERISTIC)
    }

    companion object {
        // Android request codes.
        private const val REQUEST_CODE_PERMISSION = 1

        // Low pass alpha value, smaller is more smooth.
        private const val AVATAR_ROTATION_SMOOTH_RATE = 0.1
    }
}
