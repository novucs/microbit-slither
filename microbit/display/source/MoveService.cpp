#include "MoveService.h"

namespace slither {

    MoveService::MoveService(BLEDevice &ble) : ble(ble) {}

    void MoveService::initialize() {
        // Create the speed and direction characteristics.
        GattCharacteristic directionCharacteristic(MoveCharacteristicDirectionUUID,
                                                   (uint8_t *) directionBuffer, 0,
                                                   sizeof(directionBuffer),
                                                   GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_READ |
                                                   GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_NOTIFY);
        GattCharacteristic speedCharacteristic(MoveCharacteristicSpeedUUID,
                                               (uint8_t *) speedBuffer, 0,
                                               sizeof(speedBuffer),
                                               GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_READ |
                                               GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_NOTIFY);

        directionBuffer[0] = 0;
        directionBuffer[1] = 0;
        speedBuffer[0] = 0;

        // Ensure all messages are encrypted and preventing
        // man-in-the-middle attacks.
        directionCharacteristic.requireSecurity(SecurityManager::MICROBIT_BLE_SECURITY_LEVEL);
        speedCharacteristic.requireSecurity(SecurityManager::MICROBIT_BLE_SECURITY_LEVEL);

        // Create and add this service to the micro:bit GATT table.
        GattCharacteristic *characteristics[] = {&directionCharacteristic, &speedCharacteristic};
        GattService service(MoveServiceUUID, characteristics,
                            sizeof(characteristics) / sizeof(GattCharacteristic *));
        ble.addService(service);

        // Set the direction and speed replies for read requests.
        directionHandle = directionCharacteristic.getValueHandle();
        ble.gattServer().write(directionHandle, (uint8_t *) directionBuffer, sizeof(directionBuffer));

        speedHandle = speedCharacteristic.getValueHandle();
        ble.gattServer().write(speedHandle, (uint8_t *) speedBuffer, sizeof(directionBuffer));
    }

    void MoveService::sendDirection(uint8_t x, uint8_t y) {
        if (!ble.getGapState().connected) {
            return;
        }

        directionBuffer[0] = x;
        directionBuffer[1] = y;

        ble.gattServer().notify(directionHandle, (uint8_t *) directionBuffer, sizeof(directionBuffer));
    }

    void MoveService::sendSpeed(uint8_t speed) {
        if (!ble.getGapState().connected) {
            return;
        }

        speedBuffer[0] = speed;
        ble.gattServer().notify(speedHandle, (uint8_t *) speedBuffer, sizeof(speedBuffer));
    }

    const uint8_t MoveServiceUUID[] = {
            0xaa, 0xb7, 0x93, 0x43, 0x9a, 0x83, 0x48, 0x86, 0xa1, 0xd3, 0x32, 0xa8, 0x00, 0x25, 0x99, 0x37
    };

    const uint8_t MoveCharacteristicDirectionUUID[] = {
            0xe4, 0x99, 0x0f, 0x35, 0x28, 0xf4, 0x40, 0xd8, 0xbf, 0xa2, 0xf0, 0x51, 0x18, 0x72, 0x0a, 0x28
    };

    const uint8_t MoveCharacteristicSpeedUUID[] = {
            0xe4, 0x99, 0x0e, 0x35, 0x28, 0xf4, 0x40, 0xd8, 0xbf, 0xa2, 0xf0, 0x51, 0x18, 0x72, 0x0a, 0x28
    };

}
