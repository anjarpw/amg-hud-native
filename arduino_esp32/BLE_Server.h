#ifndef BLE_SERVER_H
#define BLE_SERVER_H

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

class BLE_Server {
public:
    // Type definitions for function pointers
    typedef void (*CharacteristicCallback)(const String&);
    typedef void (*ConnectionCallback)(bool);

    // Constructor to initialize the BLE server with custom name and UUIDs
    BLE_Server(const String& deviceName, const String& serviceUUID, const String& characteristicUUID);

    // Start the BLE server
    void start();

    // Check if a device is connected
    bool isDeviceConnected();

    // Set a callback for when the characteristic is written to
    void setCharacteristicCallback(CharacteristicCallback callback);

    // Set a callback for connection state changes
    void setConnectionCallback(ConnectionCallback callback);

    // Send a notification to the connected device
    void sendNotification(const String& message);

private:
    String deviceName;
    String serviceUUID;
    String characteristicUUID;

    BLEServer* pServer;
    BLEService* pService;
    BLECharacteristic* pCharacteristic;

    bool deviceConnected;

    // Callback for handling characteristic writes
    CharacteristicCallback characteristicCallback;

    // Callback for handling connection state changes
    ConnectionCallback connectionCallback;

    // Internal BLE server callbacks
    class MyServerCallbacks : public BLEServerCallbacks {
    public:
        MyServerCallbacks(BLE_Server* parent) : parent(parent) {}
        void onConnect(BLEServer* pServer) override;
        void onDisconnect(BLEServer* pServer) override;

    private:
        BLE_Server* parent;
    };

    // Internal characteristic callbacks
    class MyCharacteristicCallbacks : public BLECharacteristicCallbacks {
    public:
        MyCharacteristicCallbacks(BLE_Server* parent) : parent(parent) {}
        void onWrite(BLECharacteristic* pCharacteristic) override;

    private:
        BLE_Server* parent;
    };
};

#endif
