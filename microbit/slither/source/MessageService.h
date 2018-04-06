#ifndef MESSAGE_SERVICE_H
#define MESSAGE_SERVICE_H

#include <MicroBit.h>
#include <vector>
#include <functional>

namespace slither {
    extern const uint8_t MessageServiceUUID[];
    extern const uint8_t MessageRxUUID[];
    extern const uint8_t MessageTxUUID[];

    template<typename T>
    class MessageCallbackWrapper {
    private:
        T *object;
        void (T::*handler)(ManagedString);

    public:
        MessageCallbackWrapper(T *object, void (T::*handler)(ManagedString)) : object(object), handler(handler) {}

        void call(const GattWriteCallbackParams *params) {
            ManagedString message((char *) params->data, params->len);
            (object->*handler)(message);
        }
    };

    class MessageService {
    private:
        BLEDevice &ble;
        uint8_t buffer[128];
        const GattServer::DataWrittenCallback_t *callback = nullptr;
        const void *wrapper = nullptr;

    public:
        explicit MessageService(BLEDevice &ble);

        void initialize();

        void defen();

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
