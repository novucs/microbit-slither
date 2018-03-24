#include "MicroBit.h"
#include "MicroBitUARTService.h"

MicroBit uBit;
MicroBitUARTService *uart;

int connected = 0;

void onConnected(MicroBitEvent) {

    uBit.display.scroll("C");

    connected = 1;

    // mobile app will send ASCII strings terminated with the colon character
    ManagedString eom(":");

    while (connected == 1) {
        ManagedString msg = uart->readUntil(eom);
        uBit.display.scroll(msg);
    }
}

void onDisconnected(MicroBitEvent) {
    uBit.display.scroll("D");
    connected = 0;
}

void onButtonA(MicroBitEvent) {
    if (connected == 0) {
        return;
    }
    uart->send(ManagedString("YES"));
    uBit.display.scroll("Y");
}

void onButtonB(MicroBitEvent) {
    if (connected == 0) {
        return;
    }
    uart->send(ManagedString("NO"));
    uBit.display.scroll("N");
}

void onButtonAB(MicroBitEvent) {
    if (connected == 0) {
        return;
    }
    uart->send(ManagedString("GOT IT!!"));
    uBit.display.scroll("!");
}

int main() {
    // Initialise the micro:bit runtime.
    uBit.init();

    uBit.messageBus.listen(MICROBIT_ID_BLE, MICROBIT_BLE_EVT_CONNECTED, onConnected);
    uBit.messageBus.listen(MICROBIT_ID_BLE, MICROBIT_BLE_EVT_DISCONNECTED, onDisconnected);
    uBit.messageBus.listen(MICROBIT_ID_BUTTON_A, MICROBIT_BUTTON_EVT_CLICK, onButtonA);
    uBit.messageBus.listen(MICROBIT_ID_BUTTON_B, MICROBIT_BUTTON_EVT_CLICK, onButtonB);
    uBit.messageBus.listen(MICROBIT_ID_BUTTON_AB, MICROBIT_BUTTON_EVT_CLICK, onButtonAB);

    new MicroBitAccelerometerService(*uBit.ble, uBit.accelerometer);
    new MicroBitButtonService(*uBit.ble);
    new MicroBitIOPinService(*uBit.ble, uBit.io);
    new MicroBitLEDService(*uBit.ble, uBit.display);
    new MicroBitTemperatureService(*uBit.ble, uBit.thermometer);

    // Note GATT table size increased from default in MicroBitConfig.h
    // #define MICROBIT_SD_GATT_TABLE_SIZE             0x500
    uart = new MicroBitUARTService(*uBit.ble, 32, 32);
    uBit.display.scroll("UART ready");

    // If main exits, there may still be other fibers running or registered event handlers etc.
    // Simply release this fiber, which will mean we enter the scheduler. Worse case, we then
    // sit in the idle task forever, in a power efficient sleep.
    release_fiber();
}
