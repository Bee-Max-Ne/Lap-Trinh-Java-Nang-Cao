package huddtds.algorithm;

import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;
import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Lớp HUDD_TDS là bộ điều phối trung tâm của hệ thống phát hiện trôi dạt độ lợi.
 * Nhận luồng giao dịch, duy trì cửa sổ trượt, phát hiện HUI tại các checkpoint
 * và kiểm định trôi dạt toàn cục (Global Drift) và cục bộ (Local Drift).
 */
public class HUDD_TDS {
    private final Map<String, Double> externalUtilities;
    private final double minutil;
    private final int interval;
    private final int windowSize;
    private final double alphaConfidence;

    /** Bộ nhớ trượt lưu các giao dịch trong phạm vi cửa sổ hoạt động */
    private final List<Transaction> memory;

    /** Danh sách checkpoint đã sinh ra */
    private final List<Checkpoint> checkpoints;

    /** Bộ phát hiện drift toàn cục */
    private final GlobalDriftDetector globalDriftDetector;

    /** Bộ phát hiện drift cục bộ */
    private final LocalDriftDetector localDriftDetector;

    /** Bộ khai phá HUI */
    private final HUIDiscovery huiDiscovery;

    public HUDD_TDS(Map<String, Double> externalUtilities,
                    double minutil,
                    int interval,
                    int windowSize,
                    double alphaConfidence) {
        this.externalUtilities = (externalUtilities != null) ? externalUtilities : Collections.emptyMap();
        this.minutil = minutil;
        this.interval = interval;
        this.windowSize = windowSize;
        this.alphaConfidence = alphaConfidence;
        this.memory = new ArrayList<>();
        this.checkpoints = new ArrayList<>();
        this.globalDriftDetector = new GlobalDriftDetector(alphaConfidence, 1.0);
        this.localDriftDetector = new LocalDriftDetector(alphaConfidence, 100.0, windowSize);
        this.huiDiscovery = new HUIDiscovery(this.externalUtilities, minutil, windowSize);
    }

    public HUDD_TDS(Map<String, Double> externalUtilities,
                    double minutil,
                    int interval,
                    int windowSize,
                    double alphaConfidence,
                    int maxItemsetSize) {
        this(externalUtilities, minutil, interval, windowSize, alphaConfidence);
        this.huiDiscovery.setMaxItemsetSize(maxItemsetSize);
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

    public HUIDiscovery getHuiDiscovery() {
        return huiDiscovery;
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

        // Quản trị bộ nhớ trượt: giữ lại tối đa 3 lần windowSize để phục vụ so sánh giữa các checkpoint
        int safeRetentionTid = tx.getTid() - (windowSize * 3);
        while (!memory.isEmpty() && memory.get(0).getTid() <= safeRetentionTid) {
            memory.remove(0);
        }

        // Kiểm tra xem đã đến chu kỳ checkpoint chưa
        if (tx.getTid() % interval != 0) {
            return null;
        }

        Checkpoint cp = new Checkpoint(tx.getTid());
        cp.getHuis().addAll(huiDiscovery(tx.getTid()));

        double totalDistance = 0.0;
        for (HighUtilityItemset hui : cp.getHuis()) {
            totalDistance += hui.getDistanceToRoot();
        }
        cp.setGlobalDistance(totalDistance);
        checkpoints.add(cp);
        return cp;
    }

    /**
     * Phát hiện các HUI trong cửa sổ dữ liệu gần nhất với thời điểm t.
     *
     * @param t thời điểm hiện tại (TID)
     * @return danh sách HUI thoả ngưỡng minutil
     */
    public List<HighUtilityItemset> huiDiscovery(int t) {
        return huiDiscovery.discover(memory, t);
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
        String direction = globalDriftDetector.updateAndCheck(curr.getGlobalDistance());
        if (direction != null) {
            double diff = Math.abs(curr.getGlobalDistance() - prev.getGlobalDistance());
            return String.format("GLOBAL DRIFT (%s) [Δ=%.4f]", direction, diff);
        }
        return null;
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

        String driftItemset = localDriftDetector.detect(checkpoints);
        if (driftItemset != null) {
            return "LOCAL DRIFT (" + driftItemset + ")";
        }
        return null;
    }
}
