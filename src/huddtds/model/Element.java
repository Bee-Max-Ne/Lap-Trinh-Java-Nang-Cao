package huddtds.model;

import huddtds.model.flyweight.ItemFlyweight;
import huddtds.model.flyweight.ItemFactory;

/**
 * Lớp Element biểu diễn một phần tử đơn trong một giao dịch.
 * Hỗ trợ cả hai chế độ:
 * 1. Chế độ định lượng (Quantity-based) dùng cho running example: (item, quantity).
 * 2. Chế độ tiện ích trực tiếp (Utility-based) dùng cho benchmark dataset thực tế: (item, utility).
 * <p>
 * Đã được refactor để sử dụng Flyweight pattern cho item, giúp tối ưu bộ nhớ RAM.
 */
public class Element {
    /** Tham chiếu đến đối tượng ItemFlyweight (chung) để lưu trữ mã item và tiện ích ngoại vi */
    private final ItemFlyweight flyweight;

    /** Số lượng của item (internal utility / purchase quantity) */
    private final int quantity;

    /** Giá trị độ lợi trực tiếp u(i, T) của item trong giao dịch */
    private final double utility;

    /**
     * Khởi tạo phần tử theo số lượng (tương thích ngược với code cũ).
     * @param item tên mục hàngóa
     * @param quantity số lượng của mục đó
     */
    public Element(String item, int quantity) {
        this(ItemFactory.getOrCreate(item, 1.0), quantity, 0.0);
    }

    /**
     * Khởi tạo phần tử theo độ lợi trực tiếp (dùng cho dataset thực tế SPMF/HUIM).
     * @param item mã hoặc tên mục hàngóa
     * @param utility giá trị độ lợi thực tế của item trong giao dịch
     */
    public Element(String item, double utility) {
        this(ItemFactory.getOrCreate(item, 1.0), 1, utility);
    }

    /**
     * Khởi tạo đầy đủ cả số lượng và độ lợi.
     * @param item mã hoặc tên mục hàngóa
     * @param quantity số lượng
     * @param utility độ lợi trực tiếp
     */
    public Element(String item, int quantity, double utility) {
        this(ItemFactory.getOrCreate(item, 1.0), quantity, utility);
    }

    /**
     * Constructor nội bộ dùng để tạo Element từ ItemFlyweight (để tránh việc tìm kiếm item trong factory nhiều lần).
     * @param flyweight đối tượng ItemFlyweight chung
     * @param quantity số lượng của item
     * @param utility giá trị độ lợi trực tiếp của item trong giao dịch
     */
    private Element(ItemFlyweight flyweight, int quantity, double utility) {
        this.flyweight = flyweight;
        this.quantity = quantity;
        this.utility = utility;
    }

    /**
     * Trả về tên/mã item.
     * @return tên item (đảm bảo backward compatibility)
     */
    public String getItem() {
        return flyweight.getId();
    }

    /**
     * Trả về số lượng của item.
     * @return số lượng
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Trả về giá trị độ lợi của item trong giao dịch.
     * @return utility
     */
    public double getUtility() {
        return utility;
    }

    /**
     * Trả về đối tượng ItemFlyweight (đối tượng chung).
     * principalement dùng cho nội bộ hoặc các lớp cần truy cập vào thuộc tính chung của item.
     * @return ItemFlyweight
     */
    public ItemFlyweight getFlyweight() {
        return flyweight;
    }

    @Override
    public String toString() {
        if (utility > 0.0) {
            return flyweight.getId() + ":" + utility;
        }
        return flyweight.getId() + ":" + quantity;
    }
}