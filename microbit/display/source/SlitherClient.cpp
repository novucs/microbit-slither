#include "SlitherClient.h"

namespace slither {

    void SlitherClient::run() {
        // Initialize the micro:bit.
        microBit->init();

        // Create and initialize the bluetooth services.
        moveService = new SnakeMoveService(*microBit->ble);
        moveService->initialize();
        new MicroBitAccelerometerService(*microBit->ble, microBit->accelerometer);

        // Register event listeners.
        microBit->messageBus.listen(MICROBIT_ID_BLE, MICROBIT_BLE_EVT_CONNECTED, this, &SlitherClient::onConnected);
        microBit->messageBus.listen(MICROBIT_ID_BLE, MICROBIT_BLE_EVT_DISCONNECTED, this,
                                    &SlitherClient::onDisconnected);
        microBit->messageBus.listen(MICROBIT_ID_ACCELEROMETER, MICROBIT_ACCELEROMETER_EVT_DATA_UPDATE, this,
                                    &SlitherClient::onAccelerometerChange);
    }

    void SlitherClient::onConnected(MicroBitEvent) {
        microBit->display.scroll("C");
        connected = true;
    }

    void SlitherClient::onDisconnected(MicroBitEvent) {
        microBit->display.scroll("D");
        connected = false;
    }

    void SlitherClient::onAccelerometerChange(MicroBitEvent) {
        int16_t x = 0;
        int16_t y = 0;
        int accelerometerX = microBit->accelerometer.getX();
        int accelerometerY = microBit->accelerometer.getY();

        if (accelerometerX > MOVEMENT_THRESHOLD) {
            x = 1;
        } else if (accelerometerX < -MOVEMENT_THRESHOLD) {
            x = -1;
        }

        if (accelerometerY > MOVEMENT_THRESHOLD) {
            y = 1;
        } else if (accelerometerY < -MOVEMENT_THRESHOLD) {
            y = -1;
        }

        if (microBit->buttonB.isPressed()) {
            x *= 2;
            y *= 2;
        }

        if ((x == previousX && y == previousY) || (x == 0 && y == 0) || (microBit->systemTime() < nextAllowedMove)) {
            return;
        }

        microBit->display.image.clear();
        microBit->display.image.setPixelValue((uint16_t) (x + 2), (uint16_t) (y + 2), 255);
        previousX = x;
        previousY = y;
        nextAllowedMove = microBit->systemTime() + 50;
        moveService->sendMove((uint16_t) x, (uint16_t) y);
    }

    SlitherClient::~SlitherClient() {
        delete moveService;
    }
}