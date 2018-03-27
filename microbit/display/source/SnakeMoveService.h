#ifndef DISPLAY_SNAKEMOVESERVICE_H
#define DISPLAY_SNAKEMOVESERVICE_H

#include "MicroBitConfig.h"
#include "ble/BLE.h"
#include "MicroBitAccelerometer.h"
#include "EventModel.h"

namespace slither {

    extern const uint8_t SnakeMoveServiceUUID[];
    extern const uint8_t SnakeMoveDirectionUUID[];
    extern const uint8_t SnakeMoveSpeedUUID[];

    class SnakeMoveService {
    private:

        BLEDevice &ble;
        uint8_t directionBuffer[2];
        uint8_t speedBuffer[1];
        GattAttribute::Handle_t directionHandle;
        GattAttribute::Handle_t speedHandle;

    public:

        explicit SnakeMoveService(BLEDevice &ble);

        void initialize();

        void sendDirection(uint8_t x, uint8_t y);

        void sendSpeed(uint8_t speed);
    };
}

#endif //DISPLAY_SNAKEMOVESERVICE_H
