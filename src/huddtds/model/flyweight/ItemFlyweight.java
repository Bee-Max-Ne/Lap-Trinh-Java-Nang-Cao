package huddtds.model.flyweight;

/**
 * Lớp ItemFlyweight biểu diễn một mục hàng hóa (item) với các thuộc tính immutable:
 * - index: chỉ số của item (có thể dùng để ánh xạ trong mảng hoặc danh sách)
 * - id: mã định danh唯一 của item (ví dụ: "a", "b", "c")
 * - externalUtility: tiện ích ngoại vi của item (utility trên đơn vị)
 */
public class ItemFlyweight {
    /** Chỉ số của item */
    private final int index;
    /** Mã định danh唯一 của item */
    private final String id;
    /** Tiện ích ngoại vi của item */
    private final double externalUtility;

    /**
     * Khởi tạo một ItemFlyweight immutable.
     * @param index chỉ số của item
     * @param id mã định danh của item
     * @param externalUtility tiện ích ngoại vi của item
     */
    public ItemFlyweight(int index, String id, double externalUtility) {
        this.index = index;
        this.id = id;
        this.externalUtility = externalUtility;
    }

    /**
     * Trả về chỉ số của item.
     * @return index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Trả về mã định danh của item.
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Trả về tiện ích ngoại vi của item.
     * @return externalUtility
     */
    public double getExternalUtility() {
        return externalUtility;
    }

    @Override
    public String toString() {
        return "ItemFlyweight{index=" + index + ", id='" + id + '\'' + ", externalUtility=" + externalUtility + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemFlyweight that = (ItemFlyweight) o;
        return index == that.index &&
                Double.compare(that.externalUtility, externalUtility) == 0 &&
                id.equals(that.id);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = index;
        result = 31 * result + id.hashCode();
        temp = Double.doubleToLongBits(externalUtility);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}