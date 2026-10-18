package huddtds.benchmark;

import java.io.Serializable;
import java.util.Locale;

/**
 * Cấu trúc dữ liệu ghi nhận đầy đủ chỉ số hiệu năng và trôi dạt của một dataset.
 * Hỗ trợ chuyển đổi JSON độc lập không cần thư viện ngoài.
 */
public class BenchmarkMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    public String datasetName = "";
    public String safeFileName = "";
    public String architecture = "";
    public double minutil = 0.0;
    public int interval = 0;
    public int windowSize = 0;
    public double alpha = 0.0;
    public int maxPattern = 0;
    public int totalTransactions = 0;
    public int totalCheckpoints = 0;
    public double elapsedSeconds = 0.0;
    public int avgTps = 0;
    public int peakTps = 0;
    public double currentRamMb = 0.0;
    public double peakRamMb = 0.0;
    public double maxRamMb = 0.0;
    public long totalAllocatedObjects = 0L;
    public long gcCount = 0L;
    public long gcTimeMs = 0L;
    public double gcOverheadPct = 0.0;
    public double avgCpMs = 0.0;
    public int distinctHuis = 0;
    public int totalHuis = 0;
    public double avgHuiPerCp = 0.0;
    public int maxHuiPerCp = 0;
    public String topItemset = "N/A";
    public double topUtility = 0.0;
    public int globalDrifts = 0;
    public int localDrifts = 0;
    public double driftRatePct = 0.0;
    public double avgDisHs = 0.0;
    public double maxDisHs = 0.0;
    public String htmlReportFile = "";

    public BenchmarkMetrics() {}

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        appendJsonField(sb, "datasetName", datasetName, true);
        appendJsonField(sb, "safeFileName", safeFileName, true);
        appendJsonField(sb, "architecture", architecture, true);
        appendJsonField(sb, "minutil", minutil, true);
        appendJsonField(sb, "interval", interval, true);
        appendJsonField(sb, "windowSize", windowSize, true);
        appendJsonField(sb, "alpha", alpha, true);
        appendJsonField(sb, "maxPattern", maxPattern, true);
        appendJsonField(sb, "totalTransactions", totalTransactions, true);
        appendJsonField(sb, "totalCheckpoints", totalCheckpoints, true);
        appendJsonField(sb, "elapsedSeconds", elapsedSeconds, true);
        appendJsonField(sb, "avgTps", avgTps, true);
        appendJsonField(sb, "peakTps", peakTps, true);
        appendJsonField(sb, "currentRamMb", currentRamMb, true);
        appendJsonField(sb, "peakRamMb", peakRamMb, true);
        appendJsonField(sb, "maxRamMb", maxRamMb, true);
        appendJsonField(sb, "totalAllocatedObjects", totalAllocatedObjects, true);
        appendJsonField(sb, "gcCount", gcCount, true);
        appendJsonField(sb, "gcTimeMs", gcTimeMs, true);
        appendJsonField(sb, "gcOverheadPct", gcOverheadPct, true);
        appendJsonField(sb, "avgCpMs", avgCpMs, true);
        appendJsonField(sb, "distinctHuis", distinctHuis, true);
        appendJsonField(sb, "totalHuis", totalHuis, true);
        appendJsonField(sb, "avgHuiPerCp", avgHuiPerCp, true);
        appendJsonField(sb, "maxHuiPerCp", maxHuiPerCp, true);
        appendJsonField(sb, "topItemset", topItemset, true);
        appendJsonField(sb, "topUtility", topUtility, true);
        appendJsonField(sb, "globalDrifts", globalDrifts, true);
        appendJsonField(sb, "localDrifts", localDrifts, true);
        appendJsonField(sb, "driftRatePct", driftRatePct, true);
        appendJsonField(sb, "avgDisHs", avgDisHs, true);
        appendJsonField(sb, "maxDisHs", maxDisHs, true);
        appendJsonField(sb, "htmlReportFile", htmlReportFile, false);
        sb.append("\n}");
        return sb.toString();
    }

    private static void appendJsonField(StringBuilder sb, String key, Object val, boolean comma) {
        sb.append("  \"").append(key).append("\": ");
        if (val instanceof Number || val instanceof Boolean) {
            sb.append(val);
        } else {
            String s = val == null ? "" : val.toString().replace("\"", "\\\"");
            sb.append("\"").append(s).append("\"");
        }
        if (comma) sb.append(",");
        sb.append("\n");
    }

    public static BenchmarkMetrics fromJson(String json) {
        BenchmarkMetrics m = new BenchmarkMetrics();
        if (json == null || json.trim().isEmpty()) return m;

        String[] lines = json.split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.startsWith("{") || line.startsWith("}")) continue;
            int colonIdx = line.indexOf(':');
            if (colonIdx <= 0) continue;

            String key = line.substring(0, colonIdx).replace("\"", "").trim();
            String val = line.substring(colonIdx + 1).trim();
            if (val.endsWith(",")) val = val.substring(0, val.length() - 1).trim();
            if (val.startsWith("\"") && val.endsWith("\"")) {
                val = val.substring(1, val.length() - 1);
            }

            try {
                switch (key) {
                    case "datasetName": m.datasetName = val; break;
                    case "safeFileName": m.safeFileName = val; break;
                    case "architecture": m.architecture = val; break;
                    case "minutil": m.minutil = Double.parseDouble(val); break;
                    case "interval": m.interval = Integer.parseInt(val); break;
                    case "windowSize": m.windowSize = Integer.parseInt(val); break;
                    case "alpha": m.alpha = Double.parseDouble(val); break;
                    case "maxPattern": m.maxPattern = Integer.parseInt(val); break;
                    case "totalTransactions": m.totalTransactions = Integer.parseInt(val); break;
                    case "totalCheckpoints": m.totalCheckpoints = Integer.parseInt(val); break;
                    case "elapsedSeconds": m.elapsedSeconds = Double.parseDouble(val); break;
                    case "avgTps": m.avgTps = Integer.parseInt(val); break;
                    case "peakTps": m.peakTps = Integer.parseInt(val); break;
                    case "currentRamMb": m.currentRamMb = Double.parseDouble(val); break;
                    case "peakRamMb": m.peakRamMb = Double.parseDouble(val); break;
                    case "maxRamMb": m.maxRamMb = Double.parseDouble(val); break;
                    case "totalAllocatedObjects": m.totalAllocatedObjects = Long.parseLong(val); break;
                    case "gcCount": m.gcCount = Long.parseLong(val); break;
                    case "gcTimeMs": m.gcTimeMs = Long.parseLong(val); break;
                    case "gcOverheadPct": m.gcOverheadPct = Double.parseDouble(val); break;
                    case "avgCpMs": m.avgCpMs = Double.parseDouble(val); break;
                    case "distinctHuis": m.distinctHuis = Integer.parseInt(val); break;
                    case "totalHuis": m.totalHuis = Integer.parseInt(val); break;
                    case "avgHuiPerCp": m.avgHuiPerCp = Double.parseDouble(val); break;
                    case "maxHuiPerCp": m.maxHuiPerCp = Integer.parseInt(val); break;
                    case "topItemset": m.topItemset = val; break;
                    case "topUtility": m.topUtility = Double.parseDouble(val); break;
                    case "globalDrifts": m.globalDrifts = Integer.parseInt(val); break;
                    case "localDrifts": m.localDrifts = Integer.parseInt(val); break;
                    case "driftRatePct": m.driftRatePct = Double.parseDouble(val); break;
                    case "avgDisHs": m.avgDisHs = Double.parseDouble(val); break;
                    case "maxDisHs": m.maxDisHs = Double.parseDouble(val); break;
                    case "htmlReportFile": m.htmlReportFile = val; break;
                }
            } catch (Exception ignored) {}
        }
        return m;
    }
}
