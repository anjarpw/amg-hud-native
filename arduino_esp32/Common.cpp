#include "Common.h"
#include <math.h>

float interpolate(float from, float fromMin, float fromMax, float toMin, float toMax) {
  from = cap(from, fromMin, fromMax);
  float val = (from - fromMin) / (fromMax - fromMin);
  return val*(toMax - toMin) + toMin;
}

float target(float target, float from, float feedback) {
  return from + (target - from) / feedback;
}

float interpolateWithOffset(float defaultValue, float from, float fromThreshold, float fromMax, float toMin, float toMax){
  if(from < fromThreshold){
      return defaultValue;
  }
  return interpolate(from, fromThreshold, fromMax, toMin, toMax);
}

float cap(float target, float min, float max) {
  if (target > max) {
    target = max;
  }
  if (target < min) {
    target = min;
  }
  return target;
}

int breakSegment(int from, int to, int input, int segmentCount){
    float v = 1.0*(segmentCount-1)*(input-from)/(to-from);
    return round(v);
}
float median(float from, float fromMin, float fromMax, float middleZeroRange){
  float val = interpolate(from, fromMin, fromMax, -1.0, 1.0);
  if(abs(val) < middleZeroRange){
    return 0;
  }
  if(val > 0){
    return interpolate(val, middleZeroRange, 1.0, 0.0, 1.0);
  }else{
    return interpolate(val,  -1.0, -middleZeroRange, -1.0, 0.0);
  }
}

float scalarXY(float x, float y, float xToYRatio){
  float normY = y/xToYRatio;
  return sqrt((x*x)+(normY*normY));
}

int direction(float v){
  if(v > 0){
    return 1;
  }else if (v < 0){
    return -1;
  }else{
    return 0;
  }
}

