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

    public SimulationEvent(Builder builder) {
        this.type = builder.type;
        this.tid = builder.tid;
        this.transaction = builder.transaction;
        this.checkpoint = builder.checkpoint;
        this.driftResult = builder.driftResult;
        this.globalDrift = builder.globalDrift;
        this.localDrift = builder.localDrift;
        this.itemsetVectors = builder.itemsetVectors == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(builder.itemsetVectors));
        this.totalEstimate = builder.totalEstimate;
        this.speedTxPerSec = builder.speedTxPerSec;
        this.message = builder.message;
        this.globalDriftMessage = builder.globalDriftMessage;
        this.localDriftMessage = builder.localDriftMessage;
    }

    /**
     * Builder Pattern for clean SimulationEvent construction without long positional parameter lists.
     */
    public static class Builder {
        private EventType type;
        private int tid;
        private Transaction transaction;
        private Checkpoint checkpoint;
        private DriftResult driftResult;
        private DriftResult globalDrift;
        private DriftResult localDrift;
        private List<String> itemsetVectors;
        private int totalEstimate;
        private int speedTxPerSec;
        private String message;
        private String globalDriftMessage;
        private String localDriftMessage;

        public Builder withType(EventType type) {
            this.type = type;
            return this;
        }

        public Builder withTid(int tid) {
            this.tid = tid;
            return this;
        }

        public Builder withTransaction(Transaction transaction) {
            this.transaction = transaction;
            return this;
        }

        public Builder withCheckpoint(Checkpoint checkpoint) {
            this.checkpoint = checkpoint;
            return this;
        }

        public Builder withDriftResult(DriftResult driftResult) {
            this.driftResult = driftResult;
            return this;
        }

        public Builder withGlobalDrift(DriftResult globalDrift) {
            this.globalDrift = globalDrift;
            return this;
        }

        public Builder withLocalDrift(DriftResult localDrift) {
            this.localDrift = localDrift;
            return this;
        }

        public Builder withItemsetVectors(List<String> itemsetVectors) {
            this.itemsetVectors = itemsetVectors;
            return this;
        }

        public Builder withTotalEstimate(int totalEstimate) {
            this.totalEstimate = totalEstimate;
            return this;
        }

        public Builder withSpeedTxPerSec(int speedTxPerSec) {
            this.speedTxPerSec = speedTxPerSec;
            return this;
        }

        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder withGlobalDriftMessage(String globalDriftMessage) {
            this.globalDriftMessage = globalDriftMessage;
            return this;
        }

        public Builder withLocalDriftMessage(String localDriftMessage) {
            this.localDriftMessage = localDriftMessage;
            return this;
        }

        public SimulationEvent build() {
            return new SimulationEvent(this);
        }
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
