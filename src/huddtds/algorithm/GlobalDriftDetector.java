package huddtds.algorithm;

import huddtds.algorithm.drift.GlobalDriftStrategy;
import huddtds.math.UtilityMetrics;
import huddtds.model.DriftResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Global Utility Drift Detector.
 *
 * Based on Algorithm 3 and Equation (8)
 * of HUDD-TDS.
 *
 * Input:
 *   d1, d2, ..., dn
 *
 * Udrift:
 *   average of the observations up to the cut point.
 *
 * V:
 *   average of all observations.
 *
 * Equation (8):
 *
 * epsilon =
 * sqrt(
 *      (n - m) / (2*n*m)
 *      * ln(2/alpha)
 * )
 */
public class GlobalDriftDetector implements GlobalDriftStrategy {

    private final double alpha;
    private final double range;

    /**
     * Distance values received from checkpoints.
     */
    private final List<Double> distances;

    /**
     * Cut point m.
     *
     * m means that:
     *
     * Udrift = mean(d1 ... dm)
     */
    private int cutPoint;

    /**
     * Last detected trend.
     */
    private String lastDirection;
    private Consumer<String> traceListener;

    public GlobalDriftDetector(
            double alpha,
            double range) {

        this.alpha = alpha;
        this.range = range;

        this.distances =
                new ArrayList<>();

        this.cutPoint = 0;
        this.lastDirection = null;
        this.traceListener = null;
    }

    @Override
    public void setTraceListener(Consumer<String> traceListener) {
        this.traceListener = traceListener;
    }

    /**
     * Add one checkpoint distance and check global drift.
     */
    public String updateAndCheck(double observation) {
        DriftResult result = updateAndCheck(observation, -1, -1);
        return result.isDetected() ? result.getDirection() : null;
    }

    @Override
    public DriftResult updateAndCheck(
            double observation,
            int oldCheckpointTid,
            int newCheckpointTid) {
        distances.add(observation);

        lastDirection = null;

        int n = distances.size();

        /*
         * Need at least two observations
         * to create two groups.
         */
        if (n < 2) {
            return DriftResult.noDrift(oldCheckpointTid, newCheckpointTid);
        }

        /*
         * First cut point:
         *
         * d1 | d2
         *
         * m = 1
         */
        if (cutPoint == 0) {
            cutPoint = 1;
        }

        /*
         * A cut point must satisfy:
         *
         * 1 <= m < n
         */
        if (cutPoint >= n) {
            cutPoint = n - 1;
        }

        int m = cutPoint;

        /*
         * --------------------------------------------------
         * Udrift
         * --------------------------------------------------
         *
         * Mean of d1 ... dm.
         */
        double uDrift =
                mean(
                        0,
                        m
                );

        /*
         * --------------------------------------------------
         * V
         * --------------------------------------------------
         *
         * Mean of d1 ... dn.
         */
        double v =
                mean(
                        0,
                        n
                );

        /*
         * --------------------------------------------------
         * Hoeffding bounds
         * --------------------------------------------------
         */
        double epsilonU =
                UtilityMetrics.hoeffdingBound(
                        range,
                        alpha,
                        m
                );

        double epsilonV =
                UtilityMetrics.hoeffdingBound(
                        range,
                        alpha,
                        n
                );

        /*
         * Save the original statistical values.
         *
         * These are the values that must be used
         * for Equation (8).
         */
        double testU = uDrift;
        double testV = v;

        /*
         * --------------------------------------------------
         * Increasing / decreasing trend
         * --------------------------------------------------
         *
         * Algorithm 3 uses the one-sided Hoeffding
         * conditions to identify a trend and update
         * the cut point.
         *
         * We therefore use the trend information
         * only to determine the next cut point.
         *
         * We do NOT overwrite testU/testV before
         * performing the Equation (8) test.
         */

        boolean increasing =
                uDrift + epsilonU
                        >= v + epsilonV;

        boolean decreasing =
                uDrift - epsilonU
                        <= v - epsilonV;

        if (increasing) {
            lastDirection = "TĂNG";
        } else if (decreasing) {
            lastDirection = "GIẢM";
        }

        /*
         * --------------------------------------------------
         * Equation (8)
         * --------------------------------------------------
         */
        double epsilon =
                UtilityMetrics.globalDriftEpsilon(
                        n,
                        m,
                        alpha,
                        range
                );

        /*
         * --------------------------------------------------
         * Reject H0
         *
         * |Udrift - V| >= epsilon
         * --------------------------------------------------
         */
        double difference =
                Math.abs(
                        testU - testV
                );

        String diagnostic = String.format(
                "[GLOBAL CHECK] n=%d, m=%d, Udrift=%.6f, V=%.6f, " +
                        "epsilonU=%.6f, epsilonV=%.6f, " +
                        "epsilon=%.6f, |U-V|=%.6f",
                n,
                m,
                testU,
                testV,
                epsilonU,
                epsilonV,
                epsilon,
                difference);
        if (traceListener != null) {
            traceListener.accept(diagnostic);
        } else {
            System.out.println(diagnostic);
        }

        if (difference >= epsilon) {

            /*
             * If no one-sided trend was identified,
             * determine the direction from the means.
             */
            if (lastDirection == null) {

                if (testU > testV) {
                    lastDirection = "TĂNG";
                } else if (testU < testV) {
                    lastDirection = "GIẢM";
                }
            }

            /*
             * Drift detected.
             *
             * The current checkpoint becomes the
             * beginning of the next sequence.
             */
            cutPoint = n;

            return DriftResult.globalDrift(
                    oldCheckpointTid,
                    newCheckpointTid,
                    difference,
                    epsilon,
                    lastDirection);
        }

        /*
         * No drift.
         *
         * If a trend has been detected, move the
         * cut point forward so that subsequent
         * observations can use the newer reference.
         *
         * This corresponds to the "update cut point"
         * operation in Algorithm 3.
         */
        if (increasing || decreasing) {
            cutPoint = n;
        }

        return DriftResult.noDrift(oldCheckpointTid, newCheckpointTid);
    }

    /**
     * Calculate mean on:
     *
     * [fromInclusive, toExclusive)
     */
    private double mean(
            int fromInclusive,
            int toExclusive) {

        if (fromInclusive >= toExclusive) {
            return 0.0;
        }

        double sum = 0.0;

        for (int i = fromInclusive;
             i < toExclusive;
             i++) {

            sum += distances.get(i);
        }

        return sum /
                (toExclusive - fromInclusive);
    }

    public String getLastDirection() {
        return lastDirection;
    }

    public double getCurrentMean() {

        if (distances.isEmpty()) {
            return 0.0;
        }

        return mean(
                0,
                distances.size()
        );
    }

    public double getReferenceMean() {

        if (distances.isEmpty()) {
            return 0.0;
        }

        int start =
                Math.min(
                        Math.max(cutPoint, 0),
                        distances.size() - 1
                );

        return mean(
                start,
                distances.size()
        );
    }

    public int getObservationCount() {
        return distances.size();
    }

    public int getCutPoint() {
        return cutPoint;
    }

    public void reset() {

        distances.clear();

        cutPoint = 0;

        lastDirection = null;
    }
}
