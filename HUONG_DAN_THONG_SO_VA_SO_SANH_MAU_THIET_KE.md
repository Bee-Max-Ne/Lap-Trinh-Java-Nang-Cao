# HƯỚNG DẪN THAM SỐ THỰC NGHIỆM & PHƯƠNG PHÁP SO SÁNH HIỆU NĂNG MẪU THIẾT KẾ (DESIGN PATTERNS)

Tài liệu này cung cấp hướng dẫn toàn diện về các thay đổi vừa được nâng cấp trên nhánh **`pre-design-patterns`**, chi tiết tác dụng của từng tham số thuật toán, các chỉ số hiệu năng và phương pháp chuẩn mực để thực hiện bài toán so sánh đối chứng trước và sau khi áp dụng các mẫu thiết kế phần mềm (**Design Patterns**).

---

## MỤC LỤC
1. [Tổng hợp các thay đổi vừa hoàn thành](#1-tổng-hợp-các-thay-đổi-vừa-hoàn-thành)
2. [Tác dụng của các tham số thuật toán & kịch bản cao / thấp](#2-tác-dụng-của-các-tham-số-thuật-toán--kịch-bản-cao--thấp)
3. [Tác dụng của các chỉ số tài nguyên & hiệu năng vừa bổ sung](#3-tác-dụng-của-các-chỉ-số-tài-nguyên--hiệu-năng-vừa-bổ-sung)
4. [Phương pháp so sánh giữa hai nhánh (Trước vs Sau áp dụng Design Patterns)](#4-phương-pháp-so-sánh-giữa-hai-nhánh-trước-vs-sau-áp-dụng-design-patterns)
5. [Quy trình thực hiện bài kiểm thử đối chứng chuẩn](#5-quy-trình-thực-hiện-bài-kiểm-thử-đối-chứng-chuẩn)

---

## 1. TỔNG HỢP CÁC THAY ĐỔI VỪA HOÀN THÀNH

Nhánh `pre-design-patterns` đóng vai trò là **chuẩn gốc (Baseline)** — nơi thuật toán chạy mà chưa áp dụng các mẫu thiết kế tối ưu đối tượng (như Object Pool, Flyweight). Các nâng cấp trực quan và đo lường vừa được hoàn tất bao gồm:

### 1.1. Mở rộng mô hình dữ liệu (`Checkpoint.java`)
- Thêm trường `ramUsageMb` (Mức tiêu thụ bộ nhớ RAM Heap tính bằng MB tại thời điểm checkpoint).
- Thêm trường `throughputTxPerSec` (Tốc độ thông lượng giao dịch xử lý trên giây tại thời điểm checkpoint).
- Cập nhật luồng `SimulationWorker` để đo lường và gán giá trị thời gian thực cho từng đối tượng Checkpoint.

### 1.2. Nâng cấp toàn diện Đồ thị phát luồng (`ChartPanel.java`)
- **Tích hợp 5 chế độ trực quan qua menu thả xuống (Dropdown)**:
  1. *Khoảng cách toàn cục (DISHS)*: Biểu đồ đường biến thiên phân bố tiện ích theo thang đo log (`log1p`).
  2. *Số lượng HUI (Dạng đường)*: Biểu diễn số lượng tập mục độ lợi cao theo thang đo tuyến tính.
  3. *Số lượng HUI (Cột phân màu)*: Biểu đồ cột tự động đổi màu Đỏ khi xảy ra Trôi dạt (Drift) và Xanh dương khi luồng ổn định.
  4. *Tiêu thụ RAM (MB)*: Đồ thị diện tích gradient xanh ngọc (Emerald) theo dõi sát sao heap RAM thực tế theo thang đo tuyến tính (Linear Scale).
  5. *Thông lượng (tx/giây)*: Đồ thị gradient tím (Violet) phản ánh tốc độ xử lý giao dịch thực tế theo thang đo tuyến tính.
- **Khử hiện tượng Over-plotting (Chống đè lấn điểm vẽ)**:
  - Khi số lượng checkpoint lớn ($N > 35$ và $N > 85$ điểm): Tự động thu gọn hoặc loại bỏ quầng phát sáng to, chỉ giữ đường xu hướng mượt mà và các mốc Drift nhỏ gọn (3–4px).
  - Không còn hiện tượng các vòng tròn đè lên nhau thành một "con sâu màu đỏ" che khuất đồ thị.
  - Khi rê chuột (Hover), điểm tương ứng mới phóng to và hiển thị đường gióng kèm Tooltip.
- **Vạch chỉ báo Đỉnh (Peak Line)**:
  - Tự động vẽ một đường nét đứt màu đỏ tại mốc cao nhất kèm nhãn (`Đỉnh: xxx MB` trên biểu đồ RAM và `Đỉnh: xxx tx/s` trên biểu đồ Thông lượng).
- **Hệ thống chú giải động (Dynamic Legend)**:
  - Tự động thay đổi nội dung chữ và biểu tượng phù hợp với từng chỉ số (ví dụ: `RAM Heap (Ổn định)` vs `RAM khi có Drift`).
  - Tự động căn lề phải theo `FontMetrics`, chống tràn viền và không đè lên tiêu đề.
- **Tooltip thông minh (Adaptive Tooltip)**:
  - Thay đổi thứ tự ưu tiên thông tin theo đúng chỉ số người dùng đang chọn; tự động lật xuống phía dưới khi điểm nằm sát mép trên để không che khuất màn hình.

### 1.3. Xây dựng Panel Tổng quan Phân tích (`SummaryChartsPanel.java`)
- Bổ sung vào bên phải **Tab 4 ("Tổng quan Thống kê")** dạng Tab lật đôi:
  - **Biểu đồ tròn Donut Chart**: Thể hiện tỷ lệ % giữa Checkpoint Ổn định (Xanh lá), Global Drift (Đỏ), và Local Drift (Vàng cam), có tâm hiển thị tổng số checkpoint và tỷ lệ % trôi dạt tổng thể.
  - **Thanh đo Tài nguyên (Resource Progress Meters)**: Trực quan hóa % Heap sử dụng (RAM hiện tại vs RAM đỉnh vs Max Heap JVM có vạch đỏ đánh dấu đỉnh), và Thông lượng TB vs Đỉnh.
  - **Hộp giám sát Thu gom rác (GC Indicator)**: Thống kê số lần GC kích hoạt và tổng thời gian dừng Stop-the-world (ms).

### 1.4. Tích hợp Hệ thống Xuất Báo cáo Toàn diện (`ReportExporter.java`)
- **Xuất Báo cáo Toàn diện (HTML Dashboard Report) ⭐**:
  - Tự động chụp lại độ nét cao của **toàn bộ 6 biểu đồ** và nhúng dạng chuỗi Base64 trực tiếp vào 1 tệp HTML duy nhất (không cần thư mục ảnh rời, không lo hỏng đường dẫn).
  - Báo cáo định dạng chuẩn khoa học gồm: Thẻ KPI, Lưới 6 biểu đồ sắc nét, Bảng số liệu phân nhóm 4 phần, và Báo cáo văn bản raw log.
  - Tích hợp nút **"🖨 In báo cáo / Lưu PDF (Ctrl + P)"** có sẵn CSS Print chuẩn khổ giấy A4 để xuất thành file PDF nộp bài ngay lập tức.
  - Khắc phục triệt để lỗi định dạng chuỗi CSS `%# 0,f` bằng cách sử dụng `StringBuilder` ghép chuỗi thuần.
- Hỗ trợ thêm các tùy chọn: *Lưu ảnh biểu đồ hiện tại (PNG)* và *Xuất toàn bộ 5 biểu đồ ra thư mục ảnh PNG*.

---

## 2. TÁC DỤNG CỦA CÁC THAM SỐ THUẬT TOÁN & KỊCH BẢN CAO / THẤP

Bảng điều khiển phía trên của ứng dụng cung cấp 6 tham số cấu hình chính. Dưới đây là ý nghĩa và hành vi của hệ thống khi tham số được đặt ở mức Cao hoặc Thấp:

| Tham số | Ý nghĩa & Đơn vị | Khi đặt QUÁ CAO | Khi đặt QUÁ THẤP | Giá trị khuyến nghị theo Dataset |
| :--- | :--- | :--- | :--- | :--- |
| **`MinUtil`** | Ngưỡng độ lợi tối thiểu để một tập mục được công nhận là HUI (Độ lợi tuyệt đối) | • Số lượng HUI tìm thấy rất ít (hoặc = 0).<br>• Thuật toán chạy rất nhanh, RAM tiêu thụ thấp.<br>• Bỏ sót các mẫu kinh doanh có giá trị. | • **Bùng nổ tổ hợp (Combinatorial explosion)**: Số lượng HUI tăng vọt (hàng nghìn itemsets).<br>• Tiêu tốn nhiều RAM Heap, CPU cao, tốc độ xử lý luồng chậm lại. | • Running Example: `15.0`<br>• Chess: `2,000,000`<br>• Mushroom: `2,500,000`<br>• Retail: `100,000`<br>• Chainstore: `500,000` |
| **`Interval`** | Chu kỳ giao dịch giữa 2 lần tạo Checkpoint và kiểm định Drift (Số lượng Tx) | • Kiểm tra quá thưa: Phản ứng chậm với các biến đổi của luồng (Concept Drift bị phát hiện muộn).<br>• Ít điểm trên biểu đồ, đồ thị thưa thớt.<br>• Bù lại: Tốc độ streaming rất cao do ít tốn overhead tính toán. | • Kiểm tra quá dày: Mỗi vài giao dịch lại tính vector khoảng cách $DIS\_HS$ và kiểm định thống kê một lần.<br>• Tốc độ thông lượng (tx/s) giảm sút nghiêm trọng.<br>• Đồ thị có quá nhiều điểm dày đặc. | • Dataset nhỏ (< 5,000 tx): `1` – `50`<br>• Dataset vừa (10,000 – 100,000 tx): `300` – `1,000`<br>• Dataset lớn (> 500,000 tx): `2,000` – `5,000` |
| **`Window Size`** | Kích thước cửa sổ trượt (Số lượng giao dịch được lưu giữ trong cửa sổ hiện tại $W$) | • Cửa sổ lưu trữ quá nhiều giao dịch: Tốn nhiều bộ nhớ RAM để duy trì danh sách giao dịch.<br>• Quá trình fading bị chậm, phản ứng kém nhạy với trôi dạt mới xảy ra. | • Cửa sổ quá nhỏ: Không đủ cỡ mẫu thống kê để tính kỳ vọng và phương sai $\rightarrow$ Dễ xuất hiện **cảnh báo trôi dạt giả (False Positives)** do nhiễu tức thời. | • Thường đặt gấp `2x` đến `5x` giá trị của `Interval`.<br>• Ví dụ: Interval = 500 $\rightarrow$ Window = 1000 đến 2500. |
| **`Alpha`** | Mức ý nghĩa thống kê ($\alpha$) dùng trong công thức chặn Hoeffding / Chebyshev | • Ngưỡng sai số $\epsilon$ nhỏ lại: Thuật toán trở nên **quá nhạy** $\rightarrow$ Thường xuyên báo động Trôi dạt ngay cả với các biến động nhỏ không đáng kể. | • Ngưỡng sai số $\epsilon$ lớn lên: Thuật toán trở nên **quá khắt khe** $\rightarrow$ Chỉ phát hiện Drift khi phân bố thay đổi cực kỳ đột ngột, có thể bỏ sót trôi dạt từ từ (Gradual Drift). | • Chuẩn thống kê: `0.05` (Độ tin cậy 95%) hoặc `0.01` (Độ tin cậy 99%) hoặc `0.10` (Độ tin cậy 90%). |
| **`Max Len`** | Độ dài tối đa của tập mục ($k$-itemsets) được phép khai phá | • Cho phép khai phá tập mục rất dài ($k \ge 5$).<br>• Không gian tìm kiếm mở rộng theo cấp số nhân $\binom{M}{k}$, cây tìm kiếm phình to, RAM tiêu thụ lớn. | • Giới hạn tập mục ngắn ($k = 1, 2, 3$).<br>• Tiết kiệm tối đa bộ nhớ và thời gian tính toán, thích hợp cho hệ thống cần thông lượng streaming cực cao. | • Thường đặt `3` hoặc `4`. |
| **`Max Tx`** | Giới hạn số lượng giao dịch đọc từ dataset (`0` = chạy toàn bộ tệp) | • Chạy toàn bộ file (có thể lên tới hàng triệu dòng nếu file lớn), dùng để kiểm thử sức chịu tải tối đa (Stress Test). | • Nhập một số cụ thể (ví dụ: `5000`, `10000`, `20000`) để chạy thử nghiệm nhanh, lấy mẫu quan sát đồ thị mà không cần chờ cả file lớn. | • Thử nghiệm nhanh: `5000` – `20000`<br>• Benchmark toàn phần: `0` |

---

## 3. TÁC DỤNG CỦA CÁC CHỈ SỐ TÀI NGUYÊN & HIỆU NĂNG VỪA BỔ SUNG

Khi đối chiếu hiệu năng kiến trúc, các chỉ số dưới đây là thước đo quyết định:

### 3.1. Tiêu thụ RAM Heap hiện tại & RAM Heap Đỉnh (Peak Heap RAM - MB)
- **Định nghĩa**: Lượng bộ nhớ RAM trong không gian Heap của máy ảo Java (JVM) mà ứng dụng đang chiếm dụng tại mỗi thời điểm.
- **Ý nghĩa**: Đánh giá chi phí tài nguyên phần cứng cần thiết để vận hành hệ thống.
- **Hiện tượng đặc trưng trên Baseline**: Xuất hiện đường đồ thị hình răng cưa lớn (Saw-tooth pattern) — RAM tăng rất nhanh do liên tục tạo mới đối tượng (`new Transaction`, `new Element`), sau đó sụt giảm đột ngột mỗi khi GC kích hoạt.

### 3.2. Tốc độ thông lượng trung bình & Đỉnh (Throughput - tx/giây)
- **Định nghĩa**: Số lượng giao dịch trong luồng dữ liệu mà thuật toán xử lý thành công trong 1 giây (Transactions Per Second - TPS).
- **Ý nghĩa**: Tiêu chí đánh giá khả năng đáp ứng thời gian thực (Real-time capability). Tốc độ càng cao, hệ thống càng có thể xử lý các luồng dữ liệu tốc độ lớn từ thực tế (e.g., IoT, ngân hàng, thương mại điện tử).

### 3.3. Hoạt động Thu gom rác (GC Count & GC Pause Time - ms)
- **Định nghĩa**: Số lần bộ Garbage Collector của JVM phải can thiệp để dọn dẹp các đối tượng không còn sử dụng trong bộ nhớ, và tổng thời gian hệ thống bị ngưng trệ (Stop-The-World pause).
- **Ý nghĩa**: Phản ánh trực tiếp chi phí quản lý vòng đời đối tượng. Một kiến trúc tốt sẽ giảm thiểu tối đa số lần kích hoạt GC.

---

## 4. PHƯƠNG PHÁP SO SÁNH GIỮA HAI NHÁNH (TRƯỚC VS. SAU ÁP DỤNG DESIGN PATTERNS)

### 4.1. Bản chất khác biệt về mặt Kiến trúc phần mềm

| Đặc điểm | Nhánh `pre-design-patterns` (Baseline) | Nhánh `main` / `post-design-patterns` (Áp dụng Mẫu) |
| :--- | :--- | :--- |
| **Quản lý đối tượng giao dịch** | **Tạo mới trực tiếp**: Mỗi dòng giao dịch trong stream đều gọi `new Transaction(...)` và danh sách các `new Element(...)`. | **Mẫu thiết kế Object Pool**: Duy trì một hồ chứa đối tượng tái sử dụng. Khi có giao dịch mới, lấy đối tượng rỗng từ Pool ra gán giá trị; sau khi xử lý checkpoint xong, đưa đối tượng trở lại Pool để dùng tiếp. |
| **Quản lý vector và cấu trúc phụ** | Tạo mới `ItemsetVector` và các mảng tạm lặp đi lặp lại qua mỗi chu kỳ. | **Mẫu thiết kế Flyweight & Singleton**: Chia sẻ trạng thái bất biến của các mục dữ liệu, tái sử dụng vector bộ nhớ đệm. |
| **Hành vi bộ nhớ JVM** | Sinh ra hàng triệu đối tượng ngắn hạn (short-lived objects). Khi vùng nhớ Young Generation đầy, JVM buộc phải kích hoạt Minor GC liên tục. | Số lượng đối tượng hoạt động trong bộ nhớ luôn được duy trì ở mức cố định (bằng kích thước Pool). Hầu như không sinh thêm rác mới. |

### 4.2. Bảng dự báo tiêu chí so sánh thực nghiệm (Benchmark Checklist)

Khi bạn chuyển sang chạy trên nhánh có Design Patterns và đối chiếu với nhánh Baseline hiện tại, bạn sẽ quan sát thấy các khác biệt rõ rệt sau:

| Tiêu chí đánh giá | Nhánh `pre-design-patterns` (Baseline) | Nhánh Áp dụng Mẫu thiết kế (`post-design-patterns`) | Tỷ lệ cải thiện kỳ vọng |
| :--- | :--- | :--- | :--- |
| **Bộ nhớ RAM Đỉnh (Peak Heap)** | **Rất cao**: Đỉnh RAM có thể lên tới 500 MB – 1,500 MB tùy dung lượng file. | **Thấp và ổn định**: Đỉnh RAM thường chỉ duy trì quanh mức 100 MB – 300 MB. | **Tiết kiệm từ 40% – 70% RAM** |
| **Đường đồ thị RAM** | Răng cưa lớn, độ dốc tăng dựng đứng trước mỗi đợt GC. | Đường biểu diễn phẳng, biến thiên rất nhẹ nhàng và ổn định. | Đường cong bộ nhớ phẳng |
| **Số lần kích hoạt GC** | Rất nhiều (từ vài chục đến hàng trăm lần trong suốt phiên chạy). | Rất ít (chỉ kích hoạt vài lần ở giai đoạn nạp dữ liệu ban đầu). | **Giảm 75% – 90% số lần GC** |
| **Tổng thời gian dừng GC (Pause)** | Cao (hàng trăm đến hàng nghìn mili-giây bị lãng phí do dọn rác). | Rất thấp (hầu như bằng 0 ms hoặc vài mili-giây). | **Giảm 80% – 95% độ trễ GC** |
| **Thông lượng trung bình (Avg TPS)** | Thấp hơn do CPU phải san sẻ tài nguyên để chạy luồng GC. | **Cao hơn rõ rệt**, duy trì tốc độ xử lý ổn định qua các checkpoint. | **Tăng từ 25% – 60% tốc độ TPS** |
| **Số HUI & Mốc Drift phát hiện** | Kết quả khai phá tập mục và phát hiện trôi dạt. | **HOÀN TOÀN GIỐNG HỆT 100%** so với nhánh Baseline. | Đảm bảo tính toàn vẹn khoa học |

---

## 5. QUY TRÌNH THỰC HIỆN BÀI KIỂM THỬ ĐỐI CHỨNG CHUẨN

Để báo cáo khoa học hoặc đồ án đạt điểm tối đa về tính thuyết phục, bạn nên thực hiện bài kiểm tra đối chứng theo 5 bước sau:

```
[Bước 1: Chạy Baseline (pre-design-patterns)] 
        ↓ Cùng bộ tham số thực nghiệm
[Bước 2: Xuất Báo cáo HTML Baseline: Report_Baseline.html]
        ↓ 
[Bước 3: Chuyển sang nhánh Áp dụng Mẫu thiết kế (main / post-patterns)]
        ↓ Chạy lại đúng bộ tham số thực nghiệm trên
[Bước 4: Xuất Báo cáo HTML Pattern: Report_Pattern.html]
        ↓
[Bước 5: Mở 2 file HTML song song để so sánh trực quan và chụp ảnh báo cáo]
```

### Các bước thao tác chi tiết:

1. **Trên nhánh `pre-design-patterns` (Nhánh hiện tại)**:
   - Chọn một bộ dữ liệu chuẩn, ví dụ: `Chess-metadata` hoặc `Retail-metadata`.
   - Cấu hình một bộ tham số cố định (ví dụ: `MinUtil = 2000000`, `Interval = 300`, `Window = 500`, `Alpha = 0.05`, `Max Len = 3`, `Max Tx = 10000`, `Tốc độ trễ = 0 ms`).
   - Nhấn **▶ Phát luồng (RUN)** và chờ tiến trình hoàn tất 100%.
   - Nhấn vào **Xuất dữ liệu ▾** $\rightarrow$ chọn **"🌐 Xuất Báo cáo Toàn diện (HTML + Toàn bộ Biểu đồ) ⭐"**.
   - Đặt tên file là: `Bao_Cao_Thuc_Nghiem_BASELINE.html`.

2. **Chuyển sang nhánh có áp dụng Design Patterns**:
   - Chuyển git branch sang nhánh có mẫu thiết kế (khi bạn tiến hành bước tiếp theo).
   - Khởi chạy ứng dụng và nhập **đúng các tham số y hệt như bước 1** (cùng Dataset, cùng MinUtil, Interval, Window, Alpha, MaxLen, MaxTx).
   - Nhấn **▶ Phát luồng (RUN)** và chờ hoàn tất.
   - Xuất Báo cáo HTML thứ hai với tên: `Bao_Cao_Thuc_Nghiem_DESIGN_PATTERNS.html`.

3. **Tổng hợp kết luận cho báo cáo**:
   - Mở cả hai tệp HTML trên trình duyệt.
   - So sánh trực tiếp:
     - Biểu đồ số 4 (*Tiêu thụ RAM*): Nhánh Pattern sẽ thấp và phẳng hơn nhiều so với nhánh Baseline.
     - Biểu đồ số 5 (*Thông lượng*): Nhánh Pattern sẽ đạt mốc tx/s cao hơn.
     - Bảng số liệu: So sánh dòng *Bộ nhớ RAM Heap đỉnh điểm* và dòng *Thu gom rác (Garbage Collection)*.
   - Nhấn nút **"🖨 In báo cáo / Lưu PDF"** trên cả 2 trang HTML để đính kèm bản in PDF vào phụ lục đồ án!
