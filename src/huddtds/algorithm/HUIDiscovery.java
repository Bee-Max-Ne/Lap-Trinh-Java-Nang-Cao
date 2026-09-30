package huddtds.algorithm;

import huddtds.math.UtilityMetrics;
import huddtds.model.Element;
import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import java.util.*;
import java.util.function.Consumer;

/**
 * Phát hiện High Utility Itemset trong cửa sổ dữ liệu hiện tại.
 * Tích hợp kỹ thuật tỉa cận trên TWU (Transaction-Weighted Downward Closure)
 * và hỗ trợ cả dữ liệu thực tế (utility-based) lẫn dữ liệu mô phỏng (quantity-based).
 */
public class HUIDiscovery {
    private final Map<String, Double> externalUtilities;
    private final double minutil;
    private final int windowSize;
    private int maxItemsetSize = 4; // Mặc định giới hạn kích thước tập mục để tối ưu tốc độ
    private Consumer<String> traceListener;
    private int candidatesVisited;
    private int candidatesPruned;
    private int candidatesEvaluated;

    public HUIDiscovery(Map<String, Double> externalUtilities, double minutil, int windowSize) {
        this.externalUtilities = (externalUtilities != null) ? externalUtilities : Collections.emptyMap();
        this.minutil = minutil;
        this.windowSize = windowSize;
        this.traceListener = null;
    }

    public void setTraceListener(Consumer<String> traceListener) {
        this.traceListener = traceListener;
    }

    private void trace(String message) {
        if (traceListener != null) {
            traceListener.accept(message);
        }
    }

    public HUIDiscovery(Map<String, Double> externalUtilities, double minutil, int windowSize, int maxItemsetSize) {
        this(externalUtilities, minutil, windowSize);
        this.maxItemsetSize = maxItemsetSize;
    }

    public int getMaxItemsetSize() {
        return maxItemsetSize;
    }

    public void setMaxItemsetSize(int maxItemsetSize) {
        this.maxItemsetSize = maxItemsetSize;
    }

    /**
     * Tìm HUI trong cửa sổ giao dịch kết thúc tại currentTid.
     *
     * @param memory lịch sử giao dịch
     * @param currentTid TID hiện tại
     * @return danh sách HUI trong cửa sổ
     */
    public List<HighUtilityItemset> discover(List<Transaction> memory, int currentTid) {
        List<Transaction> window = buildWindow(memory, currentTid);
        trace(String.format("[HUI][TID=%d] Built window: %d transaction(s), expected range (%d, %d]",
                currentTid, window.size(), currentTid - windowSize, currentTid));
        if (window.isEmpty()) {
            trace(String.format("[HUI][TID=%d] No transactions in active window; discovery skipped", currentTid));
            return Collections.emptyList();
        }

        // 1. Lọc item tiềm năng bằng TWU có decay
        List<String> promisingItems = filterPromisingItems(window, currentTid);
        trace(String.format("[HUI][TID=%d] TWU + decay filter retained %d promising item(s): %s",
                currentTid, promisingItems.size(), summarizeItems(promisingItems)));
        List<HighUtilityItemset> result = new ArrayList<>();

        // 2. Khai phá các tập mục đạt minutil
        candidatesVisited = 0;
        candidatesPruned = 0;
        candidatesEvaluated = 0;
        generateCandidates(promisingItems, 0, new ArrayList<>(), window, currentTid, result);
        trace(String.format("[HUI][TID=%d] Candidate search complete: visited=%d, TWU-pruned=%d, utility-evaluated=%d, HUI=%d",
                currentTid, candidatesVisited, candidatesPruned, candidatesEvaluated, result.size()));
        List<HighUtilityItemset> topHuis = new ArrayList<>(result);
        topHuis.sort(Comparator.comparingDouble(HighUtilityItemset::getTotalUtility).reversed());
        for (HighUtilityItemset hui : topHuis.subList(0, Math.min(20, topHuis.size()))) {
            trace(String.format("[HUI][TID=%d] %s utility=%.6f, Dmo=%.6f",
                    currentTid, hui.getItems(), hui.getTotalUtility(), hui.getDistanceToRoot()));
        }
        return result;
    }

    private String summarizeItems(List<String> items) {
        int shown = Math.min(30, items.size());
        String summary = items.subList(0, shown).toString();
        return items.size() > shown ? summary + " ... (" + (items.size() - shown) + " more)" : summary;
    }

