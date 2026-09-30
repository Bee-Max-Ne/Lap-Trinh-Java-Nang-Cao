# HUDD-TDS: Hệ Thống Giám Sát Trôi Dạt Độ Lợi (Utility Drift Detection)

Hệ thống mô phỏng, khai phá và giám sát sự thay đổi của các tập mục có độ lợi cao (**High Utility Itemsets - HUI**) và phát hiện hiện tượng trôi dạt độ lợi (**Concept / Utility Drift**) trên luồng dữ liệu giao dịch (**Transactional Data Streams**), hiện thực hóa thuật toán nghiên cứu **HUDD-TDS** (*Duong et al., High Utility Drift Detection in Quantitative Data Streams*).

---

## 1. Các Tính Năng Cốt Lõi

- **Khai phá HUI trên Cửa sổ Trượt (Sliding Window HUIM)**: Khai thác các tập mục có độ lợi cao trong cửa sổ giao dịch gần nhất, áp dụng hàm suy giảm thời gian (**Time-Decay Function**) $d_\lambda(\Delta t) = 2^{-\frac{\Delta t}{2}}$ để ưu tiên dữ liệu mới.
- **Kỹ thuật Tỉa Cận Trên TWU (Transaction-Weighted Downward Closure)**: Tối ưu hóa khai phá HUI với tốc độ cao, xử lý mượt mà các tập dữ liệu dày (dense datasets) có hàng chục item/giao dịch.
- **Quản Trị Bộ Nhớ Trượt (Sliding Memory Control)**: Tự động dọn dẹp các giao dịch cũ ngoài phạm vi cửa sổ, đảm bảo ứng dụng chạy ổn định trên luồng hàng triệu giao dịch mà không bị tràn RAM.
- **Phát Hiện Trôi Dạt Toàn Cục (Global Utility Drift)**: Sử dụng kiểm định thống kê **Hoeffding's Bound** trên chuỗi khoảng cách toàn cục $DIS_{HS}$ qua các checkpoint thời gian.
- **Phát Hiện Trôi Dạt Cục Bộ (Local Utility Drift)**: Sử dụng **Hiệu chỉnh Bonferroni (Bonferroni Correction)** để phát hiện chính xác từng tập mục cụ thể đang bị trôi dạt tiện ích.
- **Tầng Dữ Liệu Chuẩn Hóa (Data Layer)**: Hỗ trợ nạp và kiểm định tự động cả 9 bộ benchmark dataset thực tế (SPMF/HUIM format) và dữ liệu mẫu.
- **Giao Diện Trực Quan Đa Luồng (Java Swing + SwingWorker)**: Xử lý luồng ngầm không gây đơ/treo giao diện, tích hợp thanh tiến trình, thẻ chỉ số thời gian thực, bảng nhật ký HUI, nhật ký Drift và biểu đồ đường trực quan.

---

## 2. Kiến Trúc Hệ Thống (Layered Architecture)

Hệ thống được tổ chức phân tầng chặt chẽ thành 5 package chính:

```text
2312718_BTH01/
├── data/
│   ├── datasets/                       # 9 bộ dữ liệu chuẩn hóa
│   │   ├── Accidents/                  # 340,183 giao dịch, 468 items
│   │   ├── Chainstore/                 # 1,112,949 giao dịch, 46,086 items
│   │   ├── Chess/                      # 3,196 giao dịch, 75 items
│   │   ├── Connects/                   # 67,557 giao dịch, 129 items
│   │   ├── Mushrooms/                  # 8,416 giao dịch, 119 items
│   │   ├── NewChess/                   # Bản đối sánh trôi dạt của Chess
│   │   ├── NewConnects/                # Bản đối sánh trôi dạt của Connects
│   │   ├── NewMushroom/                # Bản đối sánh trôi dạt của Mushroom
│   │   └── Retail/                     # 88,162 giao dịch, 16,470 items
│   └── running_example.txt             # Dữ liệu mô phỏng theo bài báo
│
├── logs/                               # Thư mục lưu nhật ký benchmark tự động
│
├── src/huddtds/
│   ├── model/                          # [TẦNG MODEL]
│   │   ├── Element.java                # Phần tử (hỗ trợ cả quantity và direct utility)
│   │   ├── Transaction.java            # Giao dịch có tra cứu O(1) và tổng TU
│   │   ├── HighUtilityItemset.java     # Tập mục độ lợi cao & khoảng cách D_mo
│   │   ├── Checkpoint.java             # Mốc kiểm tra thời gian theo interval
│   │   ├── ItemsetVector.java          # Biểu diễn vector tiện ích
│   │   └── DriftResult.java            # Kết quả phát hiện trôi dạt
│   │
│   ├── data/                           # [TẦNG DỮ LIỆU]
│   │   ├── TransactionParser.java      # Parser định dạng SPMF (item:TU:utils) & legacy
│   │   ├── InvestmentLoader.java       # Nạp bảng investment_table.txt
│   │   ├── DatasetInfo.java            # Metadata của Dataset
│   │   ├── DatasetValidator.java       # Bộ kiểm định 6 tiêu chuẩn toàn vẹn dữ liệu
│   │   └── DatasetManager.java         # Quét danh mục & mở Stream đọc tệp lớn
│   │
│   ├── math/                           # [TẦNG TOÁN HỌC & ĐỘ ĐO]
│   │   └── UtilityMetrics.java         # Decay, Hoeffding, Bonferroni, Cosine, D_mo
│   │
│   ├── algorithm/                      # [TẦNG THUẬT TOÁN]
│   │   ├── HUDD_TDS.java               # Bộ điều phối luồng trung tâm
│   │   ├── HUIDiscovery.java           # Khai phá HUI với TWU pruning
│   │   ├── GlobalDriftDetector.java    # Kiểm định trôi dạt toàn cục (Equation 8)
│   │   └── LocalDriftDetector.java     # Kiểm định trôi dạt cục bộ (Equation 9)
│   │
│   └── demo/                           # [TẦNG GIAO DIỆN & DEMO]
│       ├── HUDD_TDS_GUI.java           # Giao diện Swing với SwingWorker đa luồng
│       ├── ChartPanel.java             # Vẽ biểu đồ biến thiên Global Distance
│       └── DemoRunner.java             # Điểm chạy demo dòng lệnh
│
└── test/                               # [BỘ KIỂM THỬ TỰ ĐỘNG]
    ├── BaselineRunner.java             # Kiểm tra hồi quy kết quả gốc
    ├── DataLayerTest.java              # Unit Test tầng dữ liệu (Mốc 1)
    ├── EndToEndChessRunner.java        # Chạy End-to-End toàn bộ Chess (Mốc 2)
    ├── DriftPairBenchmark.java         # Chạy luồng ghép kiểm thử drift cặp New*
    └── FullBenchmarkSuite.java         # Bộ Benchmark toàn diện 9 dataset
```

