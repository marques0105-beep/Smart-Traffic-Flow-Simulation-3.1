package controller;

import model.Intersection;

/**
 * Strategy interface for controlling lights (fixed/adaptive).
 */
public interface Strategy {
    void apply(double dt, Intersection intersection);
}