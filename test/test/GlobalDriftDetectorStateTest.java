package test;

import huddtds.algorithm.GlobalDriftDetector;
import huddtds.math.UtilityMetrics;
import huddtds.model.DriftResult;

import java.util.ArrayList;
import java.util.List;

public class GlobalDriftDetectorStateTest {
    public static void main(String[] args) {
        GlobalDriftDetector actual = new GlobalDriftDetector(0.05, 1.0);
        actual.setTraceListener(message -> { });
        ReferenceDetector reference = new ReferenceDetector(0.05, 1.0);

        for (int index = 1; index <= 2500; index++) {
            double observation = index < 1200
                    ? (index % 17) / 20.0
                    : 0.6 + (index % 23) / 25.0;
            DriftResult actualResult = actual.updateAndCheck(observation, index - 1, index);
            DriftResult expectedResult = reference.updateAndCheck(observation, index - 1, index);

            require(actualResult.getType() == expectedResult.getType(),
                    "Drift type differs at observation " + index);
            require(actualResult.getStatistic() == expectedResult.getStatistic(),
                    "Drift statistic differs at observation " + index);
            require(actualResult.getThreshold() == expectedResult.getThreshold(),
                    "Drift threshold differs at observation " + index);
            require(equal(actualResult.getDirection(), expectedResult.getDirection()),
                    "Drift direction differs at observation " + index);
            require(actual.getObservationCount() == reference.observations.size(),
                    "Observation count differs at observation " + index);
            require(actual.getCutPoint() == reference.cutPoint,
                    "Cut point differs at observation " + index);
            require(actual.getCurrentMean() == reference.currentMean(),
                    "Current mean differs at observation " + index);
            require(actual.getReferenceMean() == reference.referenceMean(),
                    "Reference mean differs at observation " + index);
        }

        actual.reset();
        require(actual.getObservationCount() == 0
                        && actual.getCurrentMean() == 0.0
                        && actual.getReferenceMean() == 0.0
                        && actual.getCutPoint() == 0,
                "Reset should clear all accumulated detector state");

        System.out.println("GlobalDriftDetectorStateTest PASSED");
    }

    private static boolean equal(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class ReferenceDetector {
        private final double alpha;
        private final double range;
        private final List<Double> observations = new ArrayList<>();
        private int cutPoint;
        private String direction;

        private ReferenceDetector(double alpha, double range) {
            this.alpha = alpha;
            this.range = range;
        }

        private DriftResult updateAndCheck(double observation, int oldTid, int newTid) {
            observations.add(observation);
            direction = null;
            int n = observations.size();
            if (n < 2) {
                return DriftResult.noDrift(oldTid, newTid);
            }
            if (cutPoint == 0) {
                cutPoint = 1;
            }
            if (cutPoint >= n) {
                cutPoint = n - 1;
            }

            int m = cutPoint;
            double uDrift = mean(0, m);
            double v = mean(0, n);
            double epsilonU = UtilityMetrics.hoeffdingBound(range, alpha, m);
            double epsilonV = UtilityMetrics.hoeffdingBound(range, alpha, n);
            boolean increasing = uDrift + epsilonU >= v + epsilonV;
            boolean decreasing = uDrift - epsilonU <= v - epsilonV;
            if (increasing) {
                direction = "TĂNG";
            } else if (decreasing) {
                direction = "GIẢM";
            }

            double epsilon = UtilityMetrics.globalDriftEpsilon(n, m, alpha, range);
            double difference = Math.abs(uDrift - v);
            if (difference >= epsilon) {
                if (direction == null) {
                    if (uDrift > v) {
                        direction = "TĂNG";
                    } else if (uDrift < v) {
                        direction = "GIẢM";
                    }
                }
                cutPoint = n;
                return DriftResult.globalDrift(oldTid, newTid, difference, epsilon, direction);
            }
            if (increasing || decreasing) {
                cutPoint = n;
            }
            return DriftResult.noDrift(oldTid, newTid);
        }

        private double currentMean() {
            return mean(0, observations.size());
        }

        private double referenceMean() {
            if (observations.isEmpty()) {
                return 0.0;
            }
            int start = Math.min(Math.max(cutPoint, 0), observations.size() - 1);
            return mean(start, observations.size());
        }

        private double mean(int fromInclusive, int toExclusive) {
            if (fromInclusive >= toExclusive) {
                return 0.0;
            }
            double sum = 0.0;
            for (int index = fromInclusive; index < toExclusive; index++) {
                sum += observations.get(index);
            }
            return sum / (toExclusive - fromInclusive);
        }
    }
}
