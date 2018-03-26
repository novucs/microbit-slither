#include "SnakeMoveService.h"

namespace slither {

    SnakeMoveService::SnakeMoveService(BLEDevice &ble) : ble(ble) {}

    void SnakeMoveService::initialize() {
        GattCharacteristic moveCharacteristic(SnakeMoveServiceDataUUID,
                                              (uint8_t *) movementBuffer, 0,
                                              sizeof(movementBuffer),
                                              GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_READ |
                                              GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_NOTIFY);

        movementBuffer[0] = 0;
        movementBuffer[1] = 0;

        moveCharacteristic.requireSecurity(SecurityManager::MICROBIT_BLE_SECURITY_LEVEL);

        GattCharacteristic *characteristics[] = {&moveCharacteristic};
        GattService service(SnakeMoveServiceUUID, characteristics, sizeof(characteristics) / sizeof(GattCharacteristic *));

        ble.addService(service);
        moveCharacteristicHandle = moveCharacteristic.getValueHandle();
        ble.gattServer().write(moveCharacteristicHandle, (uint8_t *) movementBuffer, sizeof(movementBuffer));
    }

    void SnakeMoveService::sendMove(uint16_t x, uint16_t y) {
        if (ble.getGapState().connected) {
            movementBuffer[0] = x;
            movementBuffer[1] = y;

            ble.gattServer().notify(moveCharacteristicHandle, (uint8_t *) movementBuffer, sizeof(movementBuffer));
        }
    }

    const uint8_t SnakeMoveServiceUUID[] = {
            0xaa, 0xb7, 0x93, 0x43, 0x9a, 0x83, 0x48, 0x86, 0xa1, 0xd3, 0x32, 0xa8, 0x00, 0x25, 0x99, 0x37
    };

    const uint8_t SnakeMoveServiceDataUUID[] = {
            0xe4, 0x99, 0x0f, 0x35, 0x28, 0xf4, 0x40, 0xd8, 0xbf, 0xa2, 0xf0, 0x51, 0x18, 0x72, 0x0a, 0x28
    };

}
