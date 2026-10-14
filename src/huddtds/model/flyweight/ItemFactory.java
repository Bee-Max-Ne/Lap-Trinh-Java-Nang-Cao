package huddtds.model.flyweight;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp ItemFactory quản lý một kho các đối tượng ItemFlyweight.
 * Sử dụng ConcurrentHashMap để обеспечить thread-safe trong môi trường đa luồng.
 * Các đối tượng ItemFlyweight được tạo ra và chia sẻ, giảm bộ nhớ khi có nhiều item trùng lặp.
 */
public class ItemFactory {
    /** Bản đồ lưu trữ các ItemFlyweight, key là itemId */
    private static final ConcurrentHashMap<String, ItemFlyweight> itemMap = new ConcurrentHashMap<>();

    /**
     * Lấy đối tượng ItemFlyweight tương ứng với itemId, hoặc tạo mới nếu chưa tồn tại.
     * @param itemId mã định danh của item
     * @param externalUtility tiện ích ngoại vi của item (nếu tạo mới)
     * @return đối tượng ItemFlyweight
     */
    public static ItemFlyweight getOrCreate(String itemId, double externalUtility) {
        return itemMap.computeIfAbsent(itemId, k -> {
            // Nếu chưa có, chúng ta cần chỉ số (index). Đây là một cách đơn giản: dùng kích thước bản đồ hiện tại.
            // Tuy nhiên, trong môi trường đa luồng, kích thước bản đồ có thể thay đổi giữa các lần gọi computeIfAbsent.
            // Để đơn giản, chúng ta có thể sử dụng một AtomicInteger để tạo chỉ số.
            // Nhưng để giữ đơn giản và đồng bộ với yêu cầu, chúng ta sẽ để index = -1 cho mục mới,
            // và sau đó có thể cập nhật index bằng cách khác? Tuy nhiên, yêu cầu chỉ mencion index.
            // Vì mục đích của Flyweight là chia sẻ, chúng ta có thể không cần index trong kho,
            // nhưng lớp ItemFlyweight yêu cầu index. Chúng ta sẽ tạo index dựa trên thời điểm tạo.
            // Lưu ý: trong môi trường đa luồng, có thể xảy ra trùng lặp index nếu không dùng bộ đếm nguyên tử.
            // Để đơn giản và tập trung vào mục đích chia sẻ, chúng ta sẽ để index = 0 và ghi đè nếu cần?
            // Tuy nhiên, để tránh phức tạp, chúng ta sẽ để index = -1 và ghi chú rằng index không được sử dụng trong Flyweight.
            // Thực tế, index có thể được sử dụng để ánh xạ trong mảng, nhưng trongour context, chúng ta có thể không cần.
            // Vì yêu cầu chỉ示 index, chúng ta sẽ giữ nó và để là -1 cho mục mới, và dựa vào việc rằng
            // trongour application, index không được sử dụng để tra cứu mà chỉ là một thuộc tính.
            // Một cách tốt hơn là sử dụng một AtomicInteger để tạo chỉ số tăng dần.
            // Vì chúng ta không được sửa đổi lớp ItemFlyweight sau khi tạo, chúng ta phải cung cấp index khi tạo.
            // Chúng ta sẽ tạo một AtomicInteger làm biến static để tạo chỉ số.
            return new ItemFlyweight(ItemIndexGenerator.getNextIndex(), itemId, externalUtility);
        });
    }

    /**
     * Lớp nội bộ để tạo chỉ số tăng dần một cách thread-safe.
     */
    private static class ItemIndexGenerator {
        private static final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);

        static int getNextIndex() {
            return counter.getAndIncrement();
        }
    }

    /**
     * Xóa toàn bộ kho (chủ yếu dùng cho testing).
     */
    public static void clear() {
        itemMap.clear();
        ItemIndexGenerator.counter.set(0);
    }
}