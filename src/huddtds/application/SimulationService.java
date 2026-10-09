package huddtds.application;

import huddtds.algorithm.HUDD_TDS;
import huddtds.application.event.EventType;
import huddtds.application.event.SimulationEvent;
import huddtds.application.event.SimulationListener;
import huddtds.data.TransactionParser;
import huddtds.math.UtilityMetrics;
import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;
import huddtds.model.HighUtilityItemset;
import huddtds.model.ItemsetVector;
import huddtds.model.Transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Application boundary for processing transactions and publishing simulation events.
 */
public class SimulationService {
    private final HUDD_TDS engine;
    private final List<SimulationListener> listeners = new CopyOnWriteArrayList<>();

    public SimulationService(Map<String, Double> externalUtilities,
                             double minutil,
                             int interval,
                             int windowSize,
                             double alphaConfidence,
                             int maxItemsetSize) {
        this(new HUDD_TDS(externalUtilities, minutil, interval, windowSize,
                alphaConfidence, maxItemsetSize));
    }

    public SimulationService(HUDD_TDS engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    public void addListener(SimulationListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public Subscription subscribe(SimulationListener listener) {
        SimulationListener registeredListener = Objects.requireNonNull(listener, "listener");
        addListener(registeredListener);
        AtomicBoolean subscribed = new AtomicBoolean(true);
        return () -> {
            if (subscribed.compareAndSet(true, false)) {
                removeListener(registeredListener);
            }
        };
    }

    public void removeListener(SimulationListener listener) {
        listeners.remove(listener);
    }

    public void setTraceListener(Consumer<String> listener, boolean traceEachTransaction) {
        engine.setTraceListener(listener, traceEachTransaction);
    }

    public Transaction processLine(String line, int tid) {
        Transaction transaction = TransactionParser.parseLine(line, tid);
        Checkpoint checkpoint = engine.processTransaction(transaction);
        if (checkpoint == null) {
            return transaction;
        }

        DriftResult globalDrift = engine.checkGlobalDriftResult();
        DriftResult localDrift = engine.checkLocalDriftResult();
        List<String> itemsetVectors = buildItemsetVectors(checkpoint);
        String globalMessage = formatGlobalDrift(globalDrift);
        String localMessage = formatLocalDrift(localDrift);

        publish(new SimulationEvent.Builder()
                .withType(EventType.CHECKPOINT_CREATED)
                .withTid(tid)
                .withTransaction(transaction)
                .withCheckpoint(checkpoint)
                .withGlobalDrift(globalDrift)
                .withLocalDrift(localDrift)
                .withItemsetVectors(itemsetVectors)
                .withGlobalDriftMessage(globalMessage)
                .withLocalDriftMessage(localMessage)
                .build());
        if (globalDrift.isDetected()) {
            publish(new SimulationEvent.Builder()
                    .withType(EventType.GLOBAL_DRIFT)
                    .withTid(tid)
                    .withTransaction(transaction)
                    .withCheckpoint(checkpoint)
                    .withDriftResult(globalDrift)
                    .withGlobalDrift(globalDrift)
                    .withLocalDrift(localDrift)
                    .withItemsetVectors(itemsetVectors)
                    .withMessage(globalMessage)
                    .withGlobalDriftMessage(globalMessage)
                    .withLocalDriftMessage(localMessage)
                    .build());
        }
        if (localDrift.isDetected()) {
            publish(new SimulationEvent.Builder()
                    .withType(EventType.LOCAL_DRIFT)
                    .withTid(tid)
                    .withTransaction(transaction)
                    .withCheckpoint(checkpoint)
                    .withDriftResult(localDrift)
                    .withGlobalDrift(globalDrift)
                    .withLocalDrift(localDrift)
                    .withItemsetVectors(itemsetVectors)
                    .withMessage(localMessage)
                    .withGlobalDriftMessage(globalMessage)
                    .withLocalDriftMessage(localMessage)
                    .build());
        }
        return transaction;
    }

    public void publishProgress(int tid, int totalEstimate, int speedTxPerSec) {
        publish(new SimulationEvent.Builder()
                .withType(EventType.TRANSACTION_PROCESSED)
                .withTid(tid)
                .withTotalEstimate(totalEstimate)
                .withSpeedTxPerSec(speedTxPerSec)
                .build());
    }

    public void finish(int tid) {
        publish(new SimulationEvent.Builder()
                .withType(EventType.SIMULATION_FINISHED)
                .withTid(tid)
                .build());
    }

    public void reportError(Throwable error, int tid) {
        Throwable reportedError = Objects.requireNonNull(error, "error");
        publish(new SimulationEvent.Builder()
                .withType(EventType.SIMULATION_ERROR)
                .withTid(tid)
                .withMessage(reportedError.getClass().getSimpleName() + ": " + reportedError.getMessage())
                .build());
    }

    private void publish(SimulationEvent event) {
        RuntimeException dispatchFailure = null;
        for (SimulationListener listener : listeners) {
            try {
                listener.onUpdate(event);
            } catch (RuntimeException failure) {
                if (dispatchFailure == null) {
                    dispatchFailure = failure;
                } else if (dispatchFailure != failure) {
                    dispatchFailure.addSuppressed(failure);
                }
            }
        }
        if (dispatchFailure != null) {
            throw dispatchFailure;
        }
    }

    private List<String> buildItemsetVectors(Checkpoint checkpoint) {
        List<String> vectors = new ArrayList<>(checkpoint.getHuis().size());
        List<String> distinctItems = new ArrayList<>();
        for (HighUtilityItemset hui : checkpoint.getHuis()) {
            distinctItems.addAll(hui.getItems());
            ItemsetVector vector = UtilityMetrics.buildItemsetVector(
                    hui, new ArrayList<>(new HashSet<>(distinctItems)));
            vectors.add(vector.toString());
        }
        return vectors;
    }

    private String formatGlobalDrift(DriftResult result) {
        if (!result.isDetected()) {
            return null;
        }
        List<Checkpoint> checkpoints = engine.getCheckpoints();
        Checkpoint previous = checkpoints.get(checkpoints.size() - 2);
        Checkpoint current = checkpoints.get(checkpoints.size() - 1);
        return String.format("GLOBAL DRIFT (%s) [Δ=%.4f]",
                result.getDirection(),
                Math.abs(current.getGlobalDistance() - previous.getGlobalDistance()));
    }

    private String formatLocalDrift(DriftResult result) {
        if (!result.isDetected()) {
            return null;
        }
        if (result.getAffectedItemsets().isEmpty()) {
            throw new IllegalStateException("Detected local drift must identify an affected itemset");
        }
        return "LOCAL DRIFT (" + result.getAffectedItemsets().get(0) + ")";
    }

    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }
}
