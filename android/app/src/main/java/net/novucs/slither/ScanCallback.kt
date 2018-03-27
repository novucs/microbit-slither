package net.novucs.slither

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import net.novucs.slither.BLEAttributes.ACCELEROMETER_DATA_CHARACTERISTIC
import net.novucs.slither.BLEAttributes.ACCELEROMETER_SERVICE
import net.novucs.slither.BLEAttributes.SNAKE_DIRECTION_CHARACTERISTIC
import net.novucs.slither.BLEAttributes.SNAKE_MOVE_SERVICE
import net.novucs.slither.BLEAttributes.SNAKE_SPEED_CHARACTERISTIC
import net.novucs.slither.Game.Companion.REQUIRED_PLAYER_COUNT

class ScanCallback(private val context: MenuActivity,
                   private val bluetoothAdapter: BluetoothAdapter,
                   private val game: Game) : BluetoothAdapter.LeScanCallback {

    private val connectedAddresses = mutableSetOf<String>()

    private fun lowPass(alpha: Double, real: DoubleArray, filtered: DoubleArray) {
        real.withIndex().forEach { (i, value) ->
            filtered[i] += alpha * (value - filtered[i])
        }
    }

    override fun onLeScan(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray) {
        context.runOnUiThread { connect(device) }
    }

    private fun connect(device: BluetoothDevice) {
        // Do not connect to this device if its not a micro:bit, it's not
        // already connected, or if we've reached the player cap.
        if (device.name == null ||
                !device.name.contains("BBC micro:bit") ||
                connectedAddresses.contains(device.address) ||
                connectedAddresses.size == REQUIRED_PLAYER_COUNT) {
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
        val connection = PlayerConnection(context, device)
        player.connection = connection
        connectedAddresses.add(device.address)

        // Stop scanning and play the game once we have reached the required
        // player count.
        if (connectedAddresses.size == REQUIRED_PLAYER_COUNT) {
            bluetoothAdapter.stopLeScan(this)
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
            bluetoothAdapter.startLeScan(this)

            // Set the players avatar back to unconnected.
            context.runOnUiThread {
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
            // Decode the three little-endian shorts provided.
            var x = ((data[1].toInt() shl 8) + data[0]) / 1000.0
            var y = ((data[3].toInt() shl 8) + data[2]) / 1000.0
            var z = ((data[5].toInt() shl 8) + data[4]) / 1000.0

            // Apply low pass filtering to smooth the screen appearance.
            lowPass(0.1, doubleArrayOf(x, y, z), rotation)

            // Update each of the accelerations with the filtered values.
            x = rotation[0]
            y = rotation[1]
            z = rotation[2]

            // Calculate the roll and pitch.
            val radian = 180 / Math.PI
            val pitch = Math.atan(x / Math.sqrt(Math.pow(y, 2.0) + Math.pow(z, 2.0))) * radian
            val roll = -Math.atan(y / Math.sqrt(Math.pow(x, 2.0) + Math.pow(z, 2.0))) * radian

            // Display micro:bit rotation on the players avatar.
            context.runOnUiThread {
                player.avatar.rotationX = roll.toFloat()
                player.avatar.rotationY = pitch.toFloat()
            }
        }, ACCELEROMETER_SERVICE, ACCELEROMETER_DATA_CHARACTERISTIC)

        // Subscribe to the snake direction characteristic.
        // Updates the player direction on notification.
        connection.subscribe({ data ->
            val x = data[0].toInt()
            val y = data[1].toInt()
            val movement = Vector2i(x, y)
            player.direction.set(movement)
        }, SNAKE_MOVE_SERVICE, SNAKE_DIRECTION_CHARACTERISTIC)

        // Subscribe to the snake speed characteristic.
        // Updates the player speed on notification.
        connection.subscribe({ data ->
            val speed = data[0].toInt()
            player.speed.set(speed)
        }, SNAKE_MOVE_SERVICE, SNAKE_SPEED_CHARACTERISTIC)
    }
}
