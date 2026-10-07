# Kết quả kiểm thử, benchmark, baseline và xác minh cuối

## Lần xác minh code gần nhất đã ghi nhận

Trong lượt đồng bộ bốn mẫu thiết kế, toàn bộ tệp Java hiện có trong `src` và
`test` được biên dịch bằng `javac` vào `out`. Các runner `main` độc lập sau trả
mã thoát 0:

| Runner | Phạm vi kiểm tra | Kết quả |
|---|---|---|
| `test.FinalValidationSuite` | Trường hợp biên số học, parser, nạp investment, kiểm định dữ liệu | ĐẠT |
| `test.StrategyInjectionTest` | Tiêm/ủy quyền Strategy và API drift cũ/có kiểu | ĐẠT |
| `test.SimulationServiceEventTest` | Event có kiểu, tiến độ, hoàn tất/lỗi, hủy listener | ĐẠT |
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
đều kết thúc với mã thoát 0. `test.DriftPairBenchmark` và
`test.FullBenchmarkSuite` không được chạy lại; không xem kết quả cũ là bằng
chứng chạy mới.

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

Lần chạy `test.FullBenchmarkSuite` đã hoàn thành 9 tập cấu hình: Chess,
NewChess, Mushrooms, NewMushroom, Connects, NewConnects, Retail, Accidents và
Chainstore.

Ở lần chạy hiện tại, số transaction, checkpoint, HUI cuối và drift toàn cục/cục
bộ khớp bảng lịch sử. Thời gian chạy và RAM biến thiên theo môi trường. Nhật ký
thô đã ghi ở
[`logs/benchmark_2026-10-02_10-12-52.log`](../logs/benchmark_2026-10-02_10-12-52.log).

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
- `FullBenchmarkSuite` chưa được chạy lại sau thay đổi giới hạn GUI gần nhất.
- Cấu hình Chainstore hiện tại không tái tạo thực nghiệm của bài báo.
- Source review hoặc biên dịch thành công không thay thế kiểm thử GUI runtime.

## Tổng kết xác minh cuối

| Hạng mục | Trạng thái |
|---|---|
| Biên dịch sạch sau thay đổi mã nguồn gần nhất | ĐẠT |
| FinalValidationSuite và các runner hồi quy chính | ĐẠT |
| Tiêm và ủy quyền Strategy | ĐẠT |
| Hành vi event của Observer | ĐẠT |
| Đối chiếu baseline đã ghi | KHỚP |
| Benchmark đầy đủ sau sửa giới hạn GUI | CHƯA CHẠY LẠI |
| Smoke test GUI đầy đủ | CHƯA KIỂM THỬ |
| Sẵn sàng xét riêng thuật toán/hồi quy | SẴN SÀNG |
| Sẵn sàng bao gồm xác minh GUI đầy đủ theo yêu cầu | CHƯA SẴN SÀNG |
