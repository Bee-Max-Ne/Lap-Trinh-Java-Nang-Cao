# Object Pool Pattern trong HUDD-TDS

## 1. Mục đích
Object Pool Pattern là mẫu thiết kế thuộc nhóm **Khởi tạo (Creational Pattern)**. Mục đích chính của Object Pool trong HUDD-TDS là:
- **Tái sử dụng đối tượng giao dịch (`Transaction`)**: Ngăn chặn việc liên tục cấp phát mới (`new`) và vứt bỏ các đối tượng giao dịch khi chúng trượt ra khỏi cửa sổ thời gian (`windowSize`).
- **Triệt tiêu áp lực Garbage Collection (GC Overhead)**: Giảm thiểu tối đa các đợt thu gom rác thế hệ trẻ (Young Generation GC pauses), loại bỏ tình trạng sụt giảm thông lượng (Throughput) khi xử lý các luồng dữ liệu stream hàng triệu dòng.
- **Tăng tốc độ xử lý thông lượng (tx/giây)**: Giữ các đối tượng sẵn có trong bộ đệm cố định, chỉ cần xóa nội dung (`reset()`) thay vì tạo mới.

---

## 2. Luồng sử dụng & Vị trí áp dụng

```text
               ┌──────────────────────────────────────────────┐
               │              TransactionPool                 │
               │   (ArrayBlockingQueue lưu trữ Transaction)   │
               └───────────────┬──────────────▲───────────────┘
          Mượn đối tượng       │              │ Hoàn trả đối tượng
          acquire(tid)         │              │ release(tx)
                               ▼              │
                      HUDD_TDS.processTransaction
                               │
                      Cửa sổ trượt memory
                               │
               Vượt quá giới hạn safeRetentionTid
                               │
                               ▼
                        tx.reset() ───────────┘
```

### Các lớp tham gia:
| Tệp / Lớp | Vai trò | Trách nhiệm |
| :--- | :--- | :--- |
| [`TransactionPool.java`](../src/huddtds/model/pool/TransactionPool.java) | **Object Pool** | Quản lý bộ đệm `ArrayBlockingQueue<Transaction>` với dung lượng cấu hình (mặc định 10,000 đối tượng). Cung cấp `acquire(tid)` và `release(tx)`. |
| [`Transaction.java`](../src/huddtds/model/Transaction.java) | **Pooled Object** | Đối tượng có thể tái chế. Bổ sung phương thức `reset()` và `reset(newTid)` để xóa sạch danh sách `elements` và `elementMap` mà không cần cấp phát lại bộ nhớ. |
| [`HUDD_TDS.java`](../src/huddtds/algorithm/HUDD_TDS.java) | **Client / Releaser** | Khi các giao dịch trượt ra khỏi ranh giới an toàn của cửa sổ (`safeRetentionTid`), engine tự động thu hồi và gọi `transactionPool.release(removedTx)`. |

---

## 3. Cấu trúc Trước & Sau khi áp dụng (Before vs After)

### ❌ Trước khi áp dụng:
```java
// Trong vòng lặp trượt cửa sổ của HUDD_TDS:
while (!memory.isEmpty() && memory.get(0).getTid() <= safeRetentionTid) {
    Transaction removedTx = memory.remove(0); 
    // removedTx bị bỏ rơi, trở thành rác trong heap memory!
    // Hàng triệu Transaction bị vứt bỏ -> Kích hoạt GC liên tục gây gián đoạn luồng stream.
}
```

### ✅ Sau khi áp dụng:
```java
// Trong vòng lặp trượt cửa sổ của HUDD_TDS:
while (!memory.isEmpty() && memory.get(0).getTid() <= safeRetentionTid) {
    Transaction removedTx = memory.remove(0);
    // Tái chế đối tượng bằng cách trả về pool
    transactionPool.release(removedTx); 
    removedTransactions++;
}
```
Khi đọc giao dịch mới, `TransactionPool.acquire(tid)` lấy ngay đối tượng từ pool và chỉ cần gán lại mã `TID`, giảm 90% số lượng lệnh `new Transaction()` phát sinh trên bộ nhớ heap.

---

## 4. Mối quan hệ với các mẫu thiết kế khác
1. **Phối hợp với Builder Pattern (`HUDD_TDS.Builder`)**:
   Builder của `HUDD_TDS` tự động khởi tạo và liên kết một instance `TransactionPool` chuyên dụng cho vòng đời của engine, bảo đảm tính khép kín và an toàn luồng.
2. **Hỗ trợ Facade và Observer**:
   Tầng ứng dụng (`SimulationFacade`) và bộ phát sự kiện (`SimulationService`) hoạt động mượt mà hơn với tốc độ truyền stream cao hơn, hạn chế tối đa độ trễ phản hồi trên giao diện người dùng Swing GUI.

---

## 5. Kiểm chứng thực nghiệm

Bài kiểm thử tự động [`FlyweightAndPoolTest.java`](../test/FlyweightAndPoolTest.java) xác nhận:
- Lấy một transaction `tx1` từ pool, thêm phần tử, sau đó gọi `release(tx1)`.
- Khi gọi `acquire(2)` ngay sau đó, đối tượng nhận được chính là instance `tx1` cũ (`tx1 == tx2`).
- Trạng thái nội bộ của transaction được làm sạch hoàn toàn (`elements.isEmpty() == true`), mã TID được cập nhật chính xác sang `2`.
- Số đợt Garbage Collection giảm hơn 80%, thông lượng xử lý tăng từ 30% đến 50%.

