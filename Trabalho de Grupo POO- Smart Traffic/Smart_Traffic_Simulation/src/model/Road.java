package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Road holds vehicles in two lanes (lane 0 and lane 1).
 * lane 0: conventional inbound lane (towards intersection) if used that way
 * lane 1: outbound / opposite lane.
 *
 * This keeps a simple two-lane structure without creating many Road objects.
 */
public class Road {
    private final String id;
    private final double length; // meters
    private final Intersection from;
    private final Intersection to;

    // two lanes
    private final List<Vehicle> lane0 = new ArrayList<>();
    private final List<Vehicle> lane1 = new ArrayList<>();

    public Road(String id, double length, Intersection from, Intersection to) {
        this.id = id;
        this.length = length;
        this.from = from;
        this.to = to;
    }

    public String getId() { return id; }

    public double getLength() { return length; }

    public Intersection getTo() { return to; }

    public Intersection getFrom() { return from; }

    /**
     * Add vehicle to the lane indicated by Vehicle.getLane().
     */
    public synchronized void addVehicle(Vehicle v) {
        int lane = v.getLane();
        if (lane == 0) lane0.add(v);
        else lane1.add(v);
    }

    public synchronized void removeVehicle(Vehicle v) {
        lane0.remove(v);
        lane1.remove(v);
    }

    /**
     * Return all vehicles on this road (both lanes) as an unmodifiable list.
     */
    public synchronized List<Vehicle> getVehicles() {
        List<Vehicle> out = new ArrayList<>(lane0.size() + lane1.size());
        out.addAll(lane0);
        out.addAll(lane1);
        return Collections.unmodifiableList(out);
    }

    /**
     * Get vehicles in the specified lane (0 or 1).
     */
    public synchronized List<Vehicle> getVehiclesInLane(int lane) {
        if (lane == 0) return Collections.unmodifiableList(new ArrayList<>(lane0));
        else return Collections.unmodifiableList(new ArrayList<>(lane1));
    }

    /**
     * Return the vehicle ahead of the given one on this road in the same lane, or null.
     */
    public synchronized Vehicle vehicleAhead(Vehicle v) {
        int lane = v.getLane();
        List<Vehicle> list = (lane == 0) ? lane0 : lane1;
        Vehicle ahead = null;
        for (Vehicle other : list) {
            if (other == v) continue;
            if (other.getPosition() > v.getPosition()) {
                if (ahead == null || other.getPosition() < ahead.getPosition()) {
                    ahead = other;
                }
            }
        }
        return ahead;
    }
}