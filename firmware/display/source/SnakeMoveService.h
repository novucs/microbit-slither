#ifndef DISPLAY_SNAKEMOVESERVICE_H
#define DISPLAY_SNAKEMOVESERVICE_H

#include "MicroBitConfig.h"
#include "ble/BLE.h"
#include "MicroBitAccelerometer.h"
#include "EventModel.h"

namespace slither {

    extern const uint8_t SnakeMoveServiceUUID[];
    extern const uint8_t SnakeMoveServiceDataUUID[];

    class SnakeMoveService {
    private:

        BLEDevice &ble;
        uint16_t movementBuffer[2];
        GattAttribute::Handle_t moveCharacteristicHandle;

    public:

        explicit SnakeMoveService(BLEDevice &ble);

        void initialize();

        void sendMove(uint16_t x, uint16_t y);
    };
}

#endif //DISPLAY_SNAKEMOVESERVICE_H
