#ifndef DISPLAY_SNAKEMOVESERVICE_H
#define DISPLAY_SNAKEMOVESERVICE_H

#include "MicroBitConfig.h"
#include "ble/BLE.h"
#include "MicroBitAccelerometer.h"
#include "EventModel.h"

namespace slither {
    // Service and characteristic UUIDs for the slither movement BLE service.
    extern const uint8_t MoveServiceUUID[];
    extern const uint8_t MoveDirectionCharacteristicUUID[];
    extern const uint8_t MoveSpeedCharacteristicUUID[];

    class MoveService {
    private:

        BLEDevice &ble;
        uint8_t directionBuffer[2];
        uint8_t speedBuffer[1];
        GattAttribute::Handle_t directionHandle;
        GattAttribute::Handle_t speedHandle;

    public:

        /**
         * Constructs a new movement service.
         * @param ble the ble device to register this service to.
         */
        explicit MoveService(BLEDevice &ble);

        /**
         * Registers this movement service to bluetooth.
         */
        void initialize();

        /**
         * Notifies the central device with an updated movement vector.
         * @param x the direction X to move.
         * @param y the direction Y to move.
         */
        void sendDirection(uint8_t x, uint8_t y);

        /**
         * Notifies the central device with an updated movement speed.
         * @param speed the new speed the snake should be moving at.
         *              Higher is faster.
         */
        void sendSpeed(uint8_t speed);
    };
}

#endif //DISPLAY_SNAKEMOVESERVICE_H
