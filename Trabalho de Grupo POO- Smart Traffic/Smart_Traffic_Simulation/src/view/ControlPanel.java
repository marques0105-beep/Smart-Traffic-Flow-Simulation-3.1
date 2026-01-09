package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import util.Metrics;

import java.util.List;

/**
 * Control panel extended with export button and stats labels.
 */
public class ControlPanel extends HBox {
    private final Button startButton = new Button("Start");
    private final Button stopButton = new Button("Stop");
    private final Button resetButton = new Button("Reset");
    private final Button exportButton = new Button("Export");
    private final Slider speedSlider = new Slider(0.1, 5.0, 1.0);
    private final Label speedLabel = new Label("Speed: 1.0x");
    private final Label timeLabel = new Label("Time: 0.0s");
    private final Label avgWaitLabel = new Label("Avg wait: 0.0s");
    private final Label completedLabel = new Label("Completed: 0");

    public ControlPanel() {
        setSpacing(12);
        setPadding(new Insets(8));
        setAlignment(Pos.CENTER_LEFT);

        speedSlider.setPrefWidth(200);
        speedSlider.setMajorTickUnit(0.5);
        speedSlider.setShowTickMarks(false);

        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double v = Math.round(newV.doubleValue() * 10.0) / 10.0;
            speedLabel.setText("Speed: " + v + "x");
        });

        stopButton.setDisable(true); // initially stopped

        getChildren().addAll(startButton, stopButton, resetButton, exportButton, speedLabel, speedSlider, timeLabel, avgWaitLabel, completedLabel);
    }

    public Button getStartButton() { return startButton; }
    public Button getStopButton() { return stopButton; }
    public Button getResetButton() { return resetButton; }
    public Button getExportButton() { return exportButton; }
    public double getSpeedMultiplier() { return speedSlider.getValue(); }

    public void setRunning(boolean running) {
        startButton.setDisable(running);
        stopButton.setDisable(!running);
    }

    public void updateLabels(double simTime, Metrics metrics) {
        timeLabel.setText(String.format("Time: %.1f s", simTime));
        List<Metrics.MetricsSnapshot> snaps = metrics.getSnapshots();
        if (!snaps.isEmpty()) {
            Metrics.MetricsSnapshot last = snaps.get(snaps.size() - 1);
            avgWaitLabel.setText(String.format("Avg wait: %.2f s", last.getAvgWaiting()));
            completedLabel.setText(String.format("Completed: %d", last.getCompleted()));
        } else {
            avgWaitLabel.setText("Avg wait: 0.0s");
            completedLabel.setText("Completed: 0");
        }
    }
}