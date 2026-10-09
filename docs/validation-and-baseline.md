# Kết quả kiểm thử, benchmark, baseline và xác minh cuối

## Trạng thái của hồ sơ

Các số liệu trong tài liệu này được ghi nhận ở những lần chạy lịch sử. Lần rà
soát tài liệu ngày 2026-10-09 chỉ đối chiếu source và tài liệu, không chạy lại
`javac`, test runner, benchmark hay GUI. Vì vậy không coi các bảng lịch sử là
kết quả kiểm thử của source hiện tại. Log runtime trong `logs/` bị Git ignore;
các log benchmark được dẫn từ lịch sử không còn trong checkout này. Bảng tóm
tắt versioned trong `docs/baseline/` là hồ sơ còn truy cập được.

## Đo hiệu năng và tối ưu khai phá HUI — 2026-10-08

Đã biên dịch sạch bằng JDK 17 và chạy `test.FullBenchmarkSuite` trước/sau tối
ưu với cùng cấu hình JVM (`-Xms256m -Xmx2g`) và JFR. JFR baseline xác nhận
`HUIDiscovery.generateCandidates()` và các lượt kiểm tra candidate trên từng
giao dịch chiếm phần lớn mẫu CPU.

`HUIDiscovery` được điều chỉnh để kiểm tra sự hiện diện của candidate một lần
trên mỗi giao dịch, đồng thời tính cận trên và utility trong cùng lượt quét.
Chỉ tạo `HighUtilityItemset` và tập item lưu kết quả khi candidate thực sự đạt
ngưỡng. Công thức utility, decay, ngưỡng, thứ tự duyệt và điều kiện cắt tỉa
không đổi.

| Dataset | Giao dịch | Trước (ms) | Sau (ms) | Thời gian giảm |
|---|---:|---:|---:|---:|
| Chess | 3.196 | 12.460 | 4.859 | 61,0% |
| NewChess | 3.196 | 9.478 | 3.908 | 58,8% |
| Mushrooms | 8.416 | 5.686 | 2.624 | 53,8% |
| NewMushroom | 8.416 | 9.144 | 2.743 | 70,0% |
| Connects | 10.000 | 118.115 | 98.973 | 16,2% |
| NewConnects | 10.000 | 110.067 | 78.436 | 28,7% |
| Retail | 10.000 | 24.812 | 17.584 | 29,1% |
| Accidents | 10.000 | 214.830 | 134.502 | 37,4% |
| Chainstore | 20.000 | 32.545 | 13.331 | 59,0% |
| **Tổng thời gian benchmark** | **76.424** | **537.137** | **356.960** | **33,5%** |

Mỗi lần chạy hoàn thành đủ 9 dataset. Số giao dịch, checkpoint, HUI cuối và
số lần phát hiện global/local drift khớp nhau. Nhật ký từng checkpoint và kết
quả thuật toán được so sánh; không có khác biệt trong lần chạy lịch sử đó. Log
thô bị Git ignore và không còn trong checkout; số liệu được ghi trong bảng này
và [baseline/benchmark-small.txt](./baseline/benchmark-small.txt).

Sau lần so sánh thời gian trên, engine/GUI được cập nhật tiếp để chỉ giữ tối đa
1.000 checkpoint gần nhất trong RAM; detector global chuyển sang tổng cộng dồn
hằng bộ nhớ. GUI ghi đầy đủ mọi checkpoint/HUI vào CSV trong `logs/`, giới hạn
hàng đợi event đang chờ xử lý trên EDT và giới hạn lịch sử hiển thị của chart/
bảng. `CheckpointRetentionTest`, `GlobalDriftDetectorStateTest` và
`CheckpointHistoryWriterTest` xác nhận các hành vi này; test CSV chạy trực tiếp
writer, chưa thay thế smoke test trên GUI desktop.

Benchmark cuối sau các thay đổi quản lý lịch sử hoàn tất cả 9 dataset. Chi tiết
checkpoint, số HUI và drift khớp với lần benchmark ngay trước đó. Thời gian lần
cuối có một ngoại lệ lớn ở Accidents (1.757.922 ms); do đó không dùng lần cuối
này để khẳng định tốc độ điển hình hoặc kết luận code bị chậm đi. Cần lặp lại
đo trong môi trường ổn định để có so sánh runtime đáng tin cậy. Log thô lần
cuối không còn trong checkout; số liệu lịch sử được ghi trong bảng này.

