package test;

import huddtds.application.SimulationService;
import huddtds.application.event.EventType;
import huddtds.application.event.SimulationEvent;
import huddtds.algorithm.HUDD_TDS;
import huddtds.algorithm.drift.GlobalDriftStrategy;
import huddtds.algorithm.drift.LocalDriftStrategy;
import huddtds.algorithm.mining.HUIItemsetMiner;
import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;
import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class SimulationServiceEventTest {
    public static void main(String[] args) {
        SimulationService service = new SimulationService(new HUDD_TDS(
                Collections.<String, Double>emptyMap(), 1.0, 1, 2, 0.05,
                new EmptyMiner(), new FixedGlobalDrift(), new FixedLocalDrift()));
        List<SimulationEvent> events = new ArrayList<>();
        huddtds.application.event.SimulationListener listener = events::add;
        service.addListener(listener);

        Transaction first = service.processLine("a:1", 1);
        Transaction second = service.processLine("a:2", 2);
        service.publishProgress(2, 2, 10);
        service.finish(2);
        service.reportError(new IllegalArgumentException("bad input"), 3);

        require(first.getTid() == 1 && second.getTid() == 2,
                "The service should parse and process each transaction");
        require(events.stream().anyMatch(event ->
                        event.getType() == EventType.CHECKPOINT_CREATED
                                && event.getCheckpoint() != null
                                && event.getTransaction().getTid() == 2),
                "Checkpoint events should include their transaction and checkpoint");
        require(events.stream().anyMatch(event ->
                        event.getType() == EventType.TRANSACTION_PROCESSED
                                && event.getTid() == 2
                                && event.getTotalEstimate() == 2),
                "Progress events should expose progress metadata");
        require(events.stream().anyMatch(event ->
                        event.getType() == EventType.SIMULATION_FINISHED && event.getTid() == 2),
                "The service should publish completion");
        require(events.stream().anyMatch(event ->
                        event.getType() == EventType.GLOBAL_DRIFT
                                && event.getDriftResult().getType()
                                == DriftResult.DriftType.GLOBAL_DRIFT
                                && event.getGlobalDriftMessage().contains("TĂNG")),
                "The service should publish typed global drift events");
        require(events.stream().anyMatch(event ->
                        event.getType() == EventType.LOCAL_DRIFT
                                && event.getDriftResult().getAffectedItemsets().contains("[a]")),
                "The service should publish typed local drift events");
        require(events.get(events.size() - 1).getType() == EventType.SIMULATION_ERROR
                        && events.get(events.size() - 1).getMessage().contains("bad input"),
                "The service should publish explicit error events");

        service.removeListener(listener);
        int eventCount = events.size();
        service.finish(4);
        require(events.size() == eventCount, "Removed listeners should receive no future events");

        List<EventType> observedAfterFailure = new ArrayList<>();
        huddtds.application.event.SimulationListener failingListener = event -> {
            throw new IllegalStateException("subscriber failed");
        };
        huddtds.application.event.SimulationListener laterListener =
                event -> observedAfterFailure.add(event.getType());
        service.addListener(failingListener);
        service.addListener(laterListener);
        try {
            service.finish(5);
            throw new AssertionError("Subscriber failures should be propagated");
        } catch (IllegalStateException expected) {
            require("subscriber failed".equals(expected.getMessage()),
                    "The original subscriber failure should be preserved");
        }
        require(observedAfterFailure.contains(EventType.SIMULATION_FINISHED),
                "A failing observer must not prevent dispatch to later observers");
        service.removeListener(failingListener);
        service.removeListener(laterListener);

        SimulationService.Subscription subscription = service.subscribe(
                event -> observedAfterFailure.add(event.getType()));
        int observedBeforeUnsubscribe = observedAfterFailure.size();
        subscription.close();
        subscription.close();
        service.finish(6);
        require(observedAfterFailure.size() == observedBeforeUnsubscribe,
                "Closing a subscription should unsubscribe idempotently");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static class EmptyMiner implements HUIItemsetMiner {
        @Override
        public List<HighUtilityItemset> discover(List<Transaction> memory, int currentTid) {
            return Collections.emptyList();
        }

        @Override
        public void setTraceListener(Consumer<String> traceListener) {
        }
    }

    private static class FixedGlobalDrift implements GlobalDriftStrategy {
        @Override
        public DriftResult updateAndCheck(double observation, int oldTid, int newTid) {
            return DriftResult.globalDrift(oldTid, newTid, 0.75, 0.25, "TĂNG");
        }

        @Override
        public void setTraceListener(Consumer<String> traceListener) {
        }
    }

    private static class FixedLocalDrift implements LocalDriftStrategy {
        @Override
        public DriftResult detect(Checkpoint previous, Checkpoint current) {
            return DriftResult.localDrift(previous.getTid(), current.getTid(),
                    2.0, 1.0, "[a]");
        }

        @Override
        public void setTraceListener(Consumer<String> traceListener) {
        }
    }
}
