package view;

import controller.Simulation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import model.*;

/**
 * CanvasView: renderer updated to draw lanes (two lanes per road) and place vehicles in lane positions.
 */
public class CanvasView extends Canvas {
    private Simulation sim;

    public CanvasView(Simulation sim) {
        super(1000, 600);
        this.sim = sim;
    }

    public void setSimulation(Simulation sim) {
        this.sim = sim;
    }

    public void draw() {
        GraphicsContext g = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        // background
        g.setFill(Color.web("#e8e8e8"));
        g.fillRect(0, 0, w, h);

        if (sim == null) return;

        // draw intersection center
        double cx = w / 2;
        double cy = h / 2;
        double roadHalf = 220;

        // draw four roads (simple cross)
        g.setFill(Color.DARKGRAY);
        g.fillRect(cx - 60, cy - roadHalf, 120, roadHalf); // north half (wider to show two lanes)
        g.fillRect(cx - 60, cy, 120, roadHalf); // south half
        g.fillRect(cx - roadHalf, cy - 60, roadHalf, 120); // west half
        g.fillRect(cx, cy - 60, roadHalf, 120); // east half

        // lane centre markings (two lanes)
        g.setStroke(Color.WHITE);
        g.setLineWidth(2);
        g.setLineDashes(12, 8);
        // vertical center dashed
        g.strokeLine(cx, cy - roadHalf, cx, cy - 60);
        g.strokeLine(cx, cy + 60, cx, cy + roadHalf);
        // horizontal center dashed
        g.strokeLine(cx - roadHalf, cy, cx - 60, cy);
        g.strokeLine(cx + 60, cy, cx + roadHalf, cy);
        g.setLineDashes(null);

        // crosswalks
        drawCrosswalk(g, cx - 60 - 6, cy - 20, 12, 40);
        drawCrosswalk(g, cx + 60 - 6, cy - 20, 12, 40);
        drawCrosswalk(g, cx - 20, cy - 60 - 6, 40, 12);
        drawCrosswalk(g, cx - 20, cy + 60 - 6, 40, 12);

        // draw traffic lights near intersection (inbound control)
        for (Intersection I : sim.getIntersections()) {
            for (Road r : I.getLights().keySet()) {
                TrafficLight light = I.getLights().get(r);
                double lx = cx, ly = cy;
                switch (r.getId()) {
                    case "North_in":
                        lx = cx - 80;
                        ly = cy - 60 - 10;
                        break;
                    case "South_in":
                        lx = cx + 80;
                        ly = cy + 60 + 10;
                        break;
                    case "East_in":
                        lx = cx + 60 + 10;
                        ly = cy - 80;
                        break;
                    case "West_in":
                        lx = cx - 60 - 10;
                        ly = cy + 80;
                        break;
                    default:
                        lx = cx;
                        ly = cy;
                }

                Color c = Color.DARKRED;
                model.LightColor lc = light.getColor();
                if (lc != null) {
                    switch (lc) {
                        case GREEN: c = Color.LIMEGREEN; break;
                        case YELLOW: c = Color.GOLD; break;
                        case RED: c = Color.DARKRED; break;
                    }
                }

                g.setFill(Color.BLACK);
                g.fillOval(lx - 6, ly - 6, 12, 12);
                g.setFill(c);
                g.fillOval(lx - 4, ly - 4, 8, 8);
            }
        }

        // draw vehicles (placed into lane positions)
        for (Vehicle v : sim.getVehicles()) {
            if (v.getRoad() == null) continue;
            double x = cx, y = cy;
            double pos = v.getPosition(); // meters along the road
            double scale = 0.6; // visual scaling factor
            int lane = v.getLane();

            String id = v.getRoad().getId();
            // inbound roads (start at outer edge, go towards center)
            if ("North_in".equals(id)) {
                double startX = cx - 20;
                double startY = cy - roadHalf;
                x = startX + (lane == 0 ? -8 : 8); // two lanes offset
                y = startY + pos * scale;
            } else if ("South_in".equals(id)) {
                double startX = cx + 20;
                double startY = cy + roadHalf;
                x = startX + (lane == 0 ? -8 : 8);
                y = startY - pos * scale;
            } else if ("East_in".equals(id)) {
                double startX = cx + roadHalf;
                double startY = cy - 20;
                x = startX - pos * scale;
                y = startY + (lane == 0 ? -8 : 8);
            } else if ("West_in".equals(id)) {
                double startX = cx - roadHalf;
                double startY = cy + 20;
                x = startX + pos * scale;
                y = startY + (lane == 0 ? -8 : 8);
            }
            // outbound roads (start just after intersection and go outward)
            else if ("North_out".equals(id)) {
                double startX = cx - 20;
                double startY = cy - 60;
                x = startX + (lane == 0 ? -8 : 8);
                y = startY - pos * scale;
            } else if ("South_out".equals(id)) {
                double startX = cx + 20;
                double startY = cy + 60;
                x = startX + (lane == 0 ? -8 : 8);
                y = startY + pos * scale;
            } else if ("East_out".equals(id)) {
                double startX = cx + 60;
                double startY = cy - 20;
                x = startX + pos * scale;
                y = startY + (lane == 0 ? -8 : 8);
            } else if ("West_out".equals(id)) {
                double startX = cx - 60;
                double startY = cy + 20;
                x = startX - pos * scale;
                y = startY + (lane == 0 ? -8 : 8);
            } else {
                // fallback - center line
                x = cx + pos * scale - 10;
                y = cy - 100;
            }

            // vehicle rectangle
            g.setFill(Color.DODGERBLUE);
            double vw = Math.max(8, v.getLength() * 2);
            double vh = 12;
            g.fillRoundRect(x, y, vw, vh, 4, 4);

            // ID label
            g.setFill(Color.WHITE);
            g.fillText(v.getId(), x + 2, y + vh - 2);
        }

        // HUD
        g.setFill(Color.BLACK);
        g.fillText(String.format("Sim time: %.1f s", sim.getSimTime()), 12, 18);
        g.fillText(String.format("Vehicles active: %d", sim.getVehicles().size()), 12, 34);
        g.fillText(String.format("Completed: %d", sim.getMetrics().getCompleted()), 12, 50);
    }

    private void drawCrosswalk(GraphicsContext g, double x, double y, double w, double h) {
        int stripes = 6;
        g.setFill(Color.WHITE);
        if (w > h) {
            double stripeW = w / (stripes * 2.0);
            for (int i = 0; i < stripes; i++) {
                double sx = x + i * 2 * stripeW;
                g.fillRect(sx, y, stripeW, h);
            }
        } else {
            double stripeH = h / (stripes * 2.0);
            for (int i = 0; i < stripes; i++) {
                double sy = y + i * 2 * stripeH;
                g.fillRect(x, sy, w, stripeH);
            }
        }
    }
}