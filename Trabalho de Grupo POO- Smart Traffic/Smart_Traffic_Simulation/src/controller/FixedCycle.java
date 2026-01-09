package controller;

import model.Intersection;
import model.TrafficLight;
import model.state.GreenState;
import model.state.RedState;

/**
 * FixedCycle strategy adapted to the State pattern.
 * Rotates the green among incoming roads by setting GreenState/RedState on TrafficLight.
 */
public class FixedCycle implements Strategy {
    private final double switchInterval;
    private double timer = 0;
    private int currentIndex = 0;

    public FixedCycle(double switchInterval) {
        this.switchInterval = switchInterval;
    }

    @Override
    public void apply(double dt, Intersection intersection) {
        timer += dt;
        if (timer >= switchInterval) {
            // rotate greens
            timer = 0;
            TrafficLight[] arr = intersection.getLights().values().toArray(new TrafficLight[0]);
            if (arr.length == 0) return;

            // set current to red (use its red duration)
            TrafficLight prev = arr[currentIndex];
            prev.setState(new RedState(prev.getRedDuration()));

            // advance index and set next to green (use its green duration)
            currentIndex = (currentIndex + 1) % arr.length;
            TrafficLight next = arr[currentIndex];
            next.setState(new GreenState(next.getGreenDuration()));
        }

        // update all lights' internal timers
        for (TrafficLight t : intersection.getLights().values()) {
            t.update(dt);
        }
    }
}