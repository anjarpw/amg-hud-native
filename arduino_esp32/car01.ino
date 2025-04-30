#include <dummy.h>
#include "Common.h"
#include "PowerController.h"
#include "BLE_Server.h"
#include "KVManager.h"

KVManager kvManager;

const int joystickYPin = 36;
const int joystickXPin = 39;
const int throttlePedalPin = 34;  // Analog ikvManagernput pin
const int brakePedalPin = 35;
const int modeSelectorPin = 32;
const int steerRackPin = 33;
const int leftMotorPinA = 22;  // Pin to output PWM
const int leftMotorPinB = 23;
const int rightMotorPinA = 18;  // Pin to output PWM
const int rightMotorPinB = 19;
const int motorEnablePin = 21;


// Define your custom device name, service UUID, and characteristic UUID
const String DEVICE_NAME = "ESP32_BLE_AMG";
const String SERVICE_UUID = "5fafc201-1fb5-459e-8fcc-c5c9c331914C";
const String CHARACTERISTIC_UUID = "ceb5483e-36e1-4688-b7f5-ea07361b26af";

// Create an instance of the BLE_Server class
BLE_Server bleServer(DEVICE_NAME, SERVICE_UUID, CHARACTERISTIC_UUID);


struct Differential {
  float left;
  float right;
};

void setLeftMotorSpeed(int direction, int dutyCycle) {
  if (direction == 1) {
    analogWrite(leftMotorPinA, 0);
    analogWrite(leftMotorPinB, dutyCycle);  // Output PWM signal
    kvManager.set("LEFT_MOTOR", String(dutyCycle));
  } else if (direction == -1) {
    analogWrite(leftMotorPinB, 0);
    analogWrite(leftMotorPinA, dutyCycle);  // Output PWM signal
    kvManager.set("LEFT_MOTOR", String(-dutyCycle));
  } else {
    stopLeftMotor();
  }
}

void stopLeftMotor() {
  analogWrite(leftMotorPinA, 0);
  analogWrite(leftMotorPinB, 0);
  kvManager.set("LEFT_MOTOR", "0");
}

void setRightMotorSpeed(int direction, int dutyCycle) {
  if (direction == 1) {
    analogWrite(rightMotorPinA, 0);
    analogWrite(rightMotorPinB, dutyCycle);  // Output PWM signal
    kvManager.set("RIGHT_MOTOR", String(dutyCycle));
  } else if (direction == -1) {
    analogWrite(rightMotorPinB, 0);
    analogWrite(rightMotorPinA, dutyCycle);  // Output PWM signal
    kvManager.set("RIGHT_MOTOR", String(-dutyCycle));
  } else {
    stopRightMotor();
  }
}

void stopRightMotor() {
  analogWrite(rightMotorPinA, 0);
  analogWrite(rightMotorPinB, 0);
  kvManager.set("RIGHT_MOTOR", "0");
}





String getMode() {
  int analogValue = analogRead(modeSelectorPin);
  int val = breakSegment(0, 1024, analogValue, 6);
  //  String modes[] = { "S+", "S", "D", "R", "P", "T" };
  String modes[] = { "T", "P", "R", "D", "S", "S+" };
  return modes[val];
}

PowerController powerController;  // A0 as analogPin, maxPower = 100.0
String prevMode = "P";
String mode = "P";

const int minDutyCycle = 50;
const float minActivePower = 0.05;

float innerDiffRatio = 1.0;
float outerDiffRatio = 1.0;
float directionReference = 0.0;

int isThrottling = false;
String engineMode = "FIRM";

void setMotorEnable(int leftDutyCycle, int rightDutyCycle) {
  int dutyCycle = 0;
  if (engineMode == "FIRM") {
    analogWrite(motorEnablePin, 255);
    dutyCycle = 255;
  } else if (engineMode == "LOOSE") {
    dutyCycle = max(leftDutyCycle, rightDutyCycle);
    if (dutyCycle <= minDutyCycle) {
      dutyCycle = 0;
    }
  }
  kvManager.set("ENGINE_MODE", engineMode);
  kvManager.set("MOTOR_ENABLE", String(dutyCycle));
}

