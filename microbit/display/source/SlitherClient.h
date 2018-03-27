#ifndef DISPLAY_SNAKEGAME_H
#define DISPLAY_SNAKEGAME_H

#include <MicroBitEvent.h>
#include <MicroBit.h>
#include "SnakeMoveService.h"

#define MOVEMENT_THRESHOLD 450

namespace slither {

    class SlitherClient {
    private:
        MicroBit *microBit = new MicroBit();
        SnakeMoveService *moveService = nullptr; // Initialized on run.
        int16_t previousX = 1;
        int16_t previousY = 1;
        bool connected = false;
        uint64_t nextAllowedMove = 0;

        void onAccelerometerChange(MicroBitEvent event);

    public:

        virtual ~SlitherClient();

        void run();

        void onConnected(MicroBitEvent);

        void onDisconnected(MicroBitEvent);

        void onButtonBDown(MicroBitEvent);

        void onButtonBUp(MicroBitEvent);
    };

}

#endif //DISPLAY_SNAKEGAME_H
