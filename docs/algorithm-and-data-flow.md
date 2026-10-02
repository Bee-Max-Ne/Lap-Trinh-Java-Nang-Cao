# Thuật toán và luồng dữ liệu giao dịch

Tài liệu này mô tả cách triển khai trong `src/huddtds`, không thay thế đặc tả
hình thức của bài báo nghiên cứu.

## Dữ liệu giao dịch đầu vào

GUI lấy luồng giao dịch và bảng utility thông qua `DatasetService`.
`TransactionParser` hỗ trợ:

- Dòng SPMF/HUIM dạng utility: danh sách item, utility giao dịch (TU) và
  utility từng item, ngăn cách bằng dấu hai chấm.
- Dòng dạng quantity cũ: các token `item:quantity`, ngăn cách bằng khoảng
  trắng.

Với dữ liệu utility, utility từng item được đọc trực tiếp từ dòng giao dịch.
Với dữ liệu quantity, utility được tính từ quantity và external utility; nếu
không có external utility cho item thì model dùng mặc định `1.0`.

`DatasetService` và `DatasetManager` cung cấp chức năng tìm tập dữ liệu, kiểm
định, nạp investment, mở reader giao dịch và ước lượng số dòng. GUI cũng có
Running Example dựng sẵn và hỗ trợ tệp giao dịch tùy chỉnh.

## Trình tự xử lý

```text
tập dữ liệu / tệp / dữ liệu nhập tay
        │
        ▼
DatasetService: bảng investment + reader giao dịch
        │
        ▼
SimulationWorker đọc từng dòng không rỗng và gán TID kế tiếp
        │
        ▼
SimulationService.processLine(line, tid)
        ├── TransactionParser.parseLine(...)
        ├── HUDD_TDS.processTransaction(transaction)
        └── tại checkpoint:
              ├── HUIItemsetMiner.discover(memory, tid)
              ├── tính DIS_HS cho checkpoint
              ├── GlobalDriftStrategy / LocalDriftStrategy
              └── trả về/phát kết quả có kiểu
```

GUI kiểm tra giới hạn giao dịch trước khi tăng TID và số giao dịch đã xử lý,
nên giới hạn dương là số dòng tối đa được xử lý. `0` nghĩa là không đặt giới
hạn. Dòng trống không được gán TID.

## Bộ nhớ luồng, checkpoint và HUI

`HUDD_TDS.processTransaction()` thêm giao dịch vào bộ nhớ engine và giữ lại
xấp xỉ tối đa ba lần `windowSize` theo ranh giới TID để phục vụ so sánh. Lịch
sử được giữ lại này không phải cửa sổ dùng để khai phá.

Tại checkpoint có `TID % interval == 0`, `HUIDiscovery` tạo cửa sổ khai phá:

```text
(currentTid - windowSize, currentTid]
```

Đầu tiên, thuật toán tính TWU có suy giảm để lọc các item tiềm năng, sau đó
sinh candidate đệ quy. Candidate có cận trên utility của nhánh thấp hơn
`minutil` sẽ bị cắt tỉa. Các candidate còn lại được tính utility đầy đủ; giữ
lại dưới dạng HUI nếu tổng utility đạt ít nhất `minutil`.

Hàm suy giảm trong `UtilityMetrics`:

```text
d(Δt) = 2^(-Δt / 2)
```

`Max Len <= 0` tắt giới hạn độ sâu sinh candidate; giá trị dương giới hạn độ
dài itemset.

Global distance (`DIS_HS`) của checkpoint là tổng các giá trị distance-to-root
của HUI tìm được. Giá trị đó được dùng làm quan sát cho global drift và có thể
được hiển thị trên biểu đồ.

## Đánh giá drift

### Global drift

`GlobalDriftDetector` có trạng thái. Lớp này lưu chuỗi khoảng cách checkpoint,
duy trì cut point, tính trung bình hai nhóm cùng các cận Hoeffding, rồi so sánh
độ chênh với ngưỡng. Khi phát hiện drift, lớp trả `DriftResult` có hướng, thống
kê, ngưỡng và TID của checkpoint. `HUDD_TDS` truyền alpha cấu hình vào detector
mặc định; các profile GUI dùng giá trị khác nhau, chẳng hạn `0.05` hoặc `0.10`.
Trong code, alpha được hiểu là mức ý nghĩa, không phải phần trăm độ tin cậy.

Hàm ngưỡng trong `UtilityMetrics` thực hiện:

```text
epsilon = sqrt(((n - m) / (2 n m)) * ln(2 / alpha)) * abs(range)
```

Global detector mặc định dùng `range = 1.0`. Code so sánh trị tuyệt đối của
chênh lệch giữa trung bình trước cut point và trung bình toàn chuỗi với ngưỡng.
Kiểm tra chiều biến thiên có thể cập nhật cut point; khi phát hiện, chuỗi quan
sát hiện tại trở thành đoạn tham chiếu kế tiếp.

Không nên kết luận một profile GUI bất kỳ tương đương bài báo nếu chưa khớp
toàn bộ tham số và tiền xử lý dữ liệu.

### Local drift

`LocalDriftDetector` so sánh hợp các itemset xuất hiện ở checkpoint trước và
checkpoint hiện tại. Với mỗi itemset, lớp so sánh tổng utility; nếu itemset
không xuất hiện ở một checkpoint thì utility tại checkpoint đó được xem là
`0`. Alpha được hiệu chỉnh Bonferroni theo số giả thuyết candidate. Detector
tính ngưỡng cục bộ và trả về itemset đầu tiên vượt ngưỡng.

Ngưỡng cục bộ trong implementation dùng `n = n1 + n2`,
`m = 1/n1 + 1/n2`, phương sai `sigma²` và alpha đã hiệu chỉnh:

```text
L = ln(2 ln(n) / alphaAdjusted)
epsilon = sqrt(2 m sigma² L) + (2 m / 3) L
```

Detector mặc định nhận `windowSize` làm kích thước mẫu cho cả `n1` và `n2`.
Giá trị đem so sánh là utility của itemset tại hai checkpoint; code không tái
dựng các giá trị đó từ toàn bộ giao dịch trong hai mẫu.

## Các tham số runtime chính

| Tham số | Hành vi trong code |
|---|---|
| `minutil` | Ngưỡng cắt tỉa TWU và ngưỡng utility cuối để giữ HUI |
| `interval` | Tạo checkpoint tại TID chia hết cho tham số này |
| `windowSize` | Độ rộng cửa sổ khai phá kết thúc tại TID hiện tại |
| `alpha` | Mức ý nghĩa truyền vào kiểm định drift |
| `maxItemsetSize` / GUI `Max Len` | Giá trị dương giới hạn độ dài; không dương không giới hạn độ sâu |
| GUI `Max Tx` | Bằng 0 thì đọc hết; số dương giới hạn số giao dịch được xử lý |

Checkpoint dựa trên bội số TID; code không đợi cửa sổ đầy mới bắt đầu tạo
checkpoint. Cần lưu ý semantics này khi đối chiếu với bài báo hoặc implementation
bên ngoài.

Định dạng dữ liệu, cách kiểm định và preset GUI được mô tả trong
[README.md](../README.md) ở thư mục gốc.
