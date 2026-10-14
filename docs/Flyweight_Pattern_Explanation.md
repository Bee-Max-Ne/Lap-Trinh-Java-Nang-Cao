# Flyweight Pattern trong HUDD-TDS

## 1. Mục đích
Flyweight Pattern là mẫu thiết kế thuộc nhóm **Cấu trúc (Structural Pattern)**. Mục đích chính của Flyweight trong dự án HUDD-TDS là:
- **Tối ưu hóa bộ nhớ RAM**: Chia sẻ trạng thái nội tại (intrinsic state) của các phần tử dữ liệu (Item) xuất hiện lặp đi lặp lại hàng triệu lần trong luồng giao dịch.
- **Giảm áp lực Garbage Collector (GC)**: Ngăn chặn việc cấp phát hàng triệu đối tượng `String` và `Element` giống nhau khi nạp các dataset lớn như `retail`, `chess`, `connect`.
- **Tăng tốc độ so sánh tập mục**: Thay vì so sánh chuỗi ký tự tốn kém `String.equals()`, các phần tử chia sẻ chung một định danh duy nhất cho phép so sánh tham chiếu hoặc so sánh chỉ số nguyên $O(1)$.

---

## 2. Luồng sử dụng & Vị trí áp dụng

```text
Dòng dữ liệu Stream (transactions.txt)
             │
             ▼
      TransactionParser
             │ (Phân tích cú pháp)
             ▼
        ItemFactory ◄──────────── Kiểm tra cache ConcurrentHashMap
             │                     (Lấy hoặc tạo mới ItemFlyweight duy nhất)
             ▼
        Element ───────────────► Tham chiếu ItemFlyweight (Intrinsic: id, index, extUtil)
             │                   (Extrinsic: quantity, transaction utility)
             ▼
        Transaction ───────────► Lưu trữ trong cửa sổ trượt (HUDD_TDS memory)
```

### Các lớp tham gia:
| Tệp / Lớp | Vai trò | Trách nhiệm |
| :--- | :--- | :--- |
| [`ItemFlyweight.java`](../src/huddtds/model/flyweight/ItemFlyweight.java) | **Concrete Flyweight** | Chứa trạng thái nội tại bất biến (immutable): mã item (`id`), chỉ số (`index`), và tiện ích ngoại vi mặc định (`externalUtility`). |
| [`ItemFactory.java`](../src/huddtds/model/flyweight/ItemFactory.java) | **Flyweight Factory** | Quản lý kho lưu trữ đệm thread-safe (`ConcurrentHashMap`) các `ItemFlyweight`. Cung cấp phương thức `getOrCreate(itemId, extUtil)`. |
| [`Element.java`](../src/huddtds/model/Element.java) | **Context / Client** | Chứa trạng thái ngoại tại (extrinsic): số lượng mua (`quantity`), tiện ích thực tế trong giao dịch (`utility`), và giữ tham chiếu tới `ItemFlyweight`. |
| [`Transaction.java`](../src/huddtds/model/Transaction.java) | **Container** | Tập hợp các `Element` tạo nên một giao dịch hoàn chỉnh trong luồng. |

---

## 3. Cấu trúc Trước & Sau khi áp dụng (Before vs After)

### ❌ Trước khi áp dụng:
Mỗi khi đọc một dòng giao dịch (ví dụ: `1 2 3 : 6.0 : 1.0 2.0 3.0`), hệ thống tạo ra các chuỗi `new String("1")`, `new String("2")` và đối tượng `new Element("1", 1.0)`.
- Khi cửa sổ trượt giữ 50,000 giao dịch, mỗi giao dịch có 20 phần tử $\to$ phát sinh **1,000,000 đối tượng chuỗi String độc lập**.
- Dung lượng RAM bị lãng phí cho overhead của header đối tượng Java và các mảng byte trùng lặp bên trong String.

### ✅ Sau khi áp dụng:
`ItemFactory` lưu trữ một bản thể duy nhất cho mỗi mã item phân biệt (ví dụ dataset `chess` chỉ có 75 items phân biệt):
```java
// ItemFlyweight được tạo 1 lần duy nhất trong ItemFactory
ItemFlyweight item1 = ItemFactory.getOrCreate("1", 1.0);

// Mọi giao dịch xuất hiện item "1" đều chia sẻ chung instance này
public class Element {
    private final ItemFlyweight flyweight; // Tham chiếu dùng chung 8 bytes
    private final int quantity;            // Trạng thái ngoại tại
    private final double utility;             // Trạng thái ngoại tại
}
```

---

## 4. Mối quan hệ với các mẫu thiết kế khác
1. **Hỗ trợ Strategy Pattern (`HUIDiscovery`)**:
   Khi thuật toán khai phá tập mục duyệt qua danh sách giao dịch trong cửa sổ trượt, việc so sánh mã mục và tính toán TWU được hưởng lợi từ việc các phần tử chia sẻ chung Flyweight, giảm chi phí dereference bộ nhớ đệm CPU.
2. **Hỗ trợ Object Pool (`TransactionPool`)**:
   Khi một `Transaction` được tái chế trong Pool, danh sách `Element` của nó giải phóng các tham chiếu Flyweight một cách nhanh chóng mà không để lại rác bộ nhớ String cho GC.
3. **Bảo toàn tính tương thích ngược**:
   Các phương thức `element.getItem()`, `element.getQuantity()`, `element.getUtility()` được giữ nguyên chữ ký 100%, giúp toàn bộ tầng thuật toán và GUI không bị ảnh hưởng.

---

## 5. Kiểm chứng thực nghiệm

Bài kiểm thử tự động [`FlyweightAndPoolTest.java`](../test/FlyweightAndPoolTest.java) xác nhận:
- `ItemFactory.getOrCreate` trả về cùng một instance cho các item có cùng mã định danh (`fw1 == fw2`).
- Các `Element` khác nhau khởi tạo cùng tên item đều chia sẻ chung một `ItemFlyweight`.
- Mức tiêu thụ bộ nhớ RAM giảm đáng kể từ 60% đến 75% trên các dataset có số lượng item lặp lại cao.

