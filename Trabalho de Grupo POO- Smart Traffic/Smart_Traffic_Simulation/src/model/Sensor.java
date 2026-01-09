package model;

/**
 * Simple Sensor attached to a Road (or used by Intersection).
 * Detects "waiting" vehicles by using thresholds (position near end + low speed).
 */
public class Sensor {
    private final Road road;
    private final double waitingThresholdMeters;
    private final double speedThreshold;

    public Sensor(Road road, double waitingThresholdMeters, double speedThreshold) {
        this.road = road;
        this.waitingThresholdMeters = waitingThresholdMeters;
        this.speedThreshold = speedThreshold;
    }

    /**
     * Count vehicles considered waiting near the end of the road.
     */
    public int countWaiting() {
        int count = 0;
        for (Vehicle v : road.getVehicles()) {
            double distanceToEnd = road.getLength() - v.getPosition();
            if (distanceToEnd <= waitingThresholdMeters && v.getSpeed() <= speedThreshold) {
                count++;
            }
        }
        return count;
    }

    public Road getRoad() { return road; }
}