`test.FullBenchmarkSuite` hiện ghi đầu mỗi log một nhật ký theo các phase:
đóng băng baseline, profiling, tối ưu khai phá HUI, giới hạn lịch sử RAM,
benchmark lặp lại và kiểm thử GUI. Nhật ký nêu công thức decay, TWU suy giảm,
cận trên/utility candidate, ngưỡng global drift, quy trình xử lý, trạng thái
đã xác nhận/chưa xác nhận và giới hạn của số đo RAM. Khi chạy suite, log cũng
ghi kết quả hoàn tất theo từng dataset và flush sau checkpoint để giữ tiến độ
nếu tiến trình bị dừng bất thường. File benchmark được ghi bằng UTF-8.

Có thể tạo riêng log mô tả các phase mà không chạy benchmark dài bằng lệnh
`java -cp out test.FullBenchmarkSuite --phases-only`. Log này ghi rõ đây là
chế độ chỉ lập lộ trình, không chạy benchmark và không tạo số đo mới. Bỏ tham
số `--phases-only` để chạy benchmark đầy đủ trên 9 dataset.

GC log của lần đo cuối ghi nhận 25 lần collection và heap dùng trước GC tối đa
164 MB. So với baseline 1.377 lần và 313 MB, điều này phù hợp với mức áp lực
cấp phát thấp hơn; đây vẫn là heap tại các lần GC, không phải RSS hay phép đo
heap đỉnh tuyệt đối. Không nhầm với mức RAM của tiến trình hoặc coi đây là bảo
đảm cho mọi dataset/cấu hình.

Các runner hồi quy sau tối ưu đã trả mã thoát 0: `DataLayerTest`,
`FinalValidationSuite`, `StrategyInjectionTest`, `SimulationServiceEventTest`,
`DatasetServiceTest`, `FacadePatternTest`, `BuilderPatternTest`,
`huddtds.demo.ChartPanelTest`, `GlobalDriftDetectorStateTest`,
`CheckpointRetentionTest`, `CheckpointHistoryWriterTest`, `BaselineRunner`,
`EndToEndChessRunner` và `DriftPairBenchmark`. `FullBenchmarkSuite` cũng hoàn
tất thành công.

## Kết quả xác minh trước lần đo hiệu năng 2026-10-08

Trong lượt đồng bộ bốn mẫu thiết kế, toàn bộ tệp Java hiện có trong `src` và
`test` được biên dịch bằng `javac` vào `out`. Các runner `main` độc lập sau trả
mã thoát 0:

| Runner | Phạm vi kiểm tra | Kết quả |
|---|---|---|
| `test.FinalValidationSuite` | Trường hợp biên số học, parser, nạp investment, kiểm định dữ liệu | ĐẠT |
| `test.StrategyInjectionTest` | Tiêm/ủy quyền Strategy và API drift cũ/có kiểu | ĐẠT |
| `test.SimulationServiceEventTest` | Event có kiểu, tiến độ, hoàn tất/lỗi và hủy listener (phạm vi runner ở lần chạy được ghi nhận) | ĐẠT |
| `test.FacadePatternTest` | Discovery/validation, tạo service, xử lý giao dịch, event checkpoint và mở stream qua Facade | ĐẠT |
| `test.BuilderPatternTest` | Khởi tạo Engine và Event qua Builder, validate tham số biên và Fluent API | ĐẠT |
| `test.DatasetServiceTest` | Ranh giới ứng dụng cho tìm/đọc/kiểm định tập dữ liệu | ĐẠT |
| `test.DataLayerTest` | Parser, investment, tìm tập dữ liệu và kiểm định | ĐẠT |
| `test.BaselineRunner` | Running Example dựng sẵn gồm bốn giao dịch | ĐẠT |
| `test.EndToEndChessRunner` | Chạy end-to-end trên tập Chess | ĐẠT |

`StrategyInjectionTest`, `SimulationServiceEventTest`, `FacadePatternTest`, `BuilderPatternTest`,
`DatasetServiceTest`, `DataLayerTest` và `FinalValidationSuite` đã được chạy
trong cùng lượt xác minh sau khi biên dịch. `FacadePatternTest` kiểm tra luồng
tích hợp từ service do Facade tạo đến event checkpoint; nó không kiểm thử GUI.

