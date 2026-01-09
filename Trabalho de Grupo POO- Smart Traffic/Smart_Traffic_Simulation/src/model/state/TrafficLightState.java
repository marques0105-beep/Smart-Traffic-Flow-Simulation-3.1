package model.state;

import model.LightColor;
import model.TrafficLight;

/**
 * State interface for TrafficLight (State pattern).
 */
public interface TrafficLightState {
    /**
     * Called when the state becomes active.
     */
    void enter(TrafficLight light);

    /**
     * Called each tick with dt in seconds. Implementations must call light.changeState(...) to transit.
     */
    void update(TrafficLight light, double dt);

    /**
     * Public color seen by vehicles/UI.
     */
    LightColor getColor();
}