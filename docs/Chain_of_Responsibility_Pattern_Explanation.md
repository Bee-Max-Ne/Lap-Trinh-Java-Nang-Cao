# Chain of Responsibility Pattern trong HUDD-TDS

## 1. Mục đích
Chain of Responsibility Pattern là mẫu thiết kế thuộc nhóm **Hành vi (Behavioral Pattern)**. Mục đích chính của Chain of Responsibility trong HUDD-TDS là:
- **Tách rời các bộ phân tích cú pháp (Parser Decoupling)**: Chuyển đổi logic phân tích chuỗi dữ liệu thô từ cấu trúc lệnh `if-else` lồng nhau thành một chuỗi các bộ xử lý (Handlers) độc lập.
- **Tiền xử lý và làm sạch dữ liệu stream (Data Preprocessing)**: Tự động lọc bỏ các dòng trống, dòng chú thích (`#`, `//`) mà không làm gián đoạn luồng stream.
- **Mở rộng định dạng dữ liệu linh hoạt (Open/Closed Principle)**: Dễ dàng bổ sung thêm các định dạng giao dịch mới trong tương lai (JSON, CSV, Parquet) chỉ bằng cách tạo thêm một Handler mới cắm vào Pipeline.

---

## 2. Luồng sử dụng & Vị trí áp dụng

```text
Chuỗi ký tự dòng thô (String line)
             │
             ▼
  TransactionParsingPipeline
             │
             ▼
[EmptyAndCommentFilterHandler] ──── Dòng trống hoặc bắt đầu bằng #, // ──► Bỏ qua (trả về null)
             │ (Không khớp)
             ▼
   [SpmfFormatHandler] ──────────── Khớp cú pháp "items : TU : utils" ──► Trả về Transaction
             │ (Không khớp)
             ▼
  [LegacyFormatHandler] ─────────── Khớp cú pháp "item:qty ..." ────────► Trả về Transaction
             │ (Không khớp)
             ▼
   Kết thúc chuỗi (Handler tiếp theo là null) ──────────────────────────► Báo lỗi định dạng không hỗ trợ
```

### Các lớp tham gia:
| Tệp / Lớp | Vai trò | Trách nhiệm |
| :--- | :--- | :--- |
| [`TransactionLineHandler.java`](../src/huddtds/data/pipeline/TransactionLineHandler.java) | **Handler Interface** | Định nghĩa phương thức `handle(line, tid)` và `setNext(handler)`. |
| [`EmptyAndCommentFilterHandler.java`](../src/huddtds/data/pipeline/EmptyAndCommentFilterHandler.java) | **Concrete Handler** | Bỏ qua dòng trống, dòng khoảng trắng hoặc dòng bắt đầu bằng dấu chú thích `#`, `//`. |
| [`SpmfFormatHandler.java`](../src/huddtds/data/pipeline/SpmfFormatHandler.java) | **Concrete Handler** | Nhận diện và bóc tách định dạng SPMF / HUIM chuẩn (`item1 item2 : TU : u1 u2`). |
| [`LegacyFormatHandler.java`](../src/huddtds/data/pipeline/LegacyFormatHandler.java) | **Concrete Handler** | Nhận diện và bóc tách định dạng Running Example cổ điển (`a:1 c:6 e:2`). |
| [`TransactionParsingPipeline.java`](../src/huddtds/data/pipeline/TransactionParsingPipeline.java) | **Pipeline / Client** | Điều phối chuỗi liên kết các handler và cung cấp phương thức `parse(line, tid)` cho ứng dụng. |
| [`TransactionParser.java`](../src/huddtds/data/TransactionParser.java) | **Integration Entry** | Điểm truy cập tĩnh truyền thống của hệ thống, nay ủy quyền toàn bộ cho `TransactionParsingPipeline`. |

---

## 3. Cấu trúc Trước & Sau khi áp dụng (Before vs After)

### ❌ Trước khi áp dụng:
Phương thức tĩnh `TransactionParser.parseLine` chứa chuỗi `if-else` cứng nhắc:
```java
public static Transaction parseLine(String line, int tid) {
    if (trimmed.contains(":")) {
        String[] parts = trimmed.split(":");
        if (parts.length == 3) {
            return parseSpmfFormat(parts, tid);
        }
    }
    return parseLegacyFormat(trimmed, tid); // Dễ phát sinh lỗi nếu gặp dòng chú thích!
}
```

### ✅ Sau khi áp dụng:
Mỗi loại định dạng được đóng gói thành một lớp Handler riêng biệt và xâu chuỗi linh hoạt:
```java
// Khởi tạo Pipeline theo thứ tự ưu tiên xử lý
TransactionParsingPipeline pipeline = new TransactionParsingPipeline(
    new EmptyAndCommentFilterHandler(),
    new SpmfFormatHandler(),
    new LegacyFormatHandler()
);

// Xử lý dòng thông qua chuỗi trách nhiệm
Transaction tx = pipeline.parse(line, tid);
```

---

## 4. Mối quan hệ với các mẫu thiết kế khác
1. **Hỗ trợ Facade Pattern (`SimulationFacade`)**:
   Khi `SimulationFacade` mở transaction stream từ các tệp dataset hoặc từ ô nhập tay, dữ liệu đi thẳng qua `TransactionParser` (vốn được vận hành bởi Pipeline), giúp che giấu hoàn toàn sự phức tạp của việc phân tách định dạng.
2. **Hỗ trợ Flyweight và Object Pool**:
   Các handler như `SpmfFormatHandler` và `LegacyFormatHandler` tự động kết hợp với `ItemFactory` để nạp các đối tượng Flyweight và mượn `Transaction` từ `TransactionPool`, tạo nên một đường ống khép kín tối ưu cả CPU lẫn RAM.

---

## 5. Kiểm chứng thực nghiệm

Bài kiểm thử tự động [`ParsingPipelineChainTest.java`](../test/ParsingPipelineChainTest.java) xác nhận:
- Lọc thành công các dòng rỗng, dòng chứa tab/khoảng trắng, dòng chú thích `#` và `//`.
- Bóc tách chính xác định dạng SPMF/HUIM gồm 3 thành phần (items, TU, utilities).
- Bóc tách chính xác định dạng Running Example cổ điển theo cặp `item:quantity`.
- Cơ chế chuyển tiếp trách nhiệm (`setNext()`) hoạt động chuẩn xác từ handler này sang handler kế tiếp khi dòng dữ liệu không thuộc phạm vi xử lý của handler hiện tại.

