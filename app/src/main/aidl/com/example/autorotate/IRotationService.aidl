package com.example.autorotate;

interface IRotationService {

    void applyRotation(int accelerometerRotation, int userRotation) = 1;

    void destroy() = 16777114;
}