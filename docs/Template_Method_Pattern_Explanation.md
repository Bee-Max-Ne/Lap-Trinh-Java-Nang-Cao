# Template Method Pattern trong HUDD-TDS

## 1. Mục đích
Template Method Pattern là mẫu thiết kế thuộc nhóm **Hành vi (Behavioral Pattern)**. Mục đích chính của Template Method trong HUDD-TDS là:
- **Chuẩn hóa bộ khung kiểm định trôi dạt (Drift Detection Framework)**: Định nghĩa kịch bản kiểm định cố định 6 bước bất biến tại lớp cha trừu tượng `AbstractDriftDetector`.
- **Loại bỏ mã trùng lặp cấu trúc**: Các bộ phát hiện trôi dạt (`GlobalDriftDetector`, `LocalDriftDetector`) không cần phải viết lại kịch bản kiểm tra dữ liệu, so sánh giá trị và ghi log.
- **Tăng cường và phối hợp chặt chẽ với Strategy Pattern**: Strategy chịu trách nhiệm phân quyền bên ngoài (`HUDD_TDS`), trong khi Template Method chịu trách nhiệm quy định trình tự bên trong của từng thuật toán.

---

## 2. Luồng sử dụng & Vị trí áp dụng

```text
HUDD_TDS Engine
     │
     │ Ủy quyền gọi (Strategy API)
     ▼
AbstractDriftDetector.runDetection (final Template Method)
     │
     ├── 1. hasEnoughData(input) ────► [Nếu false: Dừng sớm, trả về noDrift]
     │
     ├── 2. computeStatistic(input) ──► Lớp con tính toán đại lượng thống kê
     │
     ├── 3. computeEpsilon(input) ────► Lớp con tính ngưỡng epsilon lý thuyết
     │
     ├── 4. Phán quyết cố định: |statistic| >= epsilon
     │
     ├── 5. traceResult(...) ────────► Ghi trace chẩn đoán chuẩn hóa
     │
     └── 6. buildDetectedResult / buildNoDriftResult ──► Trả về DriftResult
```

### Các lớp tham gia:
| Tệp / Lớp | Vai trò | Trách nhiệm |
| :--- | :--- | :--- |
| [`AbstractDriftDetector.java`](../src/huddtds/algorithm/drift/AbstractDriftDetector.java) | **Abstract Class (Template)** | Khai báo phương thức khuôn mẫu `final runDetection(DriftInput)` chứa 6 bước cố định; định nghĩa các hook methods bắt buộc và tùy chọn. |
| [`GlobalDriftDetector.java`](../src/huddtds/algorithm/GlobalDriftDetector.java) | **Concrete Class** | Kế thừa `AbstractDriftDetector` và cài đặt `GlobalDriftStrategy`. Cài đặt hook tính $|U_{drift} - V|$ và ngưỡng $\epsilon$ theo Equation (8) / Hoeffding bound. |
| [`LocalDriftDetector.java`](../src/huddtds/algorithm/LocalDriftDetector.java) | **Concrete Class** | Kế thừa `AbstractDriftDetector` và cài đặt `LocalDriftStrategy`. Cài đặt hook tính chênh lệch utility từng itemset và ngưỡng $\epsilon$ theo Equation (9) / Bonferroni. |

---

## 3. Cấu trúc Trước & Sau khi áp dụng (Before vs After)

### ❌ Trước khi áp dụng:
Mỗi lớp detector phải tự quản lý việc kiểm tra dữ liệu đầu vào, tự tính toán và tự quyết định cấu trúc kết quả trả về:
```java
public class GlobalDriftDetector implements GlobalDriftStrategy {
    // Tự lặp lại kiểm tra n < 2
    // Tự so sánh difference >= epsilon
    // Tự ghi log trace phân tán
}
```

### ✅ Sau khi áp dụng:
Lớp cha kiểm soát toàn bộ vòng đời thực thi, lớp con chỉ cần cung cấp công thức tính toán:
```java
public abstract class AbstractDriftDetector {
    public final DriftResult runDetection(DriftInput input) {
        if (!hasEnoughData(input)) return buildNoDriftResult(input);
        double statistic = computeStatistic(input);
        double epsilon = computeEpsilon(input);
        boolean detected = Math.abs(statistic) >= epsilon;
        traceResult(input, statistic, epsilon, detected);
        return detected ? buildDetectedResult(input, statistic, epsilon) : buildNoDriftResult(input);
    }
}
```

---

## 4. Sự phối hợp ăn ý giữa Template Method và Strategy Pattern

> [!TIP]
> **Strategy** và **Template Method** giải quyết 2 góc nhìn bổ trợ cho nhau:
> - **Strategy Pattern**: Ranh giới bên ngoài — cho phép `HUDD_TDS` tiêm và hoán đổi linh hoạt giữa các bộ dò drift (`GlobalDriftStrategy`, `LocalDriftStrategy`).
> - **Template Method Pattern**: Cấu trúc bên trong — chuẩn hóa quy trình tính toán 6 bước cho mọi bộ dò drift, đảm bảo không bộ dò nào bỏ quên bước kiểm tra dừng sớm hay ghi trace.
>
> ➜ **Strategy quy định "AI LÀM", còn Template Method quy định "LÀM NHƯ THẾ NÀO".**

---

## 5. Kiểm chứng thực nghiệm

Bài kiểm thử tự động [`TemplateMethodPatternTest.java`](../test/TemplateMethodPatternTest.java) xác nhận:
- Khi `hasEnoughData() == false`: Luồng thực thi lập tức dừng lại (short-circuit), không tính toán thống kê hay epsilon tốn kém.
- Khi `|statistic| >= epsilon`: Luồng gọi đầy đủ theo đúng thứ tự 6 bước và trả về `DriftResult` với trạng thái `detected = true`.
- Kế thừa đa hình hoạt động hoàn hảo, bảo toàn tính tương thích với toàn bộ mã nguồn cũ.

