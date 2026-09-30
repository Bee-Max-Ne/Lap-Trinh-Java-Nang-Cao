# TÀI LIỆU KIẾN TRÚC HỆ THỐNG HUDD-TDS (SYSTEM ARCHITECTURE)

## 1. Giới thiệu & Mục tiêu Hệ thống

Hệ thống **HUDD-TDS (High Utility Drift Detection in Transactional Data Streams)** được xây dựng nhằm mô phỏng, giám sát và phát hiện sự trôi dạt độ lợi (Utility Drift) của các tập mục có độ lợi cao (High Utility Itemsets - HUI) trong luồng dữ liệu giao dịch liên tục.

Hệ thống giải quyết 4 bài toán cốt lõi:
1. **Xử lý luồng dữ liệu trượt (Sliding Window)** kết hợp hàm suy giảm độ lợi theo thời gian (**Time-Decay Function**) để ưu tiên các giao dịch mới.
2. **Khai phá HUI tốc độ cao (Fast HUI Mining)** nhờ nguyên lý cận trên **Transaction-Weighted Downward Closure (TWDC)**.
3. **Phát hiện Trôi dạt Toàn cục (Global Drift)** bằng kiểm định thống kê **Hoeffding's Bound** dựa trên sự biến thiên khoảng cách không gian đa chiều $DIS_{HS}$.
4. **Phát hiện Trôi dạt Cục bộ (Local Drift)** bằng **Hiệu chỉnh Bonferroni** để khoanh vùng chính xác tập mục nào đang thay đổi hành vi tiện ích.

---

## 2. Kiến trúc 5 Tầng Phân Lập (5-Tier Architecture)

```text
┌────────────────────────────────────────────────────────┐
│           1. TẦNG GIAO DIỆN (GUI Layer)                │
│   HUDD_TDS_GUI  •  ChartPanel  •  SimulationWorker     │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│      2. TẦNG DỮ LIỆU & KIỂM ĐỊNH (Data Layer)          │
│   TransactionParser • InvestmentLoader                 │
│   DatasetManager    • DatasetValidator • DatasetInfo   │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│     3. TẦNG THUẬT TOÁN / ĐIỀU PHỐI (Algorithm)         │
│   HUDD_TDS  •  HUIDiscovery (TWU Pruning)              │
│   GlobalDriftDetector  •  LocalDriftDetector           │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│        4. TẦNG TOÁN HỌC & ĐỘ ĐO (Math Layer)           │
│   UtilityMetrics (Decay, Hoeffding, Bonferroni, D_mo)  │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│       5. TẦNG MÔ HÌNH THỰC THỂ (Model Layer)           │
│   Element • Transaction • HighUtilityItemset           │
│   Checkpoint • ItemsetVector • DriftResult             │
└────────────────────────────────────────────────────────┘
```

---

## 3. Chi tiết Các Tầng Chức Năng

### 3.1. Tầng Mô hình Thực thể (`huddtds.model`)
- **`Element`**: Biểu diễn một món hàng. Hỗ trợ đa hình: chế độ số lượng (`quantity`) cho running example và chế độ tiện ích trực tiếp (`utility`) cho dataset thực tế SPMF/HUIM.
- **`Transaction`**: Biểu diễn một giao dịch. Tích hợp bảng băm `elementMap` giúp các thao tác `containsItem(item)` và `getUtility(item)` đạt độ phức tạp **$O(1)$** thay vì duyệt tuyến tính $O(k)$.
- **`HighUtilityItemset`**: Tập mục đạt ngưỡng `minutil`, lưu trữ utility từng phần tử và khoảng cách $D_{mo}$ tới gốc (Root).
- **`Checkpoint`**: Mốc kiểm tra chu kỳ thời gian (khi `TID % interval == 0`), lưu danh sách HUI và tổng khoảng cách toàn cục $DIS_{HS}$.
- **`ItemsetVector`**: Chuyển đổi tập mục thành vector không gian đa chiều để tính toán Cosine Similarity.
- **`DriftResult`**: Mô hình hóa kết quả kiểm định trôi dạt (loại drift, thống kê, ngưỡng, tập mục bị ảnh hưởng).

### 3.2. Tầng Dữ liệu & Kiểm định (`huddtds.data`)
- **`TransactionParser`**: Phân tích cú pháp dòng giao dịch. Tự động tương thích cả 2 định dạng:
  - SPMF/HUIM: `item1 item2 ... : TU : u1 u2 ...`
  - Legacy format: `item1:qty1 item2:qty2 ...`
  - Bắt lỗi khi số lượng item không khớp số lượng utility hoặc giá trị $TU$ bị sai lệch.
- **`InvestmentLoader`**: Đọc bảng `investment_table.txt`, bỏ qua dòng tiêu đề và chuẩn hóa thành `Map<String, Double>`.
- **`DatasetValidator`**: Kiểm tra 6 tiêu chuẩn toàn vẹn trước khi cho phép thuật toán chạy:
  1. File `transactions.txt` và `investment_table.txt` tồn tại và không rỗng.
  2. Định dạng giao dịch hợp lệ.
  3. Số item bằng số utility trên từng dòng.
  4. Tổng utility của từng item bằng đúng giá trị $TU$ ghi trong file ($|TU - \sum u| \le 0.05$).
  5. Tất cả ItemID và Investment đều hợp lệ, không âm.
