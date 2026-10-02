package huddtds.algorithm;

import huddtds.algorithm.drift.LocalDriftStrategy;
import huddtds.math.UtilityMetrics;
import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;
import huddtds.model.HighUtilityItemset;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class LocalDriftDetector implements LocalDriftStrategy {

    private final double alpha;
    private final int sampleSize;
    private Consumer<String> traceListener;

    public LocalDriftDetector(double alpha, double range) {
        this(alpha, range, 1);
    }

    public LocalDriftDetector(double alpha, double range, int sampleSize) {
        this.alpha = alpha;
        this.sampleSize = Math.max(1, sampleSize);
        this.traceListener = null;
    }

    @Override
    public void setTraceListener(Consumer<String> traceListener) {
        this.traceListener = traceListener;
    }

    private void trace(String message) {
        if (traceListener != null) {
            traceListener.accept(message);
        } else {
            System.out.println(message);
        }
    }

    public String detect(List<Checkpoint> checkpoints) {
        if (checkpoints == null || checkpoints.size() < 2) {
            return null;
        }

        Checkpoint previous = checkpoints.get(checkpoints.size() - 2);
        Checkpoint current = checkpoints.get(checkpoints.size() - 1);
        DriftResult result = detect(previous, current);
        return result.isDetected() ? result.getAffectedItemsets().get(0) : null;
    }

    @Override
    public DriftResult detect(Checkpoint previous, Checkpoint current) {
        Set<String> candidateItemsets = collectItemsets(previous, current);
        if (candidateItemsets.isEmpty()) {
            trace(String.format("[LOCAL CHECK] T%d -> T%d; no itemsets to compare",
                previous.getTid(), current.getTid()));
            return DriftResult.noDrift(previous.getTid(), current.getTid());
        }

        int n1 = sampleSize;
        int n2 = sampleSize;
        int hypothesisCount = candidateItemsets.size();
        double adjustedAlpha = UtilityMetrics.bonferroniAdjustedAlpha(alpha, hypothesisCount);
        trace(String.format("[LOCAL CHECK] T%d -> T%d; hypotheses=%d, alpha'=%.8f",
            previous.getTid(), current.getTid(), hypothesisCount, adjustedAlpha));

        int testedItemsets = 0;
        for (String itemsetKey : candidateItemsets) {
            Set<String> itemset = parseItemsetKey(itemsetKey);
            double x1 = utilityAt(previous, itemset);
            double x2 = utilityAt(current, itemset);
            double[] observations = {x1, x2};
            double variance = UtilityMetrics.variance(observations);
            double epsilon = UtilityMetrics.localDriftEpsilon(n1, n2, variance, adjustedAlpha);
            double difference = Math.abs(x1 - x2);

            if (testedItemsets < 25) {
                trace(String.format(
                        "[LOCAL CHECK] T%d -> T%d, X=%s, n1=%d, n2=%d, X1=%.6f, X2=%.6f, " +
                                "variance=%.6f, alpha'=%.8f, epsilon=%.6f, |X1-X2|=%.6f",
                        previous.getTid(), current.getTid(), itemsetKey, n1, n2, x1, x2,
                        variance, adjustedAlpha, epsilon, difference));
            } else if (testedItemsets == 25) {
                trace(String.format("[LOCAL CHECK] Suppressing diagnostics for %d additional itemset(s)",
                        hypothesisCount - testedItemsets));
            }
            testedItemsets++;

            if (difference >= epsilon) {
                trace("[LOCAL DRIFT DETECTED] X=" + itemsetKey);
                return DriftResult.localDrift(
                        previous.getTid(),
                        current.getTid(),
                        difference,
                        epsilon,
                        itemsetKey);
            }
        }

        return DriftResult.noDrift(previous.getTid(), current.getTid());
    }

    private Set<String> collectItemsets(
            Checkpoint previous,
            Checkpoint current) {

        Set<String> result = new HashSet<>();

        for (HighUtilityItemset hui : previous.getHuis()) {
            result.add(toItemsetKey(hui.getItems()));
        }

        for (HighUtilityItemset hui : current.getHuis()) {
            result.add(toItemsetKey(hui.getItems()));
        }

        return result;
    }

    private double utilityAt(
            Checkpoint checkpoint,
            Set<String> targetItemset) {

        for (HighUtilityItemset hui : checkpoint.getHuis()) {

            if (hui.getItems().equals(targetItemset)) {
                return hui.getTotalUtility();
            }
        }

        return 0.0;
    }

    private String toItemsetKey(Set<String> items) {

        return items.stream()
                .sorted()
                .toList()
                .toString();
    }

    private Set<String> parseItemsetKey(String key) {

        Set<String> result = new HashSet<>();

        String value = key.trim();

        if (value.length() < 2) {
            return result;
        }

        value =
                value.substring(
                        1,
                        value.length() - 1
                ).trim();

        if (value.isEmpty()) {
            return result;
        }

        String[] parts = value.split(",");

        for (String part : parts) {
            result.add(part.trim());
        }

        return result;
    }

    public boolean detectSameItemset(
            Set<String> targetSet,
            List<?> window,
            int currentTid,
            Map<String, Double> externalUtilities,
            HighUtilityItemset previousHui,
            HighUtilityItemset currentHui) {

        if (previousHui == null || currentHui == null) {
            return false;
        }

        if (!previousHui.getItems().equals(
                currentHui.getItems())) {
            return false;
        }

        return Math.abs(
                currentHui.getTotalUtility()
                        - previousHui.getTotalUtility())
                > 0.0;
    }
}
