package view;

import controller.AdaptiveCycle;
import controller.FixedCycle;
import controller.Simulation;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import controller.Strategy;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.*;
import util.CsvExporter;

import java.io.IOException;
import java.util.Arrays;

/**
 * MainApp updated to create paired inbound/outbound roads and dynamic routing.
 */
public class MainApp extends Application {

    private Simulation sim;
    private CanvasView canvas;
    private ControlPanel controls;
    private Strategy strategy;

    private AnimationTimer animator;
    private boolean running = false;

    // fixed-step for deterministic simulation, seconds
    private final double fixedDt = 0.05; // 50 ms -> 20 updates/sec

    // accumulator for fixed-step loop
    private double accumulator = 0.0;
    private long lastTime = 0;

    @Override
    public void start(Stage stage) {
        setupWorld();

        // choose strategy: adaptive example
        strategy = new AdaptiveCycle(5.0, 1.0, 20.0, 3.0); // base=5s, +1s per waiting car, max=20s, min hold 3s
        // strategy = new FixedCycle(8.0); // alternative

        canvas = new CanvasView(sim);
        controls = new ControlPanel();

        // hook control events
        controls.getStartButton().setOnAction(e -> startSimulation());
        controls.getStopButton().setOnAction(e -> stopSimulation());
        controls.getResetButton().setOnAction(e -> {
            stopSimulation();
            setupWorld();
            canvas.setSimulation(sim);
            controls.updateLabels(sim.getSimTime(), sim.getMetrics());
            canvas.draw();
        });

        controls.getExportButton().setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("metrics.json");
            FileChooser.ExtensionFilter jsonFilter = new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json");
            FileChooser.ExtensionFilter csvFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
            fc.getExtensionFilters().addAll(jsonFilter, csvFilter);
            java.io.File f = fc.showSaveDialog(stage);
            if (f != null) {
                try {
                    String fn = f.getName().toLowerCase();
                    if (fn.endsWith(".csv")) {
                        CsvExporter.exportCsv(sim.getMetrics().getSnapshots(), f.toPath());
                    } else {
                        // default json
                        CsvExporter.exportJson(sim.getMetrics().getSnapshots(), f.toPath());
                    }
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Exported metrics to " + f.getAbsolutePath());
                    a.show();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Alert a = new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage());
                    a.show();
                }
            }
        });

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        BorderPane.setMargin(canvas, new Insets(8));
        root.setBottom(controls);
        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.setTitle("Smart Traffic Flow");
        stage.show();

        // AnimationTimer for rendering + stepping simulation with fixed-step loop
        animator = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) lastTime = now;
                double elapsed = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                // apply speed multiplier from slider
                double speedMultiplier = controls.getSpeedMultiplier();
                accumulator += elapsed * speedMultiplier;

                // run as many fixed steps as needed
                while (accumulator >= fixedDt) {
                    sim.tick(fixedDt, strategy);
                    accumulator -= fixedDt;
                }

                // update UI labels
                controls.updateLabels(sim.getSimTime(), sim.getMetrics());
                // redraw
                canvas.draw();
            }
        };

        // start paused (user must click Start)
        canvas.draw();
    }

    private void startSimulation() {
        if (!running) {
            accumulator = 0.0;
            lastTime = 0;
            animator.start();
            running = true;
            controls.setRunning(true);
        }
    }

    private void stopSimulation() {
        if (running) {
            animator.stop();
            running = false;
            controls.setRunning(false);
        }
    }

    /**
     * Build a sample world (cross) with vehicles.
     * Now creates inbound/outbound pairs and sets turn options for intersection.
     */
    private void setupWorld() {
        sim = new Simulation();

        // create intersection
        Intersection inter = new Intersection("I1");
        sim.addIntersection(inter);

        // create road pairs (inbound = towards intersection, outbound = away from intersection)
        // naming: North_in, North_out, etc.
        Road northIn = new Road("North_in", 300, null, inter);
        Road northOut = new Road("North_out", 300, inter, null);
        Road southIn = new Road("South_in", 300, null, inter);
        Road southOut = new Road("South_out", 300, inter, null);
        Road eastIn = new Road("East_in", 300, null, inter);
        Road eastOut = new Road("East_out", 300, inter, null);
        Road westIn = new Road("West_in", 300, null, inter);
        Road westOut = new Road("West_out", 300, inter, null);

        // register roads in simulation
        sim.addRoad(northIn); sim.addRoad(northOut);
        sim.addRoad(southIn); sim.addRoad(southOut);
        sim.addRoad(eastIn);  sim.addRoad(eastOut);
        sim.addRoad(westIn);  sim.addRoad(westOut);

        // lights only for incoming roads
        TrafficLight ln = new TrafficLight(8, 2, 16);
        TrafficLight ls = new TrafficLight(8, 2, 16);
        TrafficLight le = new TrafficLight(8, 2, 16);
        TrafficLight lw = new TrafficLight(8, 2, 16);

        // initial states
        ln.setState(new model.state.GreenState(6.0));
        lw.setState(new model.state.GreenState(6.0));
        ls.setState(new model.state.RedState(16.0));
        le.setState(new model.state.RedState(16.0));

        // add incoming mapping
        inter.addIncomingRoad(northIn, ln);
        inter.addIncomingRoad(southIn, ls);
        inter.addIncomingRoad(eastIn, le);
        inter.addIncomingRoad(westIn, lw);

        // register outgoing roads in intersection
        inter.addOutgoingRoad(northOut);
        inter.addOutgoingRoad(southOut);
        inter.addOutgoingRoad(eastOut);
        inter.addOutgoingRoad(westOut);

        // define turning options per incoming road (straight, left, right)
        // from northIn: straight -> southOut, left -> eastOut, right -> westOut
        inter.setOutgoingOptions(northIn, Arrays.asList(southOut, eastOut, westOut));
        // from southIn:
        inter.setOutgoingOptions(southIn, Arrays.asList(northOut, westOut, eastOut));
        // from eastIn:
        inter.setOutgoingOptions(eastIn, Arrays.asList(westOut, northOut, southOut));
        // from westIn:
        inter.setOutgoingOptions(westIn, Arrays.asList(eastOut, southOut, northOut));

        // spawn vehicles on inbound roads (start at the beginning of each inbound road)
        int perRoad = 5;
        double startOffsetMeters = 5.0;   // distância do início do road (em metros)
        double spacingMeters = 12.0;      // espaçamento entre veículos em metros

        // spawn vehicles on inbound roads (start at the beginning of each inbound road)
        for (int i = 0; i < perRoad; i++) {
            Vehicle vn = new Vehicle("N" + i, northIn, startOffsetMeters + i * spacingMeters, 0, null);
            sim.addVehicle(vn);
            Vehicle vs = new Vehicle("S" + i, southIn, startOffsetMeters + i * spacingMeters, 0, null);
            sim.addVehicle(vs);
            Vehicle ve = new Vehicle("E" + i, eastIn, startOffsetMeters + i * spacingMeters, 0, null);
            sim.addVehicle(ve);
            Vehicle vw = new Vehicle("W" + i, westIn, startOffsetMeters + i * spacingMeters, 0, null);
            sim.addVehicle(vw);
        }

        // (Opcional) you can also spawn vehicles on outbound roads if needed
        // e.g., vehicles leaving the center (use small position near center)
        // Vehicle vOut = new Vehicle("O1", northOut, 5.0, Arrays.asList(northOut));
        // sim.addVehicle(vOut);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (animator != null) animator.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}