package model.state;

import model.LightColor;
import model.TrafficLight;

/**
 * Green state: stays green for duration then switches to YellowState.
 */
public class GreenState implements TrafficLightState {
    private final double duration;
    private double elapsed = 0.0;

    public GreenState(double duration) {
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
            // move to yellow
            light.changeState(new YellowState(light.getYellowDuration()));
        }
    }

    @Override
    public LightColor getColor() {
        return LightColor.GREEN;
    }
}