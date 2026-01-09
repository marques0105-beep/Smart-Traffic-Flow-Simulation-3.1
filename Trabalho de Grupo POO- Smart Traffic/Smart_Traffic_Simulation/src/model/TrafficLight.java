package model;

import model.state.TrafficLightState;

/**
 * TrafficLight using the State pattern.
 *
 * Delegates time progression to the current TrafficLightState instance.
 */
public class TrafficLight {
    private TrafficLightState state;

    // default durations (seconds)
    private final double greenDuration;
    private final double yellowDuration;
    private final double redDuration;

    public TrafficLight(double greenDuration, double yellowDuration, double redDuration) {
        this.greenDuration = greenDuration;
        this.yellowDuration = yellowDuration;
        this.redDuration = redDuration;
        // default to red
        this.state = new model.state.RedState(redDuration);
        this.state.enter(this);
    }

    /**
     * Called every tick by the simulation, dt in seconds.
     */
    public synchronized void update(double dt) {
        if (state != null) state.update(this, dt);
    }

    /**
     * Internal API used by states to request a transition.
     */
    public synchronized void changeState(TrafficLightState newState) {
        this.state = newState;
        if (this.state != null) this.state.enter(this);
    }

    /**
     * Force set a state (interrupting current). Useful for strategies.
     */
    public synchronized void setState(TrafficLightState newState) {
        changeState(newState);
    }

    /**
     * Public read-only color used by vehicles/UI.
     */
    public synchronized LightColor getColor() {
        return state == null ? LightColor.RED : state.getColor();
    }

    /**
     * Helper for vehicles: whether passage is allowed.
     */
    public synchronized boolean allowsPassage() {
        return getColor() == LightColor.GREEN;
    }

    public double getGreenDuration() {
        return greenDuration;
    }

    public double getYellowDuration() {
        return yellowDuration;
    }

    public double getRedDuration() {
        return redDuration;
    }
}