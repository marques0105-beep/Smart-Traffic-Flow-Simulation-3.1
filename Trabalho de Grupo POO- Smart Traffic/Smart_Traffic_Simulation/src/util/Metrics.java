package util;

import model.Intersection;
import model.Vehicle;

import java.util.*;

/**
 * Metrics collects snapshots of simulation metrics for later export/plot.
 * Each sample records timestamp, average waiting time, completed vehicles, active count and queue lengths per road.
 */
public class Metrics {
    private int completed = 0;
    private final List<MetricsSnapshot> snapshots = new ArrayList<>();

    public synchronized void countVehicleCompleted() { completed++; }

    /**
     * Sample the current state. intersections used to get queue lengths.
     */
    public synchronized void sample(double simTime, List<Vehicle> vehicles, List<Intersection> intersections) {
        double avgWaiting = 0.0;
        if (!vehicles.isEmpty()) {
            double sum = 0.0;
            for (Vehicle v : vehicles) sum += v.getWaitingTime();
            avgWaiting = sum / vehicles.size();
        }
        int active = vehicles.size();
        Map<String, Integer> queues = new LinkedHashMap<>();
        for (Intersection in : intersections) {
            // record each incoming road by name
            for (Map.Entry<?, ?> e : in.getLights().entrySet()) {
                // key is Road instance
                Object roadObj = e.getKey();
                try {
                    String name = roadObj.toString();
                    // safer: if road class has getId method
                    String id;
                    try {
                        id = (String) roadObj.getClass().getMethod("getId").invoke(roadObj);
                    } catch (Exception ex) {
                        id = name;
                    }
                    // intersection.countWaitingVehicles requires casting to proper types; call reflect not used here
                    // But Intersection has method countWaitingVehicles(Road) — we need to cast roadObj to the Road type
                    // We'll attempt a cast
                    int q = 0;
                    try {
                        q = in.countWaitingVehicles((model.Road) roadObj);
                    } catch (ClassCastException ce) {
                        q = 0;
                    }
                    queues.put(id, q);
                } catch (Exception ex) {
                    // fallback: ignore
                }
            }
        }
        MetricsSnapshot snap = new MetricsSnapshot(simTime, avgWaiting, completed, active, queues);
        snapshots.add(snap);
    }

    public synchronized int getCompleted() { return completed; }

    public synchronized List<MetricsSnapshot> getSnapshots() {
        return new ArrayList<>(snapshots);
    }

    /**
     * Snapshot DTO (public so exporter can access)
     */
    public static class MetricsSnapshot {
        private final double time;
        private final double avgWaiting;
        private final int completed;
        private final int active;
        private final Map<String, Integer> queueLengths;

        public MetricsSnapshot(double time, double avgWaiting, int completed, int active, Map<String, Integer> queueLengths) {
            this.time = time;
            this.avgWaiting = avgWaiting;
            this.completed = completed;
            this.active = active;
            this.queueLengths = new LinkedHashMap<>(queueLengths);
        }

        public double getTime() { return time; }
        public double getAvgWaiting() { return avgWaiting; }
        public int getCompleted() { return completed; }
        public int getActive() { return active; }
        public Map<String, Integer> getQueueLengths() { return new LinkedHashMap<>(queueLengths); }
    }
}