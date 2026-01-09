package model.state;

import model.LightColor;
import model.TrafficLight;

/**
 * Yellow state: stays yellow for duration then switches to RedState.
 */
public class YellowState implements TrafficLightState {
    private final double duration;
    private double elapsed = 0.0;

    public YellowState(double duration) {
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
            // move to red
            light.changeState(new RedState(light.getRedDuration()));
        }
    }

    @Override
    public LightColor getColor() {
        return LightColor.YELLOW;
    }
}