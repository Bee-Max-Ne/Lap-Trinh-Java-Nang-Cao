package huddtds.model;

/**
 * Lớp Element biểu diễn một phần tử đơn trong một giao dịch.
 * Hỗ trợ cả hai chế độ:
 * 1. Chế độ định lượng (Quantity-based) dùng cho running example: (item, quantity).
 * 2. Chế độ tiện ích trực tiếp (Utility-based) dùng cho benchmark dataset thực tế: (item, utility).
 */
public class Element {
    /** Tên hoặc mã của item */
    private final String item;

    /** Số lượng của item (internal utility / purchase quantity) */
    private final int quantity;

    /** Giá trị độ lợi trực tiếp u(i, T) của item trong giao dịch */
    private final double utility;

    /**
     * Khởi tạo phần tử theo số lượng (tương thích ngược với code cũ).
     * @param item tên mục hàng hóa
     * @param quantity số lượng của mục đó
     */
    public Element(String item, int quantity) {
        this(item, quantity, 0.0);
    }

    /**
     * Khởi tạo phần tử theo độ lợi trực tiếp (dùng cho dataset thực tế SPMF/HUIM).
     * @param item mã hoặc tên mục hàng hóa
     * @param utility giá trị độ lợi thực tế của item trong giao dịch
     */
    public Element(String item, double utility) {
        this(item, 1, utility);
    }

    /**
     * Khởi tạo đầy đủ cả số lượng và độ lợi.
     * @param item mã hoặc tên mục hàng hóa
     * @param quantity số lượng
     * @param utility độ lợi trực tiếp
     */
    public Element(String item, int quantity, double utility) {
        this.item = item;
        this.quantity = quantity;
        this.utility = utility;
    }

    /**
     * Trả về tên/mã item.
     * @return tên item
     */
    public String getItem() {
        return item;
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

    @Override
    public String toString() {
        if (utility > 0.0) {
            return item + ":" + utility;
        }
        return item + ":" + quantity;
    }
}
