package huddtds.algorithm;

import huddtds.algorithm.drift.GlobalDriftStrategy;
import huddtds.algorithm.drift.LocalDriftStrategy;
import huddtds.algorithm.mining.HUIItemsetMiner;
import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;
import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Lớp HUDD_TDS là bộ điều phối trung tâm của hệ thống phát hiện trôi dạt độ lợi.
 * Nhận luồng giao dịch, duy trì cửa sổ trượt, phát hiện HUI tại các checkpoint
 * và kiểm định trôi dạt toàn cục (Global Drift) và cục bộ (Local Drift).
 */
public class HUDD_TDS {
    private final double minutil;
    private final int interval;
    private final int windowSize;
    private final double alphaConfidence;

    /** Bộ nhớ trượt lưu các giao dịch trong phạm vi cửa sổ hoạt động */
    private final List<Transaction> memory;

    /** Danh sách checkpoint đã sinh ra */
    private final List<Checkpoint> checkpoints;

    /** Bộ phát hiện drift toàn cục */
    private final GlobalDriftStrategy globalDriftDetector;

    /** Bộ phát hiện drift cục bộ */
    private final LocalDriftStrategy localDriftDetector;

    /** Bộ khai phá HUI */
    private final HUIItemsetMiner huiMiner;
    private Consumer<String> traceListener;
    private boolean traceEachTransaction;

    public HUDD_TDS(Map<String, Double> externalUtilities,
                    double minutil,
                    int interval,
                    int windowSize,
                    double alphaConfidence) {
        this(externalUtilities, minutil, interval, windowSize, alphaConfidence,
                new HUIDiscovery(externalUtilities, minutil, windowSize),
                new GlobalDriftDetector(alphaConfidence, 1.0),
                new LocalDriftDetector(alphaConfidence, 100.0, windowSize));
    }

    public HUDD_TDS(Map<String, Double> externalUtilities,
                    double minutil,
                    int interval,
                    int windowSize,
                    double alphaConfidence,
                    int maxItemsetSize) {
        this(externalUtilities, minutil, interval, windowSize, alphaConfidence,
                new HUIDiscovery(externalUtilities, minutil, windowSize, maxItemsetSize),
                new GlobalDriftDetector(alphaConfidence, 1.0),
                new LocalDriftDetector(alphaConfidence, 100.0, windowSize));
    }

    /**
     * Creates the stream coordinator with explicitly selected algorithm strategies.
     */
    public HUDD_TDS(Map<String, Double> externalUtilities,
                    double minutil,
                    int interval,
                    int windowSize,
                    double alphaConfidence,
                    HUIItemsetMiner huiMiner,
                    GlobalDriftStrategy globalDriftDetector,
                    LocalDriftStrategy localDriftDetector) {
        this.minutil = minutil;
        this.interval = interval;
        this.windowSize = windowSize;
        this.alphaConfidence = alphaConfidence;
        this.memory = new ArrayList<>();
        this.checkpoints = new ArrayList<>();
        this.globalDriftDetector = Objects.requireNonNull(globalDriftDetector, "globalDriftDetector");
        this.localDriftDetector = Objects.requireNonNull(localDriftDetector, "localDriftDetector");
        this.huiMiner = Objects.requireNonNull(huiMiner, "huiMiner");
        this.traceListener = null;
        this.traceEachTransaction = false;
    }

