#ifndef POWER_CONTROLLER_H
#define POWER_CONTROLLER_H

#include <Arduino.h>

class PowerController {
private:
  float cumulatedPower;
  const float safetyBrakeValue;
  float feedbackMultiplier;
  float feedbackBrakeMultiplier;
  float maxPower;

public:
  // Constructor
  PowerController();

  float empower(float throttle);
  float brake(float magnitude);
  float engineBrake();
  // Method to compute power
  void computePowerFromPedal();
  void setPowerRating(float feedbackMultiplier, float feedbackBrakeMultiplier, float maxPower);
  void resetCumulatedPower();
};
#endif