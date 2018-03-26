package net.novucs.slither;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.widget.ImageView;

import static net.novucs.slither.BLEAttributes.ACCELEROMETER_DATA_CHARACTERISTIC;
import static net.novucs.slither.BLEAttributes.ACCELEROMETER_DATA_CHARACTERISTIC_NOTIFY;
import static net.novucs.slither.BLEAttributes.ACCELEROMETER_SERVICE;

public class PlayerConnection extends BluetoothGattCallback {

    private final MainActivity activity;
    private final BluetoothDevice device;
    private final ImageView image;
    private BluetoothGatt gatt;
    private double[] previousRotation = null;

    public PlayerConnection(MainActivity activity, BluetoothDevice device, ImageView image) {
        this.activity = activity;
        this.device = device;
        this.image = image;
    }

    public void connect() {
        gatt = device.connectGatt(activity, false, this);
    }

    private double[] lowPass(double[] a, double[] b) {
        if (b == null) {
            return a;
        }

        for (int i = 0; i < a.length; i++) {
            b[i] += 0.1 * (a[i] - b[i]);
        }

        return b;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            this.gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            System.out.println("Disconnected from the GATT server");
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        double x = ((data[1] << 8) + data[0]) / 1000f;
        double y = ((data[3] << 8) + data[2]) / 1000f;
        double z = ((data[5] << 8) + data[4]) / 1000f;
        previousRotation = lowPass(new double[]{x, y, z}, previousRotation);
        x = previousRotation[0];
        y = previousRotation[1];
        z = previousRotation[2];

        double radian = 180 / Math.PI;
        double pitch = Math.atan(x / Math.sqrt(Math.pow(y, 2) + Math.pow(z, 2))) * radian;
        double roll = -Math.atan(y / Math.sqrt(Math.pow(x, 2) + Math.pow(z, 2))) * radian;

        image.setRotationX((float) roll);
        image.setRotationY((float) pitch);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return;
        }

        BluetoothGattCharacteristic characteristic = gatt
                .getService(ACCELEROMETER_SERVICE)
                .getCharacteristic(ACCELEROMETER_DATA_CHARACTERISTIC);
        gatt.setCharacteristicNotification(characteristic, true);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(ACCELEROMETER_DATA_CHARACTERISTIC_NOTIFY);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }
}