    private List<Transaction> buildWindow(List<Transaction> memory, int currentTid) {
        List<Transaction> window = new ArrayList<>();
        int minTid = currentTid - windowSize;
        for (Transaction transaction : memory) {
            if (transaction.getTid() <= currentTid && transaction.getTid() > minTid) {
                window.add(transaction);
            }
        }
        return window;
    }

    /**
     * Lọc các item có TWU (Transaction Weighted Utility) >= minutil.
     * Áp dụng tính chất TWDC: nếu TWU(item) < minutil thì mọi tập chứa item đó đều không thể là HUI.
     */
    private List<String> filterPromisingItems(List<Transaction> window, int currentTid) {
        Map<String, Double> itemTwu = new HashMap<>();

        for (Transaction tx : window) {
            int deltaTime = Math.max(0, currentTid - tx.getTid());
            double decay = UtilityMetrics.decayFunction(deltaTime);
            double decayedTu = tx.calculateTotalUtility(externalUtilities) * decay;

            for (Element el : tx.getElements()) {
                itemTwu.put(el.getItem(), itemTwu.getOrDefault(el.getItem(), 0.0) + decayedTu);
            }
        }

        List<String> promisingItems = new ArrayList<>();
        for (Map.Entry<String, Double> entry : itemTwu.entrySet()) {
            if (entry.getValue() >= minutil) {
                promisingItems.add(entry.getKey());
            }
        }

        Collections.sort(promisingItems);
        return promisingItems;
    }

    private void generateCandidates(
            List<String> itemList,
            int startIndex,
            List<String> currentItems,
            List<Transaction> window,
            int currentTid,
            List<HighUtilityItemset> result) {

        for (int index = startIndex; index < itemList.size(); index++) {
            currentItems.add(itemList.get(index));
            Set<String> candidate = new LinkedHashSet<>(currentItems);
            candidatesVisited++;

            // Kiểm tra cận trên TWU của candidate trong window
            double candidateUpperBound = calculateDecayedSubtreeUpperBound(candidate, window, currentTid);
            if (candidateUpperBound >= minutil) {
                candidatesEvaluated++;
                HighUtilityItemset hui = calculateUtility(candidate, window, currentTid);
                if (hui.getTotalUtility() >= minutil) {
                    hui.setDistanceToRoot(UtilityMetrics.calculateDmoToRoot(hui));
                    result.add(hui);
                }

                // Tiếp tục duyệt sâu nếu chưa vượt quá maxItemsetSize
                if (maxItemsetSize <= 0 || currentItems.size() < maxItemsetSize) {
                    generateCandidates(itemList, index + 1, currentItems, window, currentTid, result);
                }
            } else {
                candidatesPruned++;
            }

            currentItems.remove(currentItems.size() - 1);
        }
    }

    /**
     * Cận trên tổng tiện ích của candidate và mọi tập mục con mở rộng từ nó trong cửa sổ.
     */
    private double calculateDecayedSubtreeUpperBound(
            Set<String> targetSet,
            List<Transaction> window,
            int currentTid) {
        double bound = 0.0;
        for (Transaction tx : window) {
            boolean containsAll = true;
            for (String item : targetSet) {
                if (!tx.containsItem(item)) {
                    containsAll = false;
                    break;
                }
            }
            if (containsAll) {
                int deltaTime = Math.max(0, currentTid - tx.getTid());
                bound += tx.calculateTotalUtility(externalUtilities) * UtilityMetrics.decayFunction(deltaTime);
            }
        }
        return bound;
    }

    private HighUtilityItemset calculateUtility(
            Set<String> targetSet,
            List<Transaction> window,
            int currentTid) {

        HighUtilityItemset hui = new HighUtilityItemset(targetSet);
        double totalUtility = 0.0;

        for (Transaction tx : window) {
            boolean containsAll = true;
            for (String item : targetSet) {
                if (!tx.containsItem(item)) {
                    containsAll = false;
                    break;
                }
            }
            if (!containsAll) {
                continue;
            }

            int deltaTime = Math.max(0, currentTid - tx.getTid());
            double decay = UtilityMetrics.decayFunction(deltaTime);

            for (String item : targetSet) {
                double rawUtility;
                double directUtil = tx.getUtility(item);
                if (directUtil > 0.0) {
                    rawUtility = directUtil;
                } else {
                    int qty = tx.getQuantity(item);
                    rawUtility = qty * externalUtilities.getOrDefault(item, 1.0);
                }

                double decayedUtil = rawUtility * decay;
                hui.getItemUtilities().put(item,
                        hui.getItemUtilities().getOrDefault(item, 0.0) + decayedUtil);
                totalUtility += decayedUtil;
            }
        }

        hui.setTotalUtility(totalUtility);
        return hui;
    }
}
