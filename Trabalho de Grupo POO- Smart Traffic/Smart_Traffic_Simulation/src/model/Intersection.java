package model;

import java.util.*;

/**
 * Intersection holds mapping road -> traffic light, queue helpers and optional priority preemption support.
 * Updated: added outgoing options mapping so an incoming road can map to several outgoing roads (turns).
 */
public class Intersection {
    private final String id;
    private final Map<Road, TrafficLight> lights = new HashMap<>();

    // Optional: suporte a preempção (priority) — pode ser usado por EmergencyVehicle
    private Road priorityRoad = null;
    private double priorityTimeRemaining = 0.0;

    // NEW: outgoing roads list (all roads that start at this intersection)
    private final List<Road> outgoingRoads = new ArrayList<>();

    // NEW: mapping from incoming road -> possible outgoing roads (turn options)
    private final Map<Road, List<Road>> outgoingOptions = new HashMap<>();

    public Intersection(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public synchronized void addIncomingRoad(Road road, TrafficLight light) {
        lights.put(road, light);
    }

    public synchronized TrafficLight getLightForRoad(Road r) {
        return lights.get(r);
    }

    public synchronized Map<Road, TrafficLight> getLights() {
        return lights;
    }

    /**
     * Count vehicles that are near the end of the road and effectively waiting.
     */
    public int countWaitingVehicles(Road r) {
        int count = 0;
        double waitingThreshold = 12.0; // meters from the stop line
        double speedThreshold = 0.5; // m/s -> considered waiting
        List<Vehicle> vs = r.getVehicles();
        for (Vehicle v : vs) {
            if (v.getPosition() >= r.getLength() - waitingThreshold && v.getSpeed() <= speedThreshold) {
                count++;
            }
        }
        return count;
    }

    /**
     * Request priority (preemption) for road r for the given duration (seconds).
     * Immediately sets that road's light to GREEN and others to RED.
     */
    public synchronized void requestPriority(Road r, double durationSeconds) {
        if (!lights.containsKey(r)) return;
        priorityRoad = r;
        priorityTimeRemaining = durationSeconds;

        for (Map.Entry<Road, TrafficLight> e : lights.entrySet()) {
            Road road = e.getKey();
            TrafficLight tl = e.getValue();
            if (road.equals(r)) {
                tl.setState(new model.state.GreenState(durationSeconds));
            } else {
                tl.setState(new model.state.RedState(tl.getRedDuration()));
            }
        }
    }

    /**
     * Método chamado por Simulation.tick(dt) — decrementa timers internos (ex.: preempção).
     * Sem este método, Simulation não consegue atualizar timers específicos da interseção.
     */
    public synchronized void tick(double dt) {
        if (priorityTimeRemaining > 0.0) {
            priorityTimeRemaining -= dt;
            if (priorityTimeRemaining <= 0.0) {
                priorityTimeRemaining = 0.0;
                priorityRoad = null;
                // Nota: a estratégia deve gerir o próximo estado.
            }
        }
        // lógica extra para a intersecção pode ser adicionada aqui
    }

    public synchronized boolean hasPriority() {
        return priorityRoad != null && priorityTimeRemaining > 0.0;
    }

    public synchronized Road getPriorityRoad() {
        return priorityRoad;
    }

    // ---------- NEW: outgoing roads management ----------

    /**
     * Register an outgoing road starting at this intersection.
     */
    public synchronized void addOutgoingRoad(Road r) {
        if (!outgoingRoads.contains(r)) outgoingRoads.add(r);
    }

    /**
     * Return all outgoing roads (roads with from == this).
     */
    public synchronized List<Road> getOutgoingRoads() {
        return Collections.unmodifiableList(outgoingRoads);
    }

    /**
     * Define the options (turn choices) for an incoming road.
     * Example: incoming North_in -> [South_out (straight), East_out (left), West_out (right)]
     */
    public synchronized void setOutgoingOptions(Road incoming, List<Road> options) {
        outgoingOptions.put(incoming, new ArrayList<>(options));
    }

    /**
     * Return configured outgoing options for a given incoming road.
     * If none configured, returns all outgoing roads except a U-turn to the same (if applicable).
     */
    public synchronized List<Road> getOutgoingOptions(Road incoming) {
        List<Road> opts = outgoingOptions.get(incoming);
        if (opts != null) return Collections.unmodifiableList(opts);
        // fallback: return all outgoing roads
        List<Road> fallback = new ArrayList<>(outgoingRoads);
        return Collections.unmodifiableList(fallback);
    }
}