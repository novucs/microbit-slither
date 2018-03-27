package net.novucs.slither;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerConnection extends BluetoothGattCallback {

    private final AtomicBoolean servicesFound = new AtomicBoolean(false);
    private final LinkedBlockingQueue<SubscriptionRequest> subscriptionRequests = new LinkedBlockingQueue<>();
    private final LinkedList<SubscriptionRequest> bleSafeRequests = new LinkedList<>();
    private final Map<Subscription, OnNotification> subscriptions = new HashMap<>();
    private final MenuActivity activity;
    private final BluetoothDevice device;
    private Runnable disconnectCallback;
    private BluetoothGatt gatt;
    private boolean connected = false;

    public PlayerConnection(MenuActivity activity, BluetoothDevice device) {
        this.activity = activity;
        this.device = device;
    }

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public void connect(Runnable disconnectCallback) {
        if (connected) {
            throw new IllegalStateException("Already connected");
        }

        this.disconnectCallback = disconnectCallback;
        connected = true;
        gatt = device.connectGatt(activity, false, this);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            disconnectCallback.run();
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Subscription subscription = Subscription.of(characteristic);
        OnNotification onNotification = subscriptions.get(subscription);
        if (onNotification != null) {
            onNotification.onNotification(characteristic.getValue());
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        servicesFound.set(true);
        subscriptionRequests.drainTo(bleSafeRequests);

        if (bleSafeRequests.isEmpty()) {
            return;
        }

        SubscriptionRequest request = bleSafeRequests.pop();

        if (request != null) {
            subscribe(request.getCallback(), request.getServiceId(), request.getCharacteristicId());
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        subscriptionRequests.drainTo(bleSafeRequests);

        if (bleSafeRequests.isEmpty()) {
            return;
        }

        SubscriptionRequest request = bleSafeRequests.pop();

        if (request != null) {
            subscribe(request.getCallback(), request.getServiceId(), request.getCharacteristicId());
        }
    }

    public void subscribe(OnNotification callback, UUID serviceId, UUID characteristicId)
            throws IllegalArgumentException {
        if (!servicesFound.get()) {
            subscriptionRequests.add(new SubscriptionRequest(callback, serviceId, characteristicId));
            return;
        }

        BluetoothGattService service = gatt.getService(serviceId);
        if (service == null) {
            gatt.disconnect();
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicId);
        if (characteristic == null) {
            gatt.disconnect();
            return;
        }

        if (characteristic.getDescriptors().size() == 0) {
            gatt.disconnect();
            return;
        }

        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptors().get(0);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);

        Subscription subscription = Subscription.of(characteristic);
        subscriptions.put(subscription, callback);
    }

    public interface OnNotification {
        void onNotification(byte[] data);
    }

    private static class SubscriptionRequest {
        private final OnNotification callback;
        private final UUID serviceId;
        private final UUID characteristicId;

        public SubscriptionRequest(OnNotification callback, UUID serviceId, UUID characteristicId) {
            this.callback = callback;
            this.serviceId = serviceId;
            this.characteristicId = characteristicId;
        }

        public OnNotification getCallback() {
            return callback;
        }

        public UUID getServiceId() {
            return serviceId;
        }

        public UUID getCharacteristicId() {
            return characteristicId;
        }
    }

    private static class Subscription {
        private final UUID serviceId;
        private final UUID characteristicId;

        public Subscription(UUID serviceId, UUID characteristicId) {
            this.serviceId = serviceId;
            this.characteristicId = characteristicId;
        }

        public static Subscription of(BluetoothGattCharacteristic characteristic) {
            return new Subscription(characteristic.getService().getUuid(),
                    characteristic.getUuid());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Subscription that = (Subscription) o;

            if (serviceId != null ? !serviceId.equals(that.serviceId) : that.serviceId != null)
                return false;
            return characteristicId != null ? characteristicId.equals(that.characteristicId) : that.characteristicId == null;
        }

        @Override
        public int hashCode() {
            int result = serviceId != null ? serviceId.hashCode() : 0;
            result = 31 * result + (characteristicId != null ? characteristicId.hashCode() : 0);
            return result;
        }
    }
}
