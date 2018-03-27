package net.novucs.slither

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import java.util.concurrent.atomic.AtomicBoolean

class SlitherActivity : AppCompatActivity() {

    private val connectedAddresses = mutableSetOf<String>()

    private val requestingPermissions = AtomicBoolean(false)

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        // Try to fetch the Bluetooth adapter.
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        var adapter: BluetoothAdapter? = bluetoothManager.adapter

        // When either not found or not enabled, attempt to enable the
        // Bluetooth service and try again.
        if (adapter == null || !adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE_BLUETOOTH)
            adapter = bluetoothManager.adapter
        }

        // Provide the adapter when found, otherwise except. No devices without
        // Bluetooth LE should have this app installed.
        return@lazy adapter ?: throw RuntimeException("No Bluetooth adapter found")
    }

    private val game: Game by lazy {
        val player1 = Player(findViewById(R.id.playerImage1))
        val player2 = Player(findViewById(R.id.playerImage2))
        val gameView = findViewById<GameView>(R.id.game)
        return@lazy Game(gameView, player1, player2)
    }

    private val scanCallback = BluetoothAdapter.LeScanCallback { device, _, _ ->
        runOnUiThread { connect(device) }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        ensurePermissionsGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

        // Scan until all players have been found.
        bluetoothAdapter.startLeScan(scanCallback)

        val gameThread = Thread(game)
        gameThread.start()
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
                connectedAddresses.size == Game.REQUIRED_PLAYER_COUNT) {
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
        if (connectedAddresses.size == Game.REQUIRED_PLAYER_COUNT) {
            bluetoothAdapter.stopLeScan(scanCallback)
            game.state = GameState.PLAY
        }

        // Connect to the device.
        connection.connect()

        // Register a disconnect callback.
        connection.onDisconnect {
            // Update player and device status to disconnected.
            connectedAddresses.remove(device.address)
            player.connection = null

            // Update the game state to connecting.
            game.state = GameState.CONNECT

            // Begin scanning again.
            bluetoothAdapter.startLeScan(scanCallback)

            // Set the players avatar back to unconnected.
            runOnUiThread {
                val avatar = player.avatar
                avatar.setImageResource(R.drawable.snakegrey)
                avatar.rotationX = 0f
                avatar.rotationY = 0f
            }
        }

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
        private const val REQUEST_CODE_ENABLE_BLUETOOTH = 1
        private const val REQUEST_CODE_PERMISSION = 2

        // Low pass alpha value, smaller is more smooth.
        private const val AVATAR_ROTATION_SMOOTH_RATE = 0.1
    }
}