float computeThrottleFromPedal() {
  int analogThrottle = analogRead(throttlePedalPin);  // Read the analog value (0-1023)
  float throttle = interpolateWithOffset(0, analogThrottle, 300.0, 500.0, 0, 1.0);
  int analogBrake = analogRead(brakePedalPin);  // Read the analog value (0-1023)
  float brake = interpolateWithOffset(0, analogBrake, 300.0, 500.0, 0, 1.0);
  kvManager.set("ANALOG_BRAKE", String(analogBrake));

  kvManager.set("ANALOG_THROTTLE", String(analogThrottle));


  bool isBraking = brake > 0;

  isThrottling = throttle > 0;
  if (isBraking) {
    engineMode = "FIRM";
    return powerController.brake(brake);
  } else if (isThrottling) {
    engineMode = "FIRM";
    return powerController.empower(throttle);
  } else {
    engineMode = "LOOSE";
    return powerController.engineBrake();
  }
}

float computeTurningFromSteerRack() {
  int analogSteerRack = analogRead(steerRackPin);  // Read the analog value (0-1023)
  kvManager.set("ANALOG_STEER", String(analogSteerRack));
  float steer = -median(analogSteerRack, 0, 1024, 0.2);
  return steer;
}
float computeThrottleFromJoystick() {
  int yAxisJoystick = analogRead(joystickYPin);  // Read the analog value (0-1023)
  int xAxisJoystick = analogRead(joystickXPin);  // Read the analog value (0-1023)
  float throttle = scalarXY(1.0 * xAxisJoystick / 512 - 1.0, 1.0 * yAxisJoystick / 512 - 1.0, 2.0);
  isThrottling = throttle > 0.3;
  directionReference = median(xAxisJoystick, 0, 1023, 0.3);
  kvManager.set("ANALOG_JOY_X", String(xAxisJoystick));
  kvManager.set("ANALOG_JOY_Y", String(yAxisJoystick));

  if (isThrottling) {
    return powerController.empower(throttle);
  } else {
    return powerController.engineBrake();
  }
}
float computeTurningFromJoystick() {
  int xAxisJoystick = analogRead(joystickXPin);  // Read the analog value (0-1023)
  float steer = median(xAxisJoystick, 0, 1023, 0.2);
  return steer;
}



void resetIfModeChanged(String mode) {
  if (prevMode != mode) {
    powerController.resetCumulatedPower();
  }
}
void resetIfNotForwardDriving(String mode) {
  if (prevMode != "D" && prevMode != "S" && prevMode != "S+") {
    powerController.resetCumulatedPower();
  }
}

void setDiffRatio(float ref, float inner, float outer) {
  directionReference = ref;
  innerDiffRatio = inner;
  outerDiffRatio = outer;
}

Differential diffCumulatedPower(float turningLevel, float cumulatedPower) {
  Differential diff;
  diff.left = cumulatedPower;
  diff.right = cumulatedPower;
  if (turningLevel == 0.0) {
    diff;
  }
  float inner = directionReference + abs(turningLevel) * (innerDiffRatio - directionReference);
  float outer = directionReference + abs(turningLevel) * (outerDiffRatio - directionReference);
  if (turningLevel < 0) {
    diff.left = cumulatedPower * inner;
    diff.right = cumulatedPower * outer;
  } else {
    diff.right = cumulatedPower * inner;
    diff.left = cumulatedPower * outer;
  }
  return diff;
}

int leftDutyCycle = 0;
int rightDutyCycle = 0;

void sendBluetoothSignal() {
  auto pairs = kvManager.getDirtyPairs();
  for (const auto& pair : pairs) {
    Serial.println(pair.key + ": " + pair.value);
  }
  if (bleServer.isDeviceConnected()) {
    for (const auto& pair : pairs) {
      bleServer.sendNotification(pair.key + "=" + pair.value + "\n");
    }
  }
}



