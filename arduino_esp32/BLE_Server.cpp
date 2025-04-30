#include "BLE_Server.h"
#include <Arduino.h> // Include Arduino core library for String support

BLE_Server::BLE_Server(const String& deviceName, const String& serviceUUID, const String& characteristicUUID)
    : deviceName(deviceName), serviceUUID(serviceUUID), characteristicUUID(characteristicUUID),
      pServer(nullptr), pService(nullptr), pCharacteristic(nullptr),
      deviceConnected(false), characteristicCallback(nullptr), connectionCallback(nullptr) {}

void BLE_Server::start() {
    // Initialize BLE
    BLEDevice::init(deviceName);
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks(this));

    // Create the BLE service
    pService = pServer->createService(serviceUUID);

    // Create the BLE characteristic
    pCharacteristic = pService->createCharacteristic(
        characteristicUUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_WRITE |
        BLECharacteristic::PROPERTY_NOTIFY
    );

    pCharacteristic->setCallbacks(new MyCharacteristicCallbacks(this));
    pCharacteristic->addDescriptor(new BLE2902());

    // Start the service
    pService->start();

    // Start advertising
    pServer->getAdvertising()->start();
}

bool BLE_Server::isDeviceConnected() {
    return deviceConnected;
}

void BLE_Server::setCharacteristicCallback(CharacteristicCallback callback) {
    characteristicCallback = callback;
}

void BLE_Server::setConnectionCallback(ConnectionCallback callback) {
    connectionCallback = callback;
}

void BLE_Server::sendNotification(const String& message) {
    if (deviceConnected) {
        pCharacteristic->setValue(message);
        pCharacteristic->notify();
    }
}

// Server callbacks
void BLE_Server::MyServerCallbacks::onConnect(BLEServer* pServer) {
    parent->deviceConnected = true;
    if (parent->connectionCallback) {
        parent->connectionCallback(true);
    }
}

void BLE_Server::MyServerCallbacks::onDisconnect(BLEServer* pServer) {
    parent->deviceConnected = false;
    if (parent->connectionCallback) {
        parent->connectionCallback(false);
    }
    // Restart advertising to allow reconnection
    pServer->startAdvertising();
}

// Characteristic callbacks
void BLE_Server::MyCharacteristicCallbacks::onWrite(BLECharacteristic* pCharacteristic) {
    String value = pCharacteristic->getValue();
    if (value.length() > 0 && parent->characteristicCallback) {
        parent->characteristicCallback(value);
    }
}
