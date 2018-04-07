package net.novucs.slither

import android.bluetooth.*
import net.novucs.slither.BLEAttributes.SNAKE_MESSAGE_SERVICE
import net.novucs.slither.BLEAttributes.SNAKE_TX_CHARACTERISTIC
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

/**
 * Handles all bluetooth gatt connection callbacks for a single player.
 * Provides a layer over the android gatt api to synchronise all requests.
 */
class PlayerConnection(private val context: SlitherActivity,
                       private val device: BluetoothDevice) : BluetoothGattCallback() {

    private val operations = LinkedBlockingQueue<() -> Unit>()
    private val subscriptions = mutableMapOf<Subscription, (data: ByteArray) -> Unit>()
    private var disconnectCallback: (() -> Unit)? = null
    private var gatt: BluetoothGatt? = null
    private var connected = false
    private var busy = false

    /**
     * Connects to the found bluetooth device.
     */
    fun connect() {
        if (connected) {
            throw IllegalStateException("Already connected")
        }

        busy = true
        connected = true
        gatt = device.connectGatt(context, false, this)
    }

    /**
     * Sets a handler for disconnecting.
     *
     * @param callback the callback to invoke when the device disconnects.
     */
    fun onDisconnect(callback: () -> Unit) {
        disconnectCallback = callback
    }

    /**
     * Discovers new services on first connection, calls the disconnect
     * callback on disconnection.
     */
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            disconnectCallback?.invoke()
        }
    }

    /**
     * Invokes any subscribed callbacks when notified of a characteristic
     * change.
     */
    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val subscription = subscriptionOf(characteristic)
        val callback = subscriptions[subscription]
        callback?.invoke(characteristic.value)
    }

    /**
     * Executes any queued operations after service request has been fulfilled.
     */
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        executeNextOperation()
    }

    /**
     * Executes any queued operations after service write has been fulfilled.
     */
    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        executeNextOperation()
    }

    /**
     * Executes any queued operations after service read has been fulfilled.
     */
    override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        executeNextOperation()
    }

    /**
     * Sets the service to not busy, polls and invokes the next operation.
     */
    private fun executeNextOperation() {
        busy = false
        operations.poll()?.invoke()
    }

    /**
     * Subscribes to all notifications for a particular characteristic of a
     * given service.
     *
     * @param callback the callback to be invoked on notification.
     * @param serviceId the service ID to subscribe to.
     * @param characteristicId the characteristic ID to subscribe to.
     */
    fun subscribe(callback: (data: ByteArray) -> Unit, serviceId: UUID, characteristicId: UUID) {
        synchronized(busy) {
            if (busy) {
                operations += { subscribe(callback, serviceId, characteristicId) }
                return
            }
            busy = true
        }

        val (gatt, characteristic) = findCharacteristic(serviceId, characteristicId) ?: return

        if (characteristic.descriptors.isEmpty()) {
            gatt.disconnect()
            return
        }

        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.descriptors.first()
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)

        val subscription = subscriptionOf(characteristic)
        subscriptions[subscription] = callback
    }

    /**
     * Sends a text message via gatt to the player. The player's micro:bit
     * should then scroll this message to the LED display once sent.
     */
    fun sendMessage(message: String) {
        synchronized(busy) {
            if (busy) {
                operations += { sendMessage(message) }
                return
            }
            busy = true
        }

        val (gatt, characteristic) = findCharacteristic(
                serviceId = SNAKE_MESSAGE_SERVICE,
                characteristicId = SNAKE_TX_CHARACTERISTIC)
                ?: return

        characteristic.value = message.toByteArray()
        gatt.writeCharacteristic(characteristic)
        busy = false
    }

    /**
     * Finds a characteristic, given its service and characteristic IDs.
     * Disconnects from the device if the characteristic was unable to be found.
     *
     * @param serviceId the service to search for.
     * @param characteristicId the characteristic to search for.
     */
    private fun findCharacteristic(serviceId: UUID, characteristicId: UUID):
            Pair<BluetoothGatt, BluetoothGattCharacteristic>? {
        // Attempt to find and return the characteristic.
        val gatt = this.gatt
        gatt?.getService(serviceId)?.getCharacteristic(characteristicId)?.let {
            return Pair(gatt, it)
        }

        // Disconnect from device on failure, as we are incompatible.
        gatt?.disconnect()
        return null
    }

    /**
     * Represents a notification subscription key, using a composite of the
     * listened service ID and characteristic ID.
     */
    private data class Subscription(private val serviceId: UUID, private val characteristicId: UUID)

    /**
     * Constructs a new subscription using a characteristic.
     *
     * @param characteristic the characteristic to create a subscription of.
     */
    private fun subscriptionOf(characteristic: BluetoothGattCharacteristic): Subscription {
        return Subscription(characteristic.service.uuid, characteristic.uuid)
    }
}
