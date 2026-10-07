package test;

import huddtds.algorithm.HUDD_TDS;
import huddtds.application.event.EventType;
import huddtds.application.event.SimulationEvent;
import huddtds.model.Transaction;

import java.util.Collections;
import java.util.Map;

/**
 * Unit Test verifying Builder Pattern implementation for HUDD_TDS and SimulationEvent.
 */
public class BuilderPatternTest {
    public static void main(String[] args) {
        testHuddTdsBuilder();
        testHuddTdsBuilderValidation();
        testSimulationEventBuilder();

        System.out.println("BuilderPatternTest PASSED ✓");
    }

    private static void testHuddTdsBuilder() {
        Map<String, Double> utilities = Collections.singletonMap("a", 5.0);
        HUDD_TDS engine = new HUDD_TDS.Builder()
                .withExternalUtilities(utilities)
                .withMinutil(15.0)
                .withInterval(10)
                .withWindowSize(20)
                .withAlphaConfidence(0.05)
                .withMaxItemsetSize(3)
                .build();

        require(engine != null, "HUDD_TDS engine created via Builder should not be null");
        require(engine.getMinutil() == 15.0, "minutil should match builder parameter");
        require(engine.getInterval() == 10, "interval should match builder parameter");
        require(engine.getWindowSize() == 20, "windowSize should match builder parameter");
        require(engine.getAlphaConfidence() == 0.05, "alphaConfidence should match builder parameter");
    }

    private static void testHuddTdsBuilderValidation() {
        // Validation Test 1: Negative minutil should throw IllegalArgumentException
        try {
            new HUDD_TDS.Builder().withMinutil(-5.0).build();
            throw new AssertionError("Should fail on negative minutil");
        } catch (IllegalArgumentException expected) {
            // Expected exception
        }

        // Validation Test 2: Invalid alphaConfidence should throw IllegalArgumentException
        try {
            new HUDD_TDS.Builder().withAlphaConfidence(1.5).build();
            throw new AssertionError("Should fail on alphaConfidence > 1");
        } catch (IllegalArgumentException expected) {
            // Expected exception
        }
    }

    private static void testSimulationEventBuilder() {
        Transaction tx = new Transaction(100);
        SimulationEvent event = new SimulationEvent.Builder()
                .withType(EventType.TRANSACTION_PROCESSED)
                .withTid(100)
                .withTransaction(tx)
                .withTotalEstimate(5000)
                .withSpeedTxPerSec(1200)
                .withMessage("Progress update")
                .build();

        require(event.getType() == EventType.TRANSACTION_PROCESSED, "Event type should match");
        require(event.getTid() == 100, "Event TID should match");
        require(event.getTotalEstimate() == 5000, "Total estimate should match");
        require(event.getSpeedTxPerSec() == 1200, "Speed should match");
        require("Progress update".equals(event.getMessage()), "Message should match");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