void setup() {
  esp_log_level_set("*", ESP_LOG_NONE);
  pinMode(leftMotorPinA, OUTPUT);  // Set pin as an output
  pinMode(leftMotorPinB, OUTPUT);
  pinMode(rightMotorPinA, OUTPUT);  // Set pin as an output
  pinMode(rightMotorPinB, OUTPUT);
  pinMode(motorEnablePin, OUTPUT);
  pinMode(joystickYPin, INPUT);
  pinMode(joystickXPin, INPUT);
  pinMode(throttlePedalPin, INPUT);
  pinMode(brakePedalPin, INPUT);
  pinMode(modeSelectorPin, INPUT);
  analogWriteResolution(leftMotorPinA, 8);
  analogWriteResolution(leftMotorPinB, 8);
  analogWriteResolution(rightMotorPinA, 8);
  analogWriteResolution(rightMotorPinB, 8);
  analogReadResolution(10);
  Serial.begin(115200);


  bleServer.setCharacteristicCallback([](const String& value) {
    Serial.print("Received value: ");
    Serial.println(value);
    bleServer.sendNotification("PING=" + value + "\n");
  });

  // Set a callback for connection state changes
  bleServer.setConnectionCallback([](bool isConnected) {
    if (isConnected) {
      Serial.println("Device connected");
      kvManager.set("MODE", mode);
      kvManager.markDirty("MODE");
    } else {
      Serial.println("Device disconnected");
    }
  });

  bleServer.start();
}
int n = 0;
void loop() {
  mode = getMode();
  kvManager.set("MODE", mode);
  if (n % 10 == 0) {
    kvManager.markDirty("MODE");
    n = 0;
  }
  n = n + 1;

  float cumulatedPower = 0.0;
  float turningLevel = 0.0;
  if (mode == "P") {
    engineMode = "LOOSE";
    powerController.resetCumulatedPower();
  } else if (mode == "R") {
    resetIfModeChanged(mode);
    powerController.setPowerRating(60.0, 60.0, 0.5);
    setDiffRatio(-1.0, 0.3, -1);
    cumulatedPower = computeThrottleFromPedal();
    turningLevel = computeTurningFromSteerRack();
  } else if (mode == "D") {
    resetIfNotForwardDriving(mode);
    powerController.setPowerRating(60.0, 60.0, 0.5);
    setDiffRatio(1.0, 0.3, 1);
    cumulatedPower = computeThrottleFromPedal();
    turningLevel = computeTurningFromSteerRack();
  } else if (mode == "S") {
    resetIfNotForwardDriving(mode);
    powerController.setPowerRating(30.0, 60.0, 0.75);
    setDiffRatio(1.0, 0.45, 1);
    cumulatedPower = computeThrottleFromPedal();
    turningLevel = computeTurningFromSteerRack();
  } else if (mode == "S+") {
    resetIfNotForwardDriving(mode);
    powerController.setPowerRating(10.0, 60.0, 1.0);
    setDiffRatio(1.0, 0.6, 1.0);
    cumulatedPower = computeThrottleFromPedal();
    turningLevel = computeTurningFromSteerRack();
  } else if (mode == "T") {
    engineMode = "LOOSE";
    resetIfModeChanged(mode);
    powerController.setPowerRating(10.0, 10.0, 0.75);
    setDiffRatio(0.0, -1.0, 1.0);
    cumulatedPower = computeThrottleFromJoystick();
    turningLevel = computeTurningFromJoystick();
  }
  prevMode = mode;

  Differential diff = diffCumulatedPower(turningLevel, cumulatedPower);
  int newLeftDutyCycle = (int)(interpolateWithOffset(0, abs(diff.left), minActivePower, 1.0, minDutyCycle * 1.0, 255.0));
  int newRightDutyCycle = (int)(interpolateWithOffset(0, abs(diff.right), minActivePower, 1.0, minDutyCycle * 1.0, 255.0));
  kvManager.set("TURNING_LEVEL", String(turningLevel, 3));
  kvManager.set("CUMULATED_POWER", String(cumulatedPower, 3));
  kvManager.set("DIFF_LEFT", String(diff.left, 3));
  kvManager.set("DIFF_RIGHT", String(diff.right, 3));

  if (newLeftDutyCycle > 0 && leftDutyCycle == 0) {
    newLeftDutyCycle = minDutyCycle;
  }
  if (newRightDutyCycle > 0 && rightDutyCycle == 0) {
    newRightDutyCycle = minDutyCycle;
  }
  leftDutyCycle = newLeftDutyCycle;
  rightDutyCycle = newRightDutyCycle;


  if (leftDutyCycle == 0) {
    stopLeftMotor();
  } else {
    setLeftMotorSpeed((diff.left >= 0) ? 1 : -1, leftDutyCycle);
  }
  if (rightDutyCycle == 0) {
    stopRightMotor();
  } else {
    setRightMotorSpeed((diff.right >= 0) ? 1 : -1, rightDutyCycle);
  }
  setMotorEnable(leftDutyCycle, rightDutyCycle);


  sendBluetoothSignal();
  delay(50);  // Delay for readability
}