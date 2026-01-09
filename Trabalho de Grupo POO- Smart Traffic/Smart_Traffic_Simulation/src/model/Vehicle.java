package model;

import java.util.List;
import java.util.Random;

/**
 * Vehicle with lane support (0 or 1).
 *
 * Constructor now expects lane index so the vehicle can be placed in a specific lane when spawned.
 */
public class Vehicle {
    private final String id;
    private Road road;
    private double position; // meters from start of road
    private double speed; // m/s
    private final double maxSpeed = 18.0; // increased (~65 km/h)
    private final double length = 4.5; // vehicle length, meters
    private final List<Road> route; // optional precomputed route (may be null)
    private int routeIndex = 0;
    private double waitingTime = 0.0;

    // lane: 0 or 1
    private int lane;

    private static final Random RNG = new Random();

    public Vehicle(String id, Road startRoad, double startPos, int lane, List<Road> route) {
        this.id = id;
        this.road = startRoad;
        this.position = startPos;
        this.lane = lane;
        this.route = route;
    }

    public String getId() { return id; }
    public double getPosition() { return position; }
    public double getLength() { return length; }
    public Road getRoad() { return road; }
    public double getWaitingTime() { return waitingTime; }
    public double getSpeed() { return speed; }
    public int getLane() { return lane; }
    public void setLane(int lane) { this.lane = lane; }

    /**
     * Update vehicle: move respecting vehicle ahead and traffic light at end of road.
     * dt in seconds.
     */
    public void update(double dt) {
        if (road == null) return;

        // desired speed
        double desiredSpeed = maxSpeed;

        // check vehicle ahead on same road and lane
        Vehicle ahead = road.vehicleAhead(this);
        if (ahead != null) {
            double gap = ahead.getPosition() - this.position - ahead.getLength();
            double safeDistance = 2.0 + this.speed * 1.0; // simple model: 2m + 1s headway
            if (gap < safeDistance) {
                // slow down to avoid collision
                desiredSpeed = Math.min(desiredSpeed, Math.max(0, ahead.getSpeed() - 1.0));
            }
        }

        // check traffic light at end of road (if near) - inbound lanes should obey the intersection light
        Intersection next = road.getTo();
        double distanceToEnd = road.getLength() - position;
        boolean shouldStopForLight = false;
        if (next != null && distanceToEnd < 8.0) { // reaction zone
            TrafficLight light = next.getLightForRoad(road);
            if (light != null && !light.allowsPassage()) {
                // need to stop before intersection
                desiredSpeed = 0;
                shouldStopForLight = true;
            }
        }

        // acceleration/braking (tuned up)
        double accel = 4.0; // m/s^2 (slightly increased)
        double brakeAccel = 8.0; // m/s^2 stronger braking
        if (speed < desiredSpeed) {
            speed = Math.min(desiredSpeed, speed + accel * dt);
        } else {
            speed = Math.max(desiredSpeed, speed - brakeAccel * dt);
        }

        // update position
        double delta = speed * dt;
        if (shouldStopForLight && delta > distanceToEnd) {
            // don't enter intersection
            delta = Math.max(0, distanceToEnd - 0.5);
            speed = 0;
        }

        position += delta;

        // handle end of road / move to next
        if (position >= road.getLength() - 0.01) {
            advanceToNextRoad();
        }

        if (speed < 0.1) {
            waitingTime += dt;
        }
    }

    private void advanceToNextRoad() {
        Road old = road;
        old.removeVehicle(this);

        // If a precomputed route exists, follow it
        Road nextRoad = null;
        if (route != null && routeIndex < route.size()) {
            nextRoad = route.get(routeIndex);
            routeIndex++;
        } else {
            // dynamic: pick one of the outgoing options from the intersection
            Intersection inter = old.getTo();
            if (inter == null) {
                // leaves the world
                road = null;
                position = 0;
                speed = 0;
                return;
            }
            List<Road> opts = inter.getOutgoingOptions(old);
            if (opts == null || opts.isEmpty()) {
                // nothing to go to -> leave
                road = null;
                position = 0;
                speed = 0;
                return;
            }
            // choose randomly among options (could be weighted later)
            nextRoad = opts.get(RNG.nextInt(opts.size()));
        }

        if (nextRoad == null) {
            road = null;
            position = 0;
            speed = 0;
        } else {
            // when entering a new road, select proper lane based on direction
            this.road = nextRoad;
            // heurística: se o road id contém "_in" então lane 0 é inbound; se contém "_out" então lane 1 é outbound
            if (nextRoad.getId().endsWith("_in")) this.lane = 0;
            else if (nextRoad.getId().endsWith("_out")) this.lane = 1;
            else {
                // fallback: keep previous lane or use lane 0
                this.lane = Math.max(0, Math.min(1, this.lane));
            }
            this.position = 0.1; // small offset into new road
            road.addVehicle(this);
        }
    }
}