`BaselineRunner` và `EndToEndChessRunner` cũng đã được chạy trong lượt này và
đều kết thúc với mã thoát 0. Các ghi chú trong mục này là ảnh chụp của lần xác
minh trước, không phải trạng thái mới nhất; trạng thái mới nhất được ghi ở mục
đo hiệu năng phía trên.

`test.DriftPairBenchmark` và `test.FullBenchmarkSuite` đã ĐẠT trong lần xác
minh trước đó; không chạy lại sau thay đổi chỉ ảnh hưởng giới hạn GUI. Hồ sơ
build/test mới nhất ở [baseline/test-result.txt](./baseline/test-result.txt);
thông tin build ở [baseline/build-result.txt](./baseline/build-result.txt).

## Sửa và kiểm tra hiển thị biểu đồ

Sau thay đổi `ChartPanel`, toàn bộ Java trong `src` và `test` được biên dịch
lại thành công. Các runner sau đã chạy lại với chế độ AWT headless và trả mã
thoát 0:

| Runner | Phạm vi | Kết quả |
|---|---|---|
| `huddtds.demo.ChartPanelTest` | TID tăng theo số, tọa độ X, log1p, nhãn đổi ngược, chỉ đánh dấu global drift và render component | ĐẠT |
| `test.StrategyInjectionTest` | Tiêm/ủy quyền Strategy | ĐẠT |
| `test.SimulationServiceEventTest` | Event/Observer | ĐẠT |
| `test.FacadePatternTest` | Facade đến checkpoint event | ĐẠT |
| `test.FinalValidationSuite` | Kiểm thử parser/toán học/dữ liệu | ĐẠT |

Đây là xác minh component bằng mã và render headless, không phải GUI desktop
smoke test. Thuật toán phát hiện drift không bị thay đổi. Điểm đỏ tiếp tục phản
ánh `DriftResult` global của engine; DISHS thấp không đồng nghĩa không có drift,
vì drift biểu thị thay đổi thống kê giữa các checkpoint chứ không phải DISHS
vượt một ngưỡng tuyệt đối.

Các lớp test là chương trình Java có `main`, không phải JUnit. Mã thoát thành
công xác nhận runner kết thúc mà không gặp assertion hoặc lỗi không được xử lý;
điều đó không đồng nghĩa GUI đã được kiểm thử toàn diện.

## Đối chiếu baseline

Revision gốc dùng để đối chiếu được ghi nhận là `7cddf2f`.

- `BaselineRunner`: TID checkpoint, các HUI/kích thước HUI, utility, D_mo,
  DIS_HS và kết quả drift được chọn khớp với revision gốc đã biên dịch riêng.
- `EndToEndChessRunner`: 119 dòng kết quả checkpoint/HUI/drift được chọn khớp
  với lần đối chiếu lịch sử.
- Các số liệu chức năng của benchmark tổng hợp (số giao dịch, checkpoint, HUI
  cuối, drift toàn cục/cục bộ) khớp bảng lịch sử trên cả 9 tập dữ liệu.

Sửa giới hạn `Max Tx` không làm thay đổi kết quả thuật toán khi chạy toàn bộ
tập dữ liệu. Lỗi giới hạn chạy có kiểm soát là nguyên nhân của thay đổi; chưa
ghi nhận một lần chạy GUI sau sửa để xác nhận trực tiếp giới hạn chính xác.
Thời gian chạy và RAM phụ thuộc máy, không được xem là ngưỡng baseline ổn định.

Xem [baseline/running-example.txt](./baseline/running-example.txt) và
[baseline/benchmark-small.txt](./baseline/benchmark-small.txt) để biết các lần
đối chiếu và kết quả đã ghi.

## Benchmark đầy đủ

Lần chạy được ghi nhận ngày 2026-10-02 của `test.FullBenchmarkSuite` đã hoàn
thành 9 tập cấu hình: Chess,
NewChess, Mushrooms, NewMushroom, Connects, NewConnects, Retail, Accidents và
Chainstore.

