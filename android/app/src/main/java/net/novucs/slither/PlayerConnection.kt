package net.novucs.slither

import android.bluetooth.*
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class PlayerConnection(private val context: SlitherActivity,
                       private val device: BluetoothDevice) : BluetoothGattCallback() {

    private val servicesFound = AtomicBoolean(false)
    private val subscriptionRequests = LinkedBlockingQueue<SubscriptionRequest>()
    private val bleSafeRequests = LinkedList<SubscriptionRequest>()
    private val subscriptions = mutableMapOf<Subscription, (data: ByteArray) -> Unit>()
    private var disconnectCallback: (() -> Unit)? = null
    private var gatt: BluetoothGatt? = null
    private var connected = false

    fun connect() {
        if (connected) {
            throw IllegalStateException("Already connected")
        }

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
        servicesFound.set(true)
        createNextSubscription()
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        createNextSubscription()
    }

    private fun createNextSubscription() {
        subscriptionRequests.drainTo(bleSafeRequests)
        if (bleSafeRequests.isEmpty()) {
            return
        }

        val request = bleSafeRequests.pop()
        if (request != null) {
            subscribe(request.callback, request.serviceId, request.characteristicId)
        }
    }

    fun subscribe(callback: (data: ByteArray) -> Unit, serviceId: UUID, characteristicId: UUID) {
        val gatt = this.gatt ?: return
        if (!servicesFound.get()) {
            subscriptionRequests.add(SubscriptionRequest(callback, serviceId, characteristicId))
            return
        }

        val service = gatt.getService(serviceId)
        if (service == null) {
            gatt.disconnect()
            return
        }

        val characteristic = service.getCharacteristic(characteristicId)
        if (characteristic == null) {
            gatt.disconnect()
            return
        }

        if (characteristic.descriptors.isEmpty()) {
            gatt.disconnect()
            return
        }

        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.descriptors[0]
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)

        val subscription = subscriptionOf(characteristic)
        subscriptions[subscription] = callback
    }

    private fun subscriptionOf(characteristic: BluetoothGattCharacteristic): Subscription {
        return Subscription(characteristic.service.uuid, characteristic.uuid)
    }

    private data class SubscriptionRequest(val callback: (data: ByteArray) -> Unit,
                                           val serviceId: UUID,
                                           val characteristicId: UUID)

    private data class Subscription(private val serviceId: UUID,
                                    private val characteristicId: UUID)
}
