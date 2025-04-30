#ifndef COMMON_H
#define COMMON_H
#include <Arduino.h>

float interpolate(float from, float fromMin, float fromMax, float toMin, float toMax);

float target(float target, float from, float feedback);

float interpolateWithOffset(float defaultValue, float from, float fromThreshold, float fromMax, float toMin, float toMax);

float cap(float target, float min, float max);

int breakSegment(int from, int to, int input, int segmentCount);

float median(float from, float fromMin, float fromMax, float middleZeroRange);

float scalarXY(float x, float y, float xToYRatio);

int direction(float v);


#endif