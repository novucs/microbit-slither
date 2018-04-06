package net.novucs.slither

import android.bluetooth.*
import net.novucs.slither.BLEAttributes.SNAKE_MESSAGE_SERVICE
import net.novucs.slither.BLEAttributes.SNAKE_TX_CHARACTERISTIC
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class PlayerConnection(private val context: SlitherActivity,
                       private val device: BluetoothDevice) : BluetoothGattCallback() {

    private val operations = LinkedBlockingQueue<() -> Unit>()
    private val subscriptions = mutableMapOf<Subscription, (data: ByteArray) -> Unit>()
    private var disconnectCallback: (() -> Unit)? = null
    private var gatt: BluetoothGatt? = null
    private var connected = false
    private var busy = false

    fun connect() {
        if (connected) {
            throw IllegalStateException("Already connected")
        }

        busy = true
        connected = true
        gatt = device.connectGatt(context, false, this)
    }

    fun onDisconnect(callback: () -> Unit) {
        disconnectCallback = callback
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            disconnectCallback?.invoke()
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val subscription = subscriptionOf(characteristic)
        val callback = subscriptions[subscription]
        callback?.invoke(characteristic.value)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        executeNextOperation()
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        executeNextOperation()
    }

    override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        executeNextOperation()
    }

    private fun executeNextOperation() {
        busy = false
        operations.poll()?.invoke()
    }

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

    private data class Subscription(private val serviceId: UUID, private val characteristicId: UUID)

    private fun subscriptionOf(characteristic: BluetoothGattCharacteristic): Subscription {
        return Subscription(characteristic.service.uuid, characteristic.uuid)
    }
}
