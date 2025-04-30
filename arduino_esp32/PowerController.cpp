#include "Common.h"
#include "PowerController.h"

PowerController::PowerController()
    : cumulatedPower(0.0),
      safetyBrakeValue(-0.1),
      feedbackMultiplier(1.0),
      feedbackBrakeMultiplier(1.0),
      maxPower(0) {}

  // Method to compute power based on pedal input

float PowerController::empower(float throttle){
  cumulatedPower = target(throttle, cumulatedPower, feedbackMultiplier);
  cumulatedPower = cap(cumulatedPower, 0.0, maxPower);
  return cumulatedPower;
}

float PowerController::brake(float magnitude){
  cumulatedPower = target(-magnitude, cumulatedPower, feedbackMultiplier);
  if(cumulatedPower<0){
    cumulatedPower = 0.0;
  }
  return cumulatedPower;
}

float PowerController::engineBrake(){
  cumulatedPower = target(safetyBrakeValue, cumulatedPower, feedbackBrakeMultiplier);
  if(cumulatedPower<0){
    cumulatedPower = 0.0;
  }
  return cumulatedPower;
}

void PowerController::setPowerRating(float _feedbackMultiplier, float _feedbackBrakeMultiplier, float _maxPower){
    feedbackMultiplier = _feedbackMultiplier;
    feedbackBrakeMultiplier = _feedbackBrakeMultiplier;
    maxPower = _maxPower;
}
void PowerController::resetCumulatedPower(){
  cumulatedPower = 0.0;
}