- **`DatasetManager`**: Tự động phát hiện 9 thư mục dataset trong `data/datasets/`, hỗ trợ mở luồng đọc đệm (`BufferedReader` 64KB) để xử lý streaming mà không giữ toàn bộ tệp vào RAM.

### 3.3. Tầng Toán học & Thống kê (`huddtds.math`)
- **Hàm suy giảm thời gian (Time-decay)**:
  $$d_\lambda(\Delta t) = 2^{-\frac{\Delta t}{2}} \quad (\text{với } \Delta t = t_{curr} - t_{tx} \ge 0)$$
- **Khoảng cách $D_{mo}$ (Magnitude & Orientation)**:
  $$S_{cos} = \frac{\sum x_i}{\|X\| \cdot \sqrt{k}}, \quad S_{mo} = S_{cos} \cdot \left(1 - \frac{|\|X\| - \sqrt{k}|}{\max(\|X\|, \sqrt{k})}\right), \quad D_{mo} = 1 - S_{mo}$$
- **Kiểm định Hoeffding Bound cho Global Drift (Equation 8)**:
  $$\epsilon = \sqrt{\frac{n - m}{2nm} \ln\left(\frac{2}{\alpha}\right)}$$
- **Kiểm định Bonferroni cho Local Drift (Equation 9)**:
  $$\alpha' = \frac{\alpha}{m}, \quad \epsilon_{\alpha'} = \sqrt{2m\sigma^2 \ln\left(\frac{2\ln n}{\alpha'}\right)} + \frac{2m}{3}\ln\left(\frac{2\ln n}{\alpha'}\right)$$

### 3.4. Tầng Thuật toán (`huddtds.algorithm`)
- **`HUIDiscovery`**:
  - Áp dụng kỹ thuật lọc TWU (**Transaction-Weighted Utility**): Loại bỏ ngay từ đầu các item có $TWU(item) \times decay < minutil$.
  - Tỉa cận trên nhánh con candidate (**TWDC pruning**): Ngừng duyệt sâu nếu nhánh con không đủ điều kiện đạt `minutil`.
  - Tham số `maxItemsetSize` linh hoạt để hạn chế nổ tổ hợp trên các tập dữ liệu dày như Chess và Connects.
- **`HUDD_TDS`**:
  - Quản trị bộ nhớ trượt: Tự động dọn dẹp các giao dịch có $TID \le currentTID - 3 \times windowSize$, đảm bảo RAM luôn ổn định dưới 50 MB ngay cả khi xử lý luồng hàng triệu giao dịch của Chainstore.
- **`GlobalDriftDetector` & `LocalDriftDetector`**:
  - Kiểm tra sự trôi dạt tại mỗi checkpoint và phát hiện chiều hướng TĂNG / GIẢM hoặc tập mục bị ảnh hưởng.

### 3.5. Tầng Giao diện (`huddtds.demo`)
- **`HUDD_TDS_GUI`**:
  - Tích hợp `SwingWorker` (`SimulationWorker`) để chạy mô phỏng ngầm trong background thread, giải phóng hoàn toàn Event Dispatch Thread (EDT).
  - Tự động điền tham số tối ưu khi chọn từng Dataset trong ComboBox.
  - Bảng nhật ký HUI, Bảng nhật ký Drift, Progress Bar và `ChartPanel` vẽ đồ thị trực quan thời gian thực.
  - Hỗ trợ dừng luồng (STOP), đặt lại (RESET) và xuất kết quả ra file CSV.

---

## 4. Luồng Xử Lý Luồng Dữ Liệu (Streaming Data Flow)

```text
Dataset thực tế (transactions.txt + investment_table.txt)
                     │
                     ▼
             DatasetValidator (Kiểm định toàn vẹn)
                     │
                     ▼
             TransactionParser (Parse từng dòng)
                     │
                     ▼
          HUDD_TDS Engine (Sliding Window & Memory Truncation)
                     │
     ┌───────────────┴───────────────┐
     ▼                               ▼
(Chưa đến Interval)            (TID % Interval == 0)
Bỏ qua Checkpoint              KÍCH HOẠT CHECKPOINT
                                     │
                                     ▼
                     HUIDiscovery (TWU + Decay + TWDC Pruning)
                                     │
                             ┌───────┴───────┐
                             ▼               ▼
                     GlobalDriftDetector  LocalDriftDetector
                     (Hoeffding Bound)    (Bonferroni Correction)
                             │               │
                             └───────┬───────┘
                                     │
                                     ▼
                     Cập nhật Giao diện (SwingWorker)
                     ├─ Thẻ chỉ số (Current TID, HUI Count, DIS_HS)
                     ├─ Bảng Nhật ký HUI (JTable)
                     ├─ Bảng Nhật ký Drift (JTable)
                     ├─ Biểu đồ biến thiên (ChartPanel)
                     └─ Xuất báo cáo CSV
```
