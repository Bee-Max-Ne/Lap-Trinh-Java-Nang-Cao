# Kết quả kiểm thử, benchmark, baseline và xác minh cuối

## Lần xác minh code gần nhất đã ghi nhận

Sau khi sửa lỗi lệch một đơn vị ở giới hạn `Max Tx` của GUI, toàn bộ 36 tệp
Java trong `src` và `test` đã được biên dịch bằng `javac -encoding UTF-8` vào
`out`. Các runner `main` độc lập sau trả mã thoát 0:

| Runner | Phạm vi kiểm tra | Kết quả |
|---|---|---|
| `test.FinalValidationSuite` | Trường hợp biên số học, parser, nạp investment, kiểm định dữ liệu | ĐẠT |
| `test.StrategyInjectionTest` | Tiêm/ủy quyền Strategy và API drift cũ/có kiểu | ĐẠT |
| `test.SimulationServiceEventTest` | Event có kiểu, tiến độ, hoàn tất/lỗi, hủy listener | ĐẠT |
| `test.DatasetServiceTest` | Ranh giới ứng dụng cho tìm/đọc/kiểm định tập dữ liệu | ĐẠT |
| `test.DataLayerTest` | Parser, investment, tìm tập dữ liệu và kiểm định | ĐẠT |
| `test.BaselineRunner` | Running Example dựng sẵn gồm bốn giao dịch | ĐẠT |
| `test.EndToEndChessRunner` | Chạy end-to-end trên tập Chess | ĐẠT |

`test.DriftPairBenchmark` và `test.FullBenchmarkSuite` đã ĐẠT trong lần xác
minh trước đó; không chạy lại sau thay đổi chỉ ảnh hưởng giới hạn GUI. Hồ sơ
build/test mới nhất ở [baseline/test-result.txt](./baseline/test-result.txt);
thông tin build ở [baseline/build-result.txt](./baseline/build-result.txt).

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
