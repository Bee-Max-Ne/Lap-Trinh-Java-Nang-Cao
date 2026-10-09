# Thuật toán và luồng dữ liệu giao dịch

Tài liệu này mô tả cách triển khai trong `src/huddtds`, không thay thế đặc tả
hình thức của bài báo nghiên cứu.

## Dữ liệu giao dịch đầu vào

GUI lấy luồng giao dịch và tạo service mô phỏng thông qua `SimulationFacade`;
Facade dùng `DatasetService` cho các thao tác dataset.
`TransactionParser` hỗ trợ:

- Dòng SPMF/HUIM dạng utility: danh sách item, utility giao dịch (TU) và
  utility từng item, ngăn cách bằng dấu hai chấm.
- Dòng dạng quantity cũ: các token `item:quantity`, ngăn cách bằng khoảng
  trắng.

Với dữ liệu utility, utility từng item được đọc trực tiếp từ dòng giao dịch.
Với dữ liệu quantity, utility được tính từ quantity và external utility; nếu
không có external utility cho item thì model dùng mặc định `1.0`.

`SimulationFacade` cung cấp điểm truy cập cho GUI; `DatasetService` và
`DatasetManager` thực hiện chức năng tìm tập dữ liệu, kiểm định, nạp investment,
mở reader giao dịch và ước lượng số dòng. GUI cũng có Running Example dựng sẵn
và hỗ trợ tệp giao dịch tùy chỉnh.

## Trình tự xử lý

```text
tập dữ liệu / tệp / dữ liệu nhập tay
        │
        ▼
SimulationFacade / DatasetService: bảng investment + reader giao dịch
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
xấp xỉ tối đa ba lần `windowSize` theo ranh giới TID để phục vụ so sánh. Cụ thể,
giao dịch đầu danh sách bị loại khi `TID <= currentTid - 3 * windowSize`. Lịch
sử giao dịch được giữ lại này không phải cửa sổ dùng để khai phá.

Engine chỉ giữ 1.000 checkpoint gần nhất cùng các HUI tương ứng trong RAM;
`ChartPanel` giữ tối đa 1.000 checkpoint và GUI dọn các dòng bảng theo checkpoint
hết hạn. Bộ phát hiện global giữ số quan sát, tổng cộng dồn, tổng tại cut point,
reference sum và một observation gần nhất; trạng thái thống kê không cần lưu
toàn bộ vector distance.

Trong phiên GUI, `SimulationWorker` tạo
`logs/HUDD_TDS_Checkpoints_<thời_gian>.csv`. Listener ghi mỗi HUI thành một dòng
CSV; checkpoint không có HUI vẫn sinh một dòng `CHECKPOINT`. Bản ghi chứa TID,
DIS_HS, số HUI, kết quả/thống kê/ngưỡng/hướng global-local, itemset, utility,
Dmo và utility từng item. Writer UTF-8 escape dấu phẩy/ngoặc kép theo CSV và
dùng tên file mới, không ghi đè phiên cũ. Đây là writer GUI, không phải cơ chế
ghi của `FullBenchmarkSuite`.

Tại checkpoint có `TID % interval == 0`, `HUIDiscovery` tạo cửa sổ khai phá:

```text
(currentTid - windowSize, currentTid]
```

Đầu tiên, thuật toán tính TWU có suy giảm để lọc các item tiềm năng, sau đó
sinh candidate đệ quy theo thứ tự item đã sắp xếp. Mỗi candidate được xét bằng
một lượt duyệt cửa sổ: các giao dịch chứa candidate đóng góp vào cận trên
`UB_d` và utility đầy đủ `U_d`. Nếu `UB_d < minutil`, nhánh bị cắt; nếu cận
trên còn đạt nhưng `U_d < minutil`, không tạo HUI nhưng vẫn có thể tiếp tục mở
rộng candidate. Chỉ candidate có `U_d >= minutil` được tạo thành
`HighUtilityItemset` và nhận `D_mo`.

```text
UB_d(X) = Σ TU(T) * d(t - TID_T), với X ⊆ T
U_d(X)  = Σ [Σ u(i,T), i ∈ X] * d(t - TID_T), với X ⊆ T
```

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

`GlobalDriftDetector` có trạng thái. Lớp này duy trì số quan sát, tổng cộng dồn
và tổng tại cut point thay vì giữ chuỗi khoảng cách trong RAM. Hai trung bình
được cập nhật theo thứ tự cộng của từng nhóm; detector vẫn tính các cận
Hoeffding rồi so sánh độ chênh với ngưỡng. Khi phát hiện drift, lớp trả
`DriftResult` có hướng, thống kê, ngưỡng và TID của checkpoint.
`HUDD_TDS` truyền alpha cấu hình vào detector mặc định; các profile GUI dùng
giá trị khác nhau, chẳng hạn `0.05` hoặc `0.10`. Trong code, alpha được hiểu là
mức ý nghĩa, không phải phần trăm độ tin cậy.

Các công thức trong `UtilityMetrics` yêu cầu `0 < alpha < 1`.
`HUDD_TDS.Builder.withAlphaConfidence()` và `SimulationConfiguration.Builder`
đều từ chối giá trị ngoài khoảng này trước khi tạo engine.

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

### Ý nghĩa của hai giới hạn lịch sử

- `windowSize` giới hạn giao dịch đầu vào mà HUI miner khai phá tại checkpoint.
- `MAX_RETAINED_CHECKPOINTS = 1000` giới hạn số checkpoint/HUI engine giữ để
  tiếp tục so sánh và truy cập qua `getCheckpoints()`.
- CSV trong GUI mới là lịch sử checkpoint/HUI đầy đủ trên đĩa. Benchmark
  console hiện báo số đo của lượt chạy nhưng không ghi đầy đủ checkpoint vào
  `CheckpointHistoryWriter`.

## Các tham số runtime chính

| Tham số | Hành vi trong code |
|---|---|
| `minutil` | Ngưỡng cắt tỉa TWU và ngưỡng utility cuối để giữ HUI |
| `interval` | Tạo checkpoint tại TID chia hết cho tham số này |
| `windowSize` | Độ rộng cửa sổ khai phá kết thúc tại TID hiện tại |
| `alpha` | Mức ý nghĩa truyền vào kiểm định drift |
| `maxItemsetSize` / GUI `Max Len` | Giá trị dương giới hạn độ dài; không dương không giới hạn độ sâu |
| GUI `Max Tx` | Bằng 0 thì đọc hết; số dương giới hạn số giao dịch được xử lý |

GUI gom các tham số qua `SimulationConfiguration.Builder`; Facade chuyển chúng
và các Strategy tùy chọn vào `HUDD_TDS.Builder`. Hai Builder từ chối `minutil`
âm/không hữu hạn, `interval` hoặc `windowSize` không dương, alpha ngoài
`(0,1)` và `maxItemsetSize` âm. Giá trị `maxItemsetSize = 0` vẫn có nghĩa là
không giới hạn độ sâu. Các constructor tương thích trực tiếp của engine được
giữ riêng và không đi qua toàn bộ validation của Builder.

Checkpoint dựa trên bội số TID; code không đợi cửa sổ đầy mới bắt đầu tạo
checkpoint. Cần lưu ý semantics này khi đối chiếu với bài báo hoặc implementation
bên ngoài.

Định dạng dữ liệu, cách kiểm định và preset GUI được mô tả trong
[README.md](../README.md) ở thư mục gốc.
