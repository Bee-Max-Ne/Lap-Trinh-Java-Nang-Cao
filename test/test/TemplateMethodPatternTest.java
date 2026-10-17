package test;

import huddtds.algorithm.drift.AbstractDriftDetector;
import huddtds.model.DriftResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test class for Template Method pattern in drift detection.
 * Standalone runner (không phụ thuộc external JUnit jar).
 */
public class TemplateMethodPatternTest {

    public static void main(String[] args) {
        System.out.println("=== Chạy TemplateMethodPatternTest ===");
        testTemplateMethodFlow();
        testTemplateMethodFlowNotDetected();
        testTemplateMethodFlowNotEnoughData();
        System.out.println("TemplateMethodPatternTest PASSED ✓");
    }

    private static class TestDriftDetector extends AbstractDriftDetector {
        private final List<String> callLog = new ArrayList<>();
        private boolean hasEnoughDataResult = true;
        private double computeStatisticResult = 1.5;
        private double computeEpsilonResult = 0.5;
        private boolean detected = false;

        @Override
        protected boolean hasEnoughData(AbstractDriftDetector.DriftInput input) {
            callLog.add("hasEnoughData");
            return hasEnoughDataResult;
        }

        @Override
        protected double computeStatistic(AbstractDriftDetector.DriftInput input) {
            callLog.add("computeStatistic");
            return computeStatisticResult;
        }

        @Override
        protected double computeEpsilon(AbstractDriftDetector.DriftInput input) {
            callLog.add("computeEpsilon");
            return computeEpsilonResult;
        }

        @Override
        protected void traceResult(AbstractDriftDetector.DriftInput input, double statistic, double epsilon, boolean detected) {
            callLog.add("traceResult");
            this.detected = detected;
        }

        @Override
        protected DriftResult buildDetectedResult(AbstractDriftDetector.DriftInput input, double statistic, double epsilon) {
            callLog.add("buildDetectedResult");
            return new DriftResult(true, DriftResult.DriftType.GLOBAL_DRIFT, 0, 1, statistic, epsilon, "DESC", "DIR", java.util.Collections.emptyList());
        }

        @Override
        protected DriftResult buildNoDriftResult(AbstractDriftDetector.DriftInput input) {
            callLog.add("buildNoDriftResult");
            return new DriftResult(false, DriftResult.DriftType.NONE, 0, 1, 0.0, 0.0, "DESC", null, java.util.Collections.emptyList());
        }

        public List<String> getCallLog() {
            return callLog;
        }

        public void setHasEnoughDataResult(boolean result) {
            this.hasEnoughDataResult = result;
        }

        public void setComputeStatisticResult(double result) {
            this.computeStatisticResult = result;
        }

        public void setComputeEpsilonResult(double result) {
            this.computeEpsilonResult = result;
        }
    }

    public static void testTemplateMethodFlow() {
        System.out.print("1. Kiểm tra luồng Template Method khi phát hiện trôi dạt... ");
        TestDriftDetector detector = new TestDriftDetector();
        AbstractDriftDetector.DriftInput input = new AbstractDriftDetector.DriftInput() {};

        DriftResult result = detector.runDetection(input);

        List<String> expectedCallOrder = Arrays.asList(
                "hasEnoughData",
                "computeStatistic",
                "computeEpsilon",
                "traceResult",
                "buildDetectedResult"
        );

        require(expectedCallOrder.equals(detector.getCallLog()), "Call order should match the template method steps");
        require(result.isDetected(), "Result should be detected because |statistic| >= epsilon");
        require(result.getType() == DriftResult.DriftType.GLOBAL_DRIFT, "Drift type should be GLOBAL_DRIFT");
        System.out.println("OK");
    }

    public static void testTemplateMethodFlowNotDetected() {
        System.out.print("2. Kiểm tra luồng Template Method khi không phát hiện trôi dạt... ");
        TestDriftDetector detector = new TestDriftDetector();
        detector.setComputeStatisticResult(0.3);
        detector.setComputeEpsilonResult(0.5);

        AbstractDriftDetector.DriftInput input = new AbstractDriftDetector.DriftInput() {};
        DriftResult result = detector.runDetection(input);

        List<String> expectedCallOrder = Arrays.asList(
                "hasEnoughData",
                "computeStatistic",
                "computeEpsilon",
                "traceResult",
                "buildNoDriftResult"
        );

        require(expectedCallOrder.equals(detector.getCallLog()), "Call order should match template method steps when not detected");
        require(!result.isDetected(), "Result should not be detected");
        require(result.getType() == DriftResult.DriftType.NONE, "Drift type should be NONE");
        System.out.println("OK");
    }

    public static void testTemplateMethodFlowNotEnoughData() {
        System.out.print("3. Kiểm tra dừng sớm khi không đủ dữ liệu (short-circuit)... ");
        TestDriftDetector detector = new TestDriftDetector();
        detector.setHasEnoughDataResult(false);

        AbstractDriftDetector.DriftInput input = new AbstractDriftDetector.DriftInput() {};
        DriftResult result = detector.runDetection(input);

        List<String> expectedCallOrder = Arrays.asList(
                "hasEnoughData",
                "buildNoDriftResult"
        );

        require(expectedCallOrder.equals(detector.getCallLog()), "Call order should short-circuit when not enough data");
        require(!result.isDetected(), "Result should not be detected");
        System.out.println("OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}