package huddtds.application.event;

import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;
import huddtds.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SimulationEvent {
    private final EventType type;
    private final int tid;
    private final Transaction transaction;
    private final Checkpoint checkpoint;
    private final DriftResult driftResult;
    private final DriftResult globalDrift;
    private final DriftResult localDrift;
    private final List<String> itemsetVectors;
    private final int totalEstimate;
    private final int speedTxPerSec;
    private final String message;
    private final String globalDriftMessage;
    private final String localDriftMessage;

    public SimulationEvent(EventType type,
                           int tid,
                           Transaction transaction,
                           Checkpoint checkpoint,
                           DriftResult driftResult,
                           DriftResult globalDrift,
                           DriftResult localDrift,
                           List<String> itemsetVectors,
                           int totalEstimate,
                           int speedTxPerSec,
                           String message,
                           String globalDriftMessage,
                           String localDriftMessage) {
        this.type = type;
        this.tid = tid;
        this.transaction = transaction;
        this.checkpoint = checkpoint;
        this.driftResult = driftResult;
        this.globalDrift = globalDrift;
        this.localDrift = localDrift;
        this.itemsetVectors = itemsetVectors == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(itemsetVectors));
        this.totalEstimate = totalEstimate;
        this.speedTxPerSec = speedTxPerSec;
        this.message = message;
        this.globalDriftMessage = globalDriftMessage;
        this.localDriftMessage = localDriftMessage;
    }

    public EventType getType() {
        return type;
    }

    public int getTid() {
        return tid;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Checkpoint getCheckpoint() {
        return checkpoint;
    }

    public DriftResult getDriftResult() {
        return driftResult;
    }

    public DriftResult getGlobalDrift() {
        return globalDrift;
    }

    public DriftResult getLocalDrift() {
        return localDrift;
    }

    public List<String> getItemsetVectors() {
        return itemsetVectors;
    }

    public int getTotalEstimate() {
        return totalEstimate;
    }

    public int getSpeedTxPerSec() {
        return speedTxPerSec;
    }

    public String getMessage() {
        return message;
    }

    public String getGlobalDriftMessage() {
        return globalDriftMessage;
    }

    public String getLocalDriftMessage() {
        return localDriftMessage;
    }
}