    public HUDD_TDS(Builder builder) {
        this.minutil = builder.minutil;
        this.interval = builder.interval;
        this.windowSize = builder.windowSize;
        this.alphaConfidence = builder.alphaConfidence;
        this.memory = new ArrayList<>();
        this.checkpoints = new ArrayList<>();
        
        HUIItemsetMiner miner = builder.huiMiner;
        if (miner == null) {
            miner = builder.maxItemsetSize > 0
                    ? new HUIDiscovery(builder.externalUtilities, builder.minutil, builder.windowSize, builder.maxItemsetSize)
                    : new HUIDiscovery(builder.externalUtilities, builder.minutil, builder.windowSize);
        }
        this.huiMiner = miner;
        
        this.globalDriftDetector = builder.globalDriftDetector != null
                ? builder.globalDriftDetector
                : new GlobalDriftDetector(builder.alphaConfidence, 1.0);
                
        this.localDriftDetector = builder.localDriftDetector != null
                ? builder.localDriftDetector
                : new LocalDriftDetector(builder.alphaConfidence, 100.0, builder.windowSize);
                
        this.traceListener = null;
        this.traceEachTransaction = false;
    }

    /**
     * Mẫu khởi tạo Builder Pattern cho bộ điều phối HUDD-TDS.
     * Cho phép cấu hình các siêu tham số toán học và tiêm các chiến lược (Strategy)
     * một cách linh hoạt, an toàn và dễ đọc.
     */
    public static class Builder {
        /** Bảng giá trị độ lợi ngoại vi của các mặt hàng */
        private Map<String, Double> externalUtilities = java.util.Collections.emptyMap();
        /** Ngưỡng độ lợi tối thiểu để xác định HUI */
        private double minutil = 10.0;
        /** Chu kỳ đánh giá checkpoint (số giao dịch) */
        private int interval = 1;
        /** Kích thước cửa sổ trượt lưu trữ giao dịch */
        private int windowSize = 2;
        /** Mức ý nghĩa thống kê Alpha cho kiểm định Hoeffding/Bonferroni */
        private double alphaConfidence = 0.05;
        /** Độ dài tập mục tối đa để lọc (0 là không giới hạn) */
        private int maxItemsetSize = 0;
        /** Chiến lược khai phá tập mục độ lợi cao (HUI Miner Strategy) */
        private HUIItemsetMiner huiMiner;
        /** Chiến lược phát hiện trôi dạt toàn cục (Global Drift Strategy) */
        private GlobalDriftStrategy globalDriftDetector;
        /** Chiến lược phát hiện trôi dạt cục bộ (Local Drift Strategy) */
        private LocalDriftStrategy localDriftDetector;

        /** Thiết lập bảng giá trị độ lợi ngoại vi */
        public Builder withExternalUtilities(Map<String, Double> externalUtilities) {
            this.externalUtilities = externalUtilities != null ? externalUtilities : java.util.Collections.emptyMap();
            return this;
        }

        /** Thiết lập ngưỡng độ lợi tối thiểu (MinUtil) - Phải >= 0 */
        public Builder withMinutil(double minutil) {
            if (minutil < 0) throw new IllegalArgumentException("Ngưỡng minutil không được âm: " + minutil);
            this.minutil = minutil;
            return this;
        }

        /** Thiết lập chu kỳ checkpoint (Interval) - Phải > 0 */
        public Builder withInterval(int interval) {
            if (interval <= 0) throw new IllegalArgumentException("Chu kỳ interval phải lớn hơn 0: " + interval);
            this.interval = interval;
            return this;
        }

        /** Thiết lập kích thước cửa sổ trượt (WindowSize) - Phải > 0 */
        public Builder withWindowSize(int windowSize) {
            if (windowSize <= 0) throw new IllegalArgumentException("Kích thước windowSize phải lớn hơn 0: " + windowSize);
            this.windowSize = windowSize;
            return this;
        }

        /** Thiết lập mức ý nghĩa Alpha (0 < alphaConfidence <= 1) */
        public Builder withAlphaConfidence(double alphaConfidence) {
            if (alphaConfidence <= 0 || alphaConfidence > 1) {
                throw new IllegalArgumentException("Ngưỡng alphaConfidence phải trong khoảng (0, 1]: " + alphaConfidence);
            }
            this.alphaConfidence = alphaConfidence;
            return this;
        }

