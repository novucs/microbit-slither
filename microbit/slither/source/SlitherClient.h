#ifndef DISPLAY_SNAKEGAME_H
#define DISPLAY_SNAKEGAME_H

#include <MicroBitEvent.h>
#include <MicroBit.h>
#include <vector>
#include "MoveService.h"

#define MOVEMENT_THRESHOLD 450

namespace slither {

    /**
     * Main controller for the slither game client. Handles all communication
     * with the central android device.
     */
    class SlitherClient {
    private:
        MicroBit *microBit = new MicroBit();
        MoveService *moveService = nullptr; // Initialized on run.
        int16_t previousX = 1;
        int16_t previousY = 1;
        uint64_t nextAllowedMove = 0;

        /**
         * Handles when the device has been moved.
         */
        void onAccelerometerChange(MicroBitEvent);

        /**
         * Handles when the micro:bit has connected to a central android device.
         */
        void onConnected(MicroBitEvent);

        /**
         * Handles when the micro:bit has disconnected from a central android
         * device.
         */
        void onDisconnected(MicroBitEvent);

        /**
         * Increases the movement speed of the snake by notifying the android
         * device when the B button is pressed down.
         */
        void onButtonBDown(MicroBitEvent);

        /**
         * Decreases the movement speed of the snake by notifying the android
         * device when the B button has been released.
         */
        void onButtonBUp(MicroBitEvent);

        /**
         * Handles when a message has been received from the android device by
         * scrolling the message on the screen.
         * @param message the message received.
         */
        void onMessage(ManagedString message);

    public:

        /**
         * Destructs the slither client, cleaning up compositions.
         */
        virtual ~SlitherClient();

        /**
         * Runs the slither client, registering all services and begins
         * communication with the next found android device.
         */
        void run();
    };

}

#endif //DISPLAY_SNAKEGAME_H