Trong lần chạy lịch sử đó, số transaction, checkpoint, HUI cuối và drift toàn
cục/cục bộ khớp bảng lịch sử. Thời gian chạy và RAM biến thiên theo môi trường.
Nhật ký thô không còn trong checkout; bảng benchmark còn lưu tại
[baseline/benchmark-small.txt](./baseline/benchmark-small.txt).

Benchmark Chainstore dùng cấu hình giới hạn 20.000 giao dịch của dự án, **không
phải** thực nghiệm Chainstore 1.112.949 giao dịch hay thí nghiệm cửa sổ 40.000/
50.000 trong bài báo. Bảng benchmark và so sánh lịch sử ở
[baseline/benchmark-small.txt](./baseline/benchmark-small.txt).

## Xác minh GUI và desktop

| Kiểm tra | Bằng chứng | Trạng thái |
|---|---|---|
| Tiến trình GUI chạy và cửa sổ xuất hiện | Khởi chạy bằng lệnh Java đã hướng dẫn | Đã quan sát |
| Cửa sổ phản hồi | Tiến trình/cửa sổ báo phản hồi khi quan sát | Đã quan sát |
| Tiến độ, nhật ký, chỉ số, biểu đồ | Hiển thị trong một lần chạy Running Example | Quan sát một phần |
| `Max Tx` có giới hạn | Giới hạn 20 cho kết quả 21; đã sửa code | Phát hiện và sửa lỗi |
| Xác nhận trực tiếp `Max Tx` sau sửa | Chưa chạy lại GUI để xác minh chính xác | CHƯA KIỂM THỬ |
| Đầy đủ RUN/PAUSE/RESUME/STOP/RESET | Chưa thao tác đáng tin cậy từ đầu đến cuối | CHƯA KIỂM THỬ |
| Tìm kiếm/lọc và xuất tệp | Chưa thao tác kiểm chứng từ đầu đến cuối | CHƯA KIỂM THỬ |

Do đó smoke test GUI tổng thể vẫn là **CHƯA KIỂM THỬ ĐẦY ĐỦ**. Các quan sát
trực quan ở trên chỉ là bằng chứng một phần, không phải kết luận GUI ĐẠT.

## Hạn chế và phần cần xác minh tiếp

- Chạy lại GUI với giới hạn giao dịch sau sửa, xác nhận cấu hình `N` xử lý đúng
  `N` giao dịch.
- Dùng luồng đủ dài để thử PAUSE, RESUME, STOP và RESET, xác nhận từng chuyển
  trạng thái.
- Kiểm tra tìm kiếm/lọc, chọn/chú giải biểu đồ và từng định dạng xuất; xác nhận
  cả tệp tạo ra lẫn nội dung.
- Tại thời điểm ghi chú lịch sử này, `FullBenchmarkSuite` chưa được chạy lại
  sau thay đổi giới hạn GUI gần nhất. Bộ benchmark đã được chạy lại ngày
  2026-10-08; xem mục đo hiệu năng phía trên.
- Cấu hình Chainstore hiện tại không tái tạo thực nghiệm của bài báo.
- Source review hoặc biên dịch thành công không thay thế kiểm thử GUI runtime.

## Tổng kết xác minh cuối

| Hạng mục | Trạng thái |
|---|---|
| Biên dịch sạch sau tối ưu HUI (JDK 17) | ĐẠT |
| 14 runner hồi quy sau tối ưu | ĐẠT |
| FullBenchmarkSuite trước/sau tối ưu | ĐẠT |
| Kết quả checkpoint/HUI/drift trước và sau | KHỚP |
| Tổng thời gian benchmark trong lần so sánh đầu | Giảm 33,5%; lần đo sau có ngoại lệ thời gian lớn |
| Heap dùng cực đại ghi nhận trước GC | Giảm từ 313 MB xuống 164 MB trong các lần đo này |
| Lịch sử engine, chart và bảng GUI trong RAM | Giới hạn 1.000 checkpoint gần nhất |
| Kết quả checkpoint/HUI đầy đủ | Ghi CSV liên tục trong `logs/` |
| RAM detector global | Trạng thái tổng hợp hằng bộ nhớ |
| Dung lượng đĩa của lịch sử CSV | Tăng theo thời lượng chạy; người dùng quản lý/xóa file |
| Smoke test GUI đầy đủ | CHƯA KIỂM THỬ |
| Xác minh GUI đầy đủ theo yêu cầu | CHƯA SẴN SÀNG |