        /** Thiết lập độ dài tập mục tối đa */
        public Builder withMaxItemsetSize(int maxItemsetSize) {
            this.maxItemsetSize = maxItemsetSize;
            return this;
        }

        /** Tiêm chiến lược khai phá HUI tùy chỉnh (Strategy Pattern) */
        public Builder withMiner(HUIItemsetMiner huiMiner) {
            this.huiMiner = huiMiner;
            return this;
        }

        /** Tiêm chiến lược phát hiện Drift toàn cục tùy chỉnh (Strategy Pattern) */
        public Builder withGlobalStrategy(GlobalDriftStrategy globalDriftDetector) {
            this.globalDriftDetector = globalDriftDetector;
            return this;
        }

        /** Tiêm chiến lược phát hiện Drift cục bộ tùy chỉnh (Strategy Pattern) */
        public Builder withLocalStrategy(LocalDriftStrategy localDriftDetector) {
            this.localDriftDetector = localDriftDetector;
            return this;
        }

        /** Khởi tạo đối tượng HUDD_TDS hoàn chỉnh sau khi kiểm tra hợp lệ */
        public HUDD_TDS build() {
            return new HUDD_TDS(this);
        }
    }

    public List<Checkpoint> getCheckpoints() {
        return checkpoints;
    }

    public double getAlphaConfidence() {
        return alphaConfidence;
    }

