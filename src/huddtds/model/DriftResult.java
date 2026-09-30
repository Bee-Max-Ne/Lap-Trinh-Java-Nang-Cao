package huddtds.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mô hình hóa kết quả kiểm định trôi dạt (Drift Result).
 */
public class DriftResult {
    public enum DriftType {
        NONE,
        GLOBAL_DRIFT,
        LOCAL_DRIFT
    }

    private final boolean detected;
    private final DriftType type;
    private final int oldCheckpointTid;
    private final int newCheckpointTid;
    private final double statistic;
    private final double threshold;
    private final String description;
    private final List<String> affectedItemsets;

    public DriftResult(boolean detected,
                       DriftType type,
                       int oldCheckpointTid,
                       int newCheckpointTid,
                       double statistic,
                       double threshold,
                       String description,
                       List<String> affectedItemsets) {
        this.detected = detected;
        this.type = type;
        this.oldCheckpointTid = oldCheckpointTid;
        this.newCheckpointTid = newCheckpointTid;
        this.statistic = statistic;
        this.threshold = threshold;
        this.description = description;
        this.affectedItemsets = (affectedItemsets != null)
                ? new ArrayList<>(affectedItemsets)
                : Collections.emptyList();
    }

    public static DriftResult noDrift(int oldTid, int newTid) {
        return new DriftResult(false, DriftType.NONE, oldTid, newTid, 0.0, 0.0, "Ổn định (Không drift)", Collections.emptyList());
    }

    public boolean isDetected() {
        return detected;
    }

    public DriftType getType() {
        return type;
    }

    public int getOldCheckpointTid() {
        return oldCheckpointTid;
    }

    public int getNewCheckpointTid() {
        return newCheckpointTid;
    }

    public double getStatistic() {
        return statistic;
    }

    public double getThreshold() {
        return threshold;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAffectedItemsets() {
        return affectedItemsets;
    }

    @Override
    public String toString() {
        if (!detected) {
            return "NO DRIFT (T" + oldCheckpointTid + " -> T" + newCheckpointTid + ")";
        }
        return type + " (T" + oldCheckpointTid + " -> T" + newCheckpointTid + "): " + description;
    }
}
