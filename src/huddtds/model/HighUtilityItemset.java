package huddtds.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Lớp HighUtilityItemset đại diện cho một tập mục có độ lợi cao (HUI).
 * Mỗi HUI gồm các item, tổng utility và khoảng cách D_mo tới gốc.
 */
public class HighUtilityItemset {
    /** Tập các item thuộc itemset */
    private final Set<String> items;

    /** Bản đồ lưu utility riêng cho từng item trong itemset */
    private final Map<String, Double> itemUtilities;

    /** Tổng utility của cả itemset */
    private double totalUtility;

    /** Độ lệch/độ xa của itemset so với vector gốc, ký hiệu D_mo */
    private double distanceToRoot;

    /**
     * Khởi tạo một High Utility Itemset mới từ tập item đã cho.
     * @param items tập mục của itemset
     */
    public HighUtilityItemset(Set<String> items) {
        this.items = items;
        this.itemUtilities = new HashMap<>();
        this.totalUtility = 0.0;
        this.distanceToRoot = 0.0;
    }

    /**
     * Trả về tập các item thuộc itemset.
     * @return tập item
     */
    public Set<String> getItems() {
        return items;
    }

    /**
     * Trả về map utility của từng item trong itemset.
     * @return map item -> utility
     */
    public Map<String, Double> getItemUtilities() {
        return itemUtilities;
    }

    /**
     * Lấy tổng utility của itemset.
     * @return tổng utility
     */
    public double getTotalUtility() {
        return totalUtility;
    }

    /**
     * Gán giá trị tổng utility cho itemset.
     * @param totalUtility tổng utility cần lưu
     */
    public void setTotalUtility(double totalUtility) {
        this.totalUtility = totalUtility;
    }

    /**
     * Lấy khoảng cách D_mo từ itemset tới gốc.
     * @return distanceToRoot
     */
    public double getDistanceToRoot() {
        return distanceToRoot;
    }

    /**
     * Gán khoảng cách D_mo cho itemset.
     * @param distanceToRoot giá trị khoảng cách cần lưu
     */
    public void setDistanceToRoot(double distanceToRoot) {
        this.distanceToRoot = distanceToRoot;
    }

    /**
     * Chuyển itemset sang chuỗi hiển thị để in ra console hoặc bảng.
     * @return chuỗi biểu diễn tập item dạng [a, b, c]
     */
    @Override
    public String toString() {
        return items.toString();
    }
}