---

## 3. Định Dạng Dữ Liệu

Mỗi thư mục dataset chuẩn bao gồm 2 file:

1. **`transactions.txt`** (Định dạng SPMF/HUIM):
   ```text
   <danh_sách_item>:<tổng_utility_giao_dịch_TU>:<utility_từng_item>
   ```
   *Ví dụ:* `1 3 5 7:11699429.00:75465.00 118984.00 561780.00 32025.00`
   - Phần 1: Mã các item trong giao dịch.
   - Phần 2: Tổng độ lợi giao dịch ($TU = \sum u(i, T)$).
   - Phần 3: Độ lợi thực tế của từng item tương ứng.

2. **`investment_table.txt`**:
   ```text
   ItemID	Total Investment
   ==============================
   1	187318730.87
   2	645166.36
   ```

---

## 4. Hướng Dẫn Biên Dịch & Khởi Chạy

### 4.1. Biên dịch toàn bộ dự án
Mở PowerShell tại thư mục `2312718_BTH01/` và thực thi:
```powershell
javac -encoding UTF-8 -d out $(Get-ChildItem -Path src,test -Filter *.java -Recurse | Select-Object -ExpandProperty FullName)
```

### 4.2. Khởi chạy Giao diện Swing (GUI)
```powershell
java -cp out huddtds.demo.HUDD_TDS_GUI
```
**Các tính năng linh hoạt vượt trội trên GUI:**
1. **Nguồn dữ liệu linh hoạt**: Chọn nhanh từ 18 dataset chuẩn hoặc nhấn **"📁 Duyệt..."** để nạp tệp `transactions.txt` và `investment_table.txt` bất kỳ từ ổ đĩa máy tính.
2. **Giới hạn số giao dịch (`Max Tx`)**: Nhập số giao dịch (ví dụ: `500`, `1000`) để kiểm tra nhanh trong vài giây hoặc nhập `0` để chạy toàn bộ dataset.
3. **Điều khiển phát luồng toàn diện**:
   - **"▶ Phát luồng (RUN)"**: Khởi chạy luồng giao dịch ngầm đa luồng qua `SwingWorker`.
   - **"⏸ Tạm dừng / Tiếp tục"**: Tạm dừng giữa chừng để soi kỹ các chỉ số/bảng rồi tiếp tục phát luồng.
   - **"⏹ Dừng (STOP)" & "🔄 Đặt lại"**: Hủy luồng hoặc đặt lại giao diện về ban đầu.
   - **"Thanh trượt Tốc độ trễ"**: Điều chỉnh từ `0 ms` (chạy batch nhanh tối đa) đến `200 ms` (làm chậm để quan sát trực quan từng checkpoint).
4. **Bộ lọc & Tìm kiếm tương tác**:
   - Ô tìm kiếm lọc Itemset trong Bảng HUI bằng Regex thời gian thực.
   - Checkbox "Tự động cuộn theo dòng mới".
   - Lọc phân loại sự kiện trong Bảng Drift (Tất cả / Chỉ Global / Chỉ Local).
5. **Đồ thị tương tác (ChartPanel)**:
   - Chuyển đổi linh hoạt giữa 2 chỉ số: *Khoảng cách toàn cục (DISHS)* hoặc *Số lượng HUI*.
   - Điểm Drift toàn cục được đánh dấu màu ĐỎ nổi bật kèm quầng cảnh báo.
   - Hover chuột xem Tooltip chi tiết của từng Checkpoint (TID, DISHS, số HUI, trạng thái ổn định/drift).
6. **Xuất báo cáo đa định dạng**: Menu xuất linh hoạt ra CSV cho Bảng HUI, Bảng Drift hoặc Báo cáo thống kê tổng thể TXT.

### 4.3. Chạy các bộ kiểm thử độc lập
- **Kiểm thử Tầng Dữ Liệu (Mốc 1)**:
  ```powershell
  java -cp out test.DataLayerTest
  ```
- **Kiểm thử End-to-End Chess (Mốc 2)**:
  ```powershell
  java -cp out test.EndToEndChessRunner
  ```
- **Kiểm thử đối sánh cặp dữ liệu trôi dạt (Chess vs NewChess, Mushrooms vs NewMushroom)**:
  ```powershell
  java -cp out test.DriftPairBenchmark
  ```
- **Chạy Benchmark tổng hợp 9 Dataset & xuất log**:
  ```powershell
  java -cp out test.FullBenchmarkSuite
  ```
