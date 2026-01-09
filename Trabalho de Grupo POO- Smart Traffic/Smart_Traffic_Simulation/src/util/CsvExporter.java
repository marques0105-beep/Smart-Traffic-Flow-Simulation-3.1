package util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Export metrics snapshots to CSV or JSON. No external libs used.
 */
public class CsvExporter {

    /**
     * Export JSON array of snapshots to path.
     */
    public static void exportJson(List<Metrics.MetricsSnapshot> snapshots, Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < snapshots.size(); i++) {
            Metrics.MetricsSnapshot s = snapshots.get(i);
            sb.append("  {\n");
            sb.append("    \"time\": ").append(s.getTime()).append(",\n");
            sb.append("    \"avgWaiting\": ").append(s.getAvgWaiting()).append(",\n");
            sb.append("    \"completed\": ").append(s.getCompleted()).append(",\n");
            sb.append("    \"active\": ").append(s.getActive()).append(",\n");
            sb.append("    \"queues\": {\n");
            Map<String,Integer> q = s.getQueueLengths();
            int j=0;
            for (Map.Entry<String,Integer> e : q.entrySet()) {
                sb.append("      \"").append(escape(e.getKey())).append("\": ").append(e.getValue());
                if (j < q.size()-1) sb.append(",");
                sb.append("\n");
                j++;
            }
            sb.append("    }\n");
            sb.append("  }");
            if (i < snapshots.size()-1) sb.append(",\n"); else sb.append("\n");
        }
        sb.append("]\n");
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write(sb.toString());
        }
    }

    /**
     * Export CSV: columns are time,avgWaiting,completed,active,<dynamic queue columns sorted>.
     */
    public static void exportCsv(List<Metrics.MetricsSnapshot> snapshots, Path path) throws IOException {
        // build set of all queue keys
        LinkedHashSet<String> allQueues = new LinkedHashSet<>();
        for (Metrics.MetricsSnapshot s : snapshots) {
            allQueues.addAll(s.getQueueLengths().keySet());
        }
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            // header
            List<String> header = new ArrayList<>();
            header.add("time");
            header.add("avgWaiting");
            header.add("completed");
            header.add("active");
            header.addAll(allQueues);
            w.write(String.join(",", header));
            w.write("\n");
            for (Metrics.MetricsSnapshot s : snapshots) {
                List<String> row = new ArrayList<>();
                row.add(Double.toString(s.getTime()));
                row.add(Double.toString(s.getAvgWaiting()));
                row.add(Integer.toString(s.getCompleted()));
                row.add(Integer.toString(s.getActive()));
                Map<String,Integer> q = s.getQueueLengths();
                for (String k : allQueues) {
                    row.add(Integer.toString(q.getOrDefault(k, 0)));
                }
                w.write(String.join(",", row));
                w.write("\n");
            }
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}