package controller;

import model.Intersection;
import model.Road;
import model.TrafficLight;
import model.state.GreenState;
import model.state.RedState;

import java.util.HashMap;
import java.util.Map;

/**
 * AdaptiveCycle with preemption awareness.
 * Prevents switching when there's zero waiting vehicles to avoid accidental all-green.
 */
public class AdaptiveCycle implements Strategy {
    private final double baseGreen;   // minimum green duration (s)
    private final double kPerVehicle; // additional seconds per waiting vehicle
    private final double maxGreen;    // cap for green duration
    private final double minGreenHold; // minimum time to hold green once assigned

    private final Map<Intersection, Road> currentGreenRoad = new HashMap<>();
    private final Map<Intersection, Double> greenElapsed = new HashMap<>();

    public AdaptiveCycle(double baseGreen, double kPerVehicle, double maxGreen, double minGreenHold) {
        this.baseGreen = baseGreen;
        this.kPerVehicle = kPerVehicle;
        this.maxGreen = maxGreen;
        this.minGreenHold = minGreenHold;
    }

    @Override
    public void apply(double dt, Intersection intersection) {
        // update all lights' internal timers
        for (TrafficLight t : intersection.getLights().values()) {
            t.update(dt);
        }

        // increment green elapsed for this intersection
        greenElapsed.putIfAbsent(intersection, 0.0);
        double elapsed = greenElapsed.get(intersection) + dt;
        greenElapsed.put(intersection, elapsed);

        // if the intersection currently has an active priority request, respect it
        if (intersection.hasPriority()) {
            Road pr = intersection.getPriorityRoad();
            if (pr != null) {
                currentGreenRoad.put(intersection, pr);
                greenElapsed.put(intersection, 0.0);
            }
            return;
        }

        // compute waiting counts to choose best road
        Road best = null;
        int maxWaiting = -1;
        for (Road r : intersection.getLights().keySet()) {
            int waiting = intersection.countWaitingVehicles(r);
            if (waiting > maxWaiting) {
                maxWaiting = waiting;
                best = r;
            }
        }
        if (best == null) return;

        // If nobody is waiting, do not change the configuration (prevents all-green on spawn)
        if (maxWaiting <= 0) {
            return;
        }

        Road current = currentGreenRoad.get(intersection);
        if (current == null) {
            assignGreen(intersection, best, maxWaiting);
            return;
        }

        // enforce minGreenHold
        if (!current.equals(best) && greenElapsed.getOrDefault(intersection, 0.0) < minGreenHold) {
            return;
        }

        if (!current.equals(best)) {
            assignGreen(intersection, best, maxWaiting);
        } else {
            // extend current green if needed
            TrafficLight tl = intersection.getLightForRoad(current);
            double adaptiveDuration = Math.min(maxGreen, baseGreen + kPerVehicle * maxWaiting);
            if (tl.getColor() != model.LightColor.GREEN) {
                tl.setState(new GreenState(adaptiveDuration));
            }
        }
    }

    private void assignGreen(Intersection intersection, Road best, int waitingCount) {
        double adaptiveDuration = Math.min(maxGreen, baseGreen + kPerVehicle * waitingCount);
        for (Road r : intersection.getLights().keySet()) {
            TrafficLight t = intersection.getLights().get(r);
            if (r.equals(best)) {
                t.setState(new GreenState(adaptiveDuration));
            } else {
                t.setState(new RedState(t.getRedDuration()));
            }
        }
        currentGreenRoad.put(intersection, best);
        greenElapsed.put(intersection, 0.0);
    }
}