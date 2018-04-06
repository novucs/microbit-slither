#include "MicroBitConfig.h"
#include "ble/UUID.h"

#include "MessageService.h"

namespace slither {

    MessageService::MessageService(BLEDevice &ble) : ble(ble) {}

    void MessageService::initialize() {
        // Create the characteristic.
        GattCharacteristic messageCharacteristic(MessageTxUUID, buffer, 0, 128,
                                                 GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_WRITE_WITHOUT_RESPONSE |
                                                 GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_WRITE);
        messageCharacteristic.requireSecurity(SecurityManager::MICROBIT_BLE_SECURITY_LEVEL);

        // Create the service.
        GattCharacteristic *characteristics[] = {&messageCharacteristic};
        GattService service(MessageServiceUUID, characteristics,
                            sizeof(characteristics) / sizeof(GattCharacteristic *));

        // Add service to the bluetooth handler.
        ble.addService(service);
    }

    void MessageService::defen() {
        ble.gattServer().onDataWritten().detach(*callback);
        delete callback;
        delete wrapper;
    }

    const uint8_t MessageServiceUUID[] = {
            0xaa, 0xb7, 0x93, 0x44, 0x9a, 0x83, 0x48, 0x86, 0xa1, 0xd3, 0x32, 0xa8, 0x00, 0x25, 0x99, 0x37
    };

    const uint8_t MessageTxUUID[] = {
            0xe4, 0x99, 0x0f, 0x36, 0x28, 0xf4, 0x40, 0xd8, 0xbf, 0xa2, 0xf0, 0x51, 0x18, 0x72, 0x0a, 0x28
    };

    const uint8_t MessageRxUUID[] = {
            0xe4, 0x99, 0x0e, 0x36, 0x28, 0xf4, 0x40, 0xd8, 0xbf, 0xa2, 0xf0, 0x51, 0x18, 0x72, 0x0a, 0x28
    };
}
