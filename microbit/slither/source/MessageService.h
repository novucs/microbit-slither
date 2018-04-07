#ifndef MESSAGE_SERVICE_H
#define MESSAGE_SERVICE_H

#include <MicroBit.h>
#include <vector>
#include <functional>

namespace slither {
    // Service and characteristic UUIDs for the slither messaging BLE service.
    extern const uint8_t MessageServiceUUID[];
    extern const uint8_t MessageRxCharacteristicUUID[];
    extern const uint8_t MessageTxCharacteristicUUID[];

    /**
     * Message callback wrapper. Wraps gatt write callbacks and parses the
     * message to hand to another callback.
     * @tparam T the message callback handling class.
     */
    template<typename T>
    class MessageCallbackWrapper {
    private:
        T *object;
        void (T::*handler)(ManagedString);

    public:
        /**
         * Constructs a new message callback wrapper.
         * @param object the object that holds the real callback method.
         * @param handler the handling function to give the message.
         */
        MessageCallbackWrapper(T *object, void (T::*handler)(ManagedString)) : object(object), handler(handler) {}

        /**
         * Handles when gatt has been written to by the BLE central device.
         * Relays this write to the registered handler.
         * @param params what has been written.
         */
        void call(const GattWriteCallbackParams *params) {
            ManagedString message((char *) params->data, params->len);
            (object->*handler)(message);
        }
    };

    /**
     * Messaging service for the slither game. Any messages sent by the app are
     * handled through this service.
     */
    class MessageService {
    private:
        BLEDevice &ble;
        uint8_t buffer[128];
        const GattServer::DataWrittenCallback_t *callback = nullptr;
        const void *wrapper = nullptr;

    public:
        /**
         * Constructs a new message service.
         * @param ble the ble device to register this service to.
         */
        explicit MessageService(BLEDevice &ble);

        /**
         * Registers this messaging service to bluetooth.
         */
        void initialize();

        /**
         * Stops listening to this messaging service.
         */
        void defen();

        /**
         * Listens to all messages sent by the central device. Does nothing if
         * the service is already being listened to.
         * @tparam T the class holding the handler.
         * @param object the callback object.
         * @param handler the function to execute on the provided callback.
         */
        template<typename T>
        void listen(T *object, void (T::*handler)(ManagedString)) {
            if (callback != nullptr) return;

            auto *wrapper = new MessageCallbackWrapper<T>(object, handler);
            callback = new GattServer::DataWrittenCallback_t(wrapper, &MessageCallbackWrapper<T>::call);
            ble.gattServer().onDataWritten(*callback);
            this->wrapper = wrapper;
        }
    };
}

#endif