    public int getInterval() {
        return interval;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public double getMinutil() {
        return minutil;
    }

    /**
     * Returns the default concrete miner for legacy callers.
     *
     * @throws IllegalStateException when a different miner strategy was injected
     */
    @Deprecated
    public HUIDiscovery getHuiDiscovery() {
        if (huiMiner instanceof HUIDiscovery discovery) {
            return discovery;
        }
        throw new IllegalStateException("The configured HUI miner is not a HUIDiscovery instance");
    }

    public HUIItemsetMiner getHuiMiner() {
        return huiMiner;
    }

    public void setTraceListener(Consumer<String> traceListener, boolean traceEachTransaction) {
        this.traceListener = traceListener;
        this.traceEachTransaction = traceEachTransaction;
        huiMiner.setTraceListener(traceListener);
        globalDriftDetector.setTraceListener(traceListener);
        localDriftDetector.setTraceListener(traceListener);
    }

    private void trace(String message) {
        if (traceListener != null) {
            traceListener.accept(message);
        }
    }

    /**
     * Xử lý từng giao dịch theo dạng luồng (Stream).
     * Tự động dọn dẹp các giao dịch nằm ngoài cửa sổ trượt để bảo toàn bộ nhớ RAM.
     *
     * @param tx giao dịch mới
     * @return Checkpoint nếu khớp chu kỳ interval, ngược lại null
     */
    public Checkpoint processTransaction(Transaction tx) {
        memory.add(tx);
        int removedTransactions = 0;

        // Quản trị bộ nhớ trượt: giữ lại tối đa 3 lần windowSize để phục vụ so sánh giữa các checkpoint
        int safeRetentionTid = tx.getTid() - (windowSize * 3);
        while (!memory.isEmpty() && memory.get(0).getTid() <= safeRetentionTid) {
            memory.remove(0);
            removedTransactions++;
        }

        if (traceEachTransaction || tx.getTid() == 1 || tx.getTid() % interval == 0) {
            trace(String.format("[STREAM] TID=%d accepted; items=%d, TU=%.4f", tx.getTid(),
                    tx.getElements().size(), tx.getTransactionUtility()));
            trace(String.format("[WINDOW] retained=%d transactions; removed=%d old transactions (retention boundary TID<=%d)",
                    memory.size(), removedTransactions, safeRetentionTid));
        }

        // Kiểm tra xem đã đến chu kỳ checkpoint chưa
        if (tx.getTid() % interval != 0) {
            if (traceEachTransaction) {
                trace(String.format("[CHECKPOINT] TID=%d skipped; next checkpoint at a multiple of %d", tx.getTid(), interval));
            }
            return null;
        }

        trace(String.format("[CHECKPOINT] TID=%d started; minutil=%.4f, windowSize=%d", tx.getTid(), minutil, windowSize));
        Checkpoint cp = new Checkpoint(tx.getTid());
        cp.getHuis().addAll(huiDiscovery(tx.getTid()));

        double totalDistance = 0.0;
        for (HighUtilityItemset hui : cp.getHuis()) {
            totalDistance += hui.getDistanceToRoot();
        }
        cp.setGlobalDistance(totalDistance);
        checkpoints.add(cp);
        trace(String.format("[CHECKPOINT] TID=%d complete; HUI count=%d, DIS_HS=%.6f",
            tx.getTid(), cp.getHuis().size(), cp.getGlobalDistance()));
        return cp;
    }

    /**
     * Phát hiện các HUI trong cửa sổ dữ liệu gần nhất với thời điểm t.
     *
     * @param t thời điểm hiện tại (TID)
     * @return danh sách HUI thoả ngưỡng minutil
     */
    public List<HighUtilityItemset> huiDiscovery(int t) {
        return huiMiner.discover(memory, t);
    }

    /**
     * Kiểm tra drift toàn cục dựa trên sự chênh lệch globalDistance qua các checkpoint.
     *
     * @return chuỗi mô tả drift nếu có, ngược lại null
     */
    public String checkGlobalDrift() {
        if (checkpoints.size() < 2) {
            return null;
        }

        Checkpoint prev = checkpoints.get(checkpoints.size() - 2);
        Checkpoint curr = checkpoints.get(checkpoints.size() - 1);
        DriftResult result = Objects.requireNonNull(
                globalDriftDetector.updateAndCheck(
                        curr.getGlobalDistance(), prev.getTid(), curr.getTid()),
                "globalDriftDetector returned null");
        if (result.isDetected()) {
            return String.format("GLOBAL DRIFT (%s) [Δ=%.4f]",
                    result.getDirection(),
                    Math.abs(curr.getGlobalDistance() - prev.getGlobalDistance()));
        }
        return null;
    }

    public DriftResult checkGlobalDriftResult() {
        if (checkpoints.size() < 2) {
            return DriftResult.noDrift(-1, -1);
        }
        Checkpoint previous = checkpoints.get(checkpoints.size() - 2);
        Checkpoint current = checkpoints.get(checkpoints.size() - 1);
        return Objects.requireNonNull(
                globalDriftDetector.updateAndCheck(
                        current.getGlobalDistance(), previous.getTid(), current.getTid()),
                "globalDriftDetector returned null");
    }

    /**
     * Kiểm tra drift cục bộ dựa trên hiệu chỉnh Bonferroni.
     *
     * @return chuỗi mô tả drift cục bộ nếu có, ngược lại null
     */
    public String checkLocalDrift() {
        if (checkpoints.size() < 2) {
            return null;
        }

        Checkpoint previous = checkpoints.get(checkpoints.size() - 2);
        Checkpoint current = checkpoints.get(checkpoints.size() - 1);
        DriftResult result = Objects.requireNonNull(
                localDriftDetector.detect(previous, current),
                "localDriftDetector returned null");
        if (result.isDetected()) {
            if (result.getAffectedItemsets().isEmpty()) {
                throw new IllegalStateException("Detected local drift must identify an affected itemset");
            }
            return "LOCAL DRIFT (" + result.getAffectedItemsets().get(0) + ")";
        }
        return null;
    }

    public DriftResult checkLocalDriftResult() {
        if (checkpoints.size() < 2) {
            return DriftResult.noDrift(-1, -1);
        }
        return Objects.requireNonNull(
                localDriftDetector.detect(
                        checkpoints.get(checkpoints.size() - 2),
                        checkpoints.get(checkpoints.size() - 1)),
                "localDriftDetector returned null");
    }
}
