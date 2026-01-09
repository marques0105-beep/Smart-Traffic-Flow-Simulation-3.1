package model.state;

import model.LightColor;
import model.TrafficLight;

/**
 * Red state: stays red for duration then switches to GreenState.
 */
public class RedState implements TrafficLightState {
    private final double duration;
    private double elapsed = 0.0;

    public RedState(double duration) {
        this.duration = duration;
    }

    @Override
    public void enter(TrafficLight light) {
        this.elapsed = 0.0;
    }

    @Override
    public void update(TrafficLight light, double dt) {
        elapsed += dt;
        if (elapsed >= duration) {
            // move to green
            light.changeState(new GreenState(light.getGreenDuration()));
        }
    }

    @Override
    public LightColor getColor() {
        return LightColor.RED;
    }
}