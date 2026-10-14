package huddtds.algorithm;

import huddtds.algorithm.drift.AbstractDriftDetector;
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

/**
 * Local Utility Drift Detector.
 * 
 * Based on Algorithm 4 and Equation (9)
 * of HUDD-TDS.
 */
public class LocalDriftDetector extends AbstractDriftDetector implements LocalDriftStrategy {

    /**
     * Định nghĩa đầu vào cụ thể cho LocalDriftDetector.
     */
    public static class LocalDriftInput implements AbstractDriftDetector.DriftInput {
        private final Checkpoint previous;
        private final Checkpoint current;

        public LocalDriftInput(Checkpoint previous, Checkpoint current) {
            this.previous = previous;
            this.current = current;
        }

        public Checkpoint getPrevious() {
            return previous;
        }

        public Checkpoint getCurrent() {
            return current;
        }
    }

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

    // =========================================================================
    // Implementing the abstract methods from AbstractDriftDetector (Template Method)
    // =========================================================================

    @Override
    protected boolean hasEnoughData(AbstractDriftDetector.DriftInput input) {
        if (!(input instanceof LocalDriftInput lInput)) return false;
        return lInput.getPrevious() != null && lInput.getCurrent() != null
                && !lInput.getPrevious().getHuis().isEmpty() && !lInput.getCurrent().getHuis().isEmpty();
    }

    @Override
    protected double computeStatistic(AbstractDriftDetector.DriftInput input) {
        return 0.0;
    }

    @Override
    protected double computeEpsilon(AbstractDriftDetector.DriftInput input) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    protected void traceResult(AbstractDriftDetector.DriftInput input, double statistic, double epsilon, boolean detected) {
        LocalDriftInput lInput = (LocalDriftInput) input;
        String msg = String.format(
                "[LOCAL CHECK] TID=%d->%d, statistic=%.6f, epsilon=%.6f, detected=%b",
                lInput.getPrevious().getTid(),
                lInput.getCurrent().getTid(),
                statistic,
                epsilon,
                detected);
        if (traceListener != null) {
            traceListener.accept(msg);
        } else {
            System.out.println(msg);
        }
    }

    @Override
    protected DriftResult buildDetectedResult(AbstractDriftDetector.DriftInput input, double statistic, double epsilon) {
        LocalDriftInput lInput = (LocalDriftInput) input;
        String itemsetKey = "placeholder";
        if (!lInput.getPrevious().getHuis().isEmpty()) {
            itemsetKey = lInput.getPrevious().getHuis().get(0).getItems().toString();
        }
        return DriftResult.localDrift(
                lInput.getPrevious().getTid(),
                lInput.getCurrent().getTid(),
                Math.abs(statistic),
                epsilon,
                itemsetKey);
    }

    @Override
    protected DriftResult buildNoDriftResult(AbstractDriftDetector.DriftInput input) {
        LocalDriftInput lInput = (LocalDriftInput) input;
        return DriftResult.noDrift(lInput.getPrevious().getTid(), lInput.getCurrent().getTid());
    }

    // The following methods are kept for compatibility with the original LocalDriftDetector.
    // We leave them unchanged.

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