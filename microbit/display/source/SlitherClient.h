#ifndef DISPLAY_SNAKEGAME_H
#define DISPLAY_SNAKEGAME_H

#include <MicroBitEvent.h>
#include <MicroBit.h>
#include <vector>
#include "MoveService.h"

#define MOVEMENT_THRESHOLD 450

namespace slither {

    class SlitherClient {
    private:
        MicroBit *microBit = new MicroBit();
        MoveService *moveService = nullptr; // Initialized on run.
        int16_t previousX = 1;
        int16_t previousY = 1;
        uint64_t nextAllowedMove = 0;

        void onAccelerometerChange(MicroBitEvent event);

    public:

        virtual ~SlitherClient();

        void run();

        void onConnected(MicroBitEvent);

        void onDisconnected(MicroBitEvent);

        void onButtonBDown(MicroBitEvent);

        void onButtonBUp(MicroBitEvent);

        void onMessage(ManagedString message);
    };

}

#endif //DISPLAY_SNAKEGAME_H
