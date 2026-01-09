package controller;

import model.Intersection;
import model.Road;
import model.TrafficLight;
import model.Vehicle;
import util.Metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * World / Simulation: holds lists and executes fixed-step tick.
 * Added simple vehicle spawner to create continuous flow (loop).
 */
public class Simulation {
    private final List<Road> roads = new ArrayList<>();
    private final List<Intersection> intersections = new ArrayList<>();
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final Metrics metrics = new Metrics();
    private double simTime = 0.0;

    // spawning (loop)
    private double spawnInterval = 3.0; // seconds between spawn attempts
    private double spawnAccumulator = 0.0;
    private int nextVehicleId = 1;

    public synchronized void addRoad(Road r) { roads.add(r); }
    public synchronized void addIntersection(Intersection i) { intersections.add(i); }
    public synchronized void addVehicle(Vehicle v) {
        vehicles.add(v);
        if (v.getRoad() != null) v.getRoad().addVehicle(v);
    }

    public synchronized List<Vehicle> getVehicles() { return new ArrayList<>(vehicles); }
    public synchronized List<TrafficLight> getLights() {
        List<TrafficLight> out = new ArrayList<>();
        for (Intersection i : intersections) out.addAll(i.getLights().values());
        return out;
    }

    public synchronized List<Road> getRoads() { return new ArrayList<>(roads); }
    public synchronized List<Intersection> getIntersections() { return new ArrayList<>(intersections); }

    /**
     * Fixed-step tick: update intersections (timers), then strategies/lights, THEN vehicles.
     * dt in seconds.
     */
    public synchronized void tick(double dt, Strategy defaultStrategy) {
        simTime += dt;

        // 1) tick intersections (for priority timers)
        for (Intersection in : intersections) {
            in.tick(dt);
        }

        // 2) update strategies/lights
        for (Intersection in : intersections) {
            if (defaultStrategy != null) defaultStrategy.apply(dt, in);
            else {
                for (TrafficLight l : in.getLights().values()) l.update(dt);
            }
        }

        // 3) update vehicles
        for (Vehicle v : new ArrayList<>(vehicles)) {
            if (v.getRoad() == null) {
                // vehicle left the world
                vehicles.remove(v);
                metrics.countVehicleCompleted();
                continue;
            }
            v.update(dt);
        }

        // 4) spawn loop: periodically try to spawn vehicles at inbound roads
        spawnAccumulator += dt;
        if (spawnAccumulator >= spawnInterval) {
            spawnAccumulator = 0.0;
            trySpawnVehicles();
        }

        // update metrics with intersections (so we can record queue lengths)
        metrics.sample(simTime, vehicles, intersections);
    }

    private void trySpawnVehicles() {
        // spawn vehicles on roads whose id ends with "_in" if there is space near the start
        for (Road r : new ArrayList<>(roads)) {
            if (!r.getId().endsWith("_in")) continue;
            // check lane 0 (inbound) for space near start (first 10 meters)
            boolean space = true;
            for (Vehicle v : r.getVehiclesInLane(0)) {
                if (v.getPosition() < 12.0) { space = false; break; }
            }
            if (space) {
                String vid = "V" + (nextVehicleId++);
                // spawn at 5 meters in lane 0 with no preset route (dynamic turns)
                Vehicle nv = new Vehicle(vid, r, 5.0, 0, null);
                addVehicle(nv);
            }
        }
    }

    public Metrics getMetrics() { return metrics; }
    public double getSimTime() { return simTime; }

    public synchronized void clear() {
        roads.clear();
        intersections.clear();
        vehicles.clear();
    }
}