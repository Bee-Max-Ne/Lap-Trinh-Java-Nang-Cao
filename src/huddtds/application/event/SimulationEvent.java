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
     * Mẫu khởi tạo Builder Pattern cho lớp SimulationEvent.
     * Giúp xây dựng các đối tượng sự kiện mô phỏng mà không cần truyền 13 tham số rời rạc.
     */
    public static class Builder {
        /** Loại sự kiện mô phỏng (CHECKPOINT_CREATED, GLOBAL_DRIFT, LOCAL_DRIFT,...) */
        private EventType type;
        /** Mã ID giao dịch hiện tại */
        private int tid;
        /** Đối tượng giao dịch hiện tại */
        private Transaction transaction;
        /** Đối tượng checkpoint chứa kết quả HUI */
        private Checkpoint checkpoint;
        /** Kết quả phát hiện trôi dạt tổng quát */
        private DriftResult driftResult;
        /** Kết quả phát hiện trôi dạt toàn cục */
        private DriftResult globalDrift;
        /** Kết quả phát hiện trôi dạt cục bộ */
        private DriftResult localDrift;
        /** Danh sách chuỗi biểu diễn vector tập mục độ lợi */
        private List<String> itemsetVectors;
        /** Ước tính tổng số giao dịch cần xử lý */
        private int totalEstimate;
        /** Tốc độ xử lý luồng (giao dịch / giây) */
        private int speedTxPerSec;
        /** Thông điệp mô tả sự kiện */
        private String message;
        /** Chuỗi thông điệp trôi dạt toàn cục */
        private String globalDriftMessage;
        /** Chuỗi thông điệp trôi dạt cục bộ */
        private String localDriftMessage;

        /** Thiết lập loại sự kiện */
        public Builder withType(EventType type) {
            this.type = type;
            return this;
        }

        /** Thiết lập mã giao dịch TID */
        public Builder withTid(int tid) {
            this.tid = tid;
            return this;
        }

        /** Thiết lập đối tượng giao dịch */
        public Builder withTransaction(Transaction transaction) {
            this.transaction = transaction;
            return this;
        }

        /** Thiết lập đối tượng checkpoint kết quả */
        public Builder withCheckpoint(Checkpoint checkpoint) {
            this.checkpoint = checkpoint;
            return this;
        }

        /** Thiết lập kết quả drift tổng quát */
        public Builder withDriftResult(DriftResult driftResult) {
            this.driftResult = driftResult;
            return this;
        }

        /** Thiết lập kết quả trôi dạt toàn cục */
        public Builder withGlobalDrift(DriftResult globalDrift) {
            this.globalDrift = globalDrift;
            return this;
        }

        /** Thiết lập kết quả trôi dạt cục bộ */
        public Builder withLocalDrift(DriftResult localDrift) {
            this.localDrift = localDrift;
            return this;
        }

        /** Thiết lập danh sách vector tập mục HUI */
        public Builder withItemsetVectors(List<String> itemsetVectors) {
            this.itemsetVectors = itemsetVectors;
            return this;
        }

        /** Thiết lập ước tính tổng số dòng giao dịch */
        public Builder withTotalEstimate(int totalEstimate) {
            this.totalEstimate = totalEstimate;
            return this;
        }

        /** Thiết lập tốc độ thông lượng xử lý */
        public Builder withSpeedTxPerSec(int speedTxPerSec) {
            this.speedTxPerSec = speedTxPerSec;
            return this;
        }

        /** Thiết lập thông điệp mô tả sự kiện */
        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        /** Thiết lập thông điệp trôi dạt toàn cục */
        public Builder withGlobalDriftMessage(String globalDriftMessage) {
            this.globalDriftMessage = globalDriftMessage;
            return this;
        }

        /** Thiết lập thông điệp trôi dạt cục bộ */
        public Builder withLocalDriftMessage(String localDriftMessage) {
            this.localDriftMessage = localDriftMessage;
            return this;
        }

        /** Khởi tạo đối tượng SimulationEvent hoàn chỉnh */
        public SimulationEvent build() {
            if (type == null) {
                throw new IllegalStateException("Event type is required");
            }
            if (tid < 0) {
                throw new IllegalStateException("Event TID must not be negative");
            }
            if (type == EventType.CHECKPOINT_CREATED && checkpoint == null) {
                throw new IllegalStateException("Checkpoint event requires a checkpoint");
            }
            if (type == EventType.GLOBAL_DRIFT
                    && !isDetectedDrift(driftResult, DriftResult.DriftType.GLOBAL_DRIFT)) {
                throw new IllegalStateException("Global drift event requires a detected global drift result");
            }
            if (type == EventType.LOCAL_DRIFT
                    && !isDetectedDrift(driftResult, DriftResult.DriftType.LOCAL_DRIFT)) {
                throw new IllegalStateException("Local drift event requires a detected local drift result");
            }
            if (type == EventType.TRANSACTION_PROCESSED
                    && (totalEstimate < 0 || speedTxPerSec < 0)) {
                throw new IllegalStateException("Progress metadata must not be negative");
            }
            return new SimulationEvent(this);
        }

        private boolean isDetectedDrift(DriftResult result, DriftResult.DriftType expectedType) {
            return result != null && result.isDetected() && result.getType() == expectedType;
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
