# Tài liệu HUDD-TDS

Thư mục này mô tả cách triển khai hiện tại và các bằng chứng đã thu thập khi
biên dịch, kiểm thử hồi quy, đo hiệu năng và xác minh GUI. Nội dung bám theo mã
nguồn trong kho dự án, không phải bản thiết kế kiến trúc mục tiêu.

## Danh mục tài liệu

| Chủ đề | Tài liệu |
|---|---|
| Kiến trúc hệ thống và quan hệ phụ thuộc giữa các gói | [architecture.md](./architecture.md) |
| Bốn mẫu Strategy, Observer, Facade và Builder | [design-patterns.md](./design-patterns.md) |
| Kịch bản thuyết trình chi tiết: sơ đồ, vị trí, luồng chạy, kết quả và câu hỏi bảo vệ | [design-patterns-presentation.md](./design-patterns-presentation.md) |
| Giải thích riêng về Facade | [Facade_Pattern_Explanation.md](./Facade_Pattern_Explanation.md) |
| Giải thích riêng về Builder | [Builder_Pattern_Explanation.md](./Builder_Pattern_Explanation.md) |
| Thuật toán và luồng dữ liệu giao dịch | [algorithm-and-data-flow.md](./algorithm-and-data-flow.md) |
| Luồng sự kiện và GUI | [event-and-gui-flow.md](./event-and-gui-flow.md) |
| Kết quả kiểm thử, benchmark, baseline và xác minh cuối | [validation-and-baseline.md](./validation-and-baseline.md) |
| Hồ sơ baseline chi tiết | [baseline/](./baseline/) |

## Tình trạng xác minh

- Lần biên dịch `javac` gần nhất trên toàn bộ source/test cùng các kiểm thử
  Strategy, Observer, Facade, Builder và regression chọn lọc đều thành công; xem
  [validation-and-baseline.md](./validation-and-baseline.md).
- `StrategyInjectionTest`, `SimulationServiceEventTest`, `FacadePatternTest` và `BuilderPatternTest`
  lần lượt kiểm tra bốn mẫu thiết kế Strategy, Observer, Facade và Builder ở mức engine/application.
  Các kiểm tra đó không thay thế GUI smoke test đầy đủ.
- Kết quả Running Example và các kết quả Chess được chọn khớp với lần đối chiếu
  phiên bản lịch sử đã ghi nhận.
- GUI đã được khởi chạy; tiến độ, nhật ký, chỉ số và biểu đồ được quan sát trong
  một lần smoke test một phần. Chưa xác minh đầy đủ RUN/PAUSE/RESUME/STOP/RESET,
  tìm kiếm/lọc và xuất tệp; không được xem GUI là đã kiểm thử hoàn chỉnh.
- Benchmark nhiều tập dữ liệu hiện tại chạy theo cấu hình 9 tập của dự án,
  không phải thực nghiệm Chainstore đầy đủ trong bài báo.

[README.md](../README.md) ở thư mục gốc tiếp tục là hướng dẫn cài đặt và sử dụng.
Các tài liệu trong thư mục này trình bày riêng kiến trúc, thuật toán, mẫu thiết
kế, luồng xử lý và kết quả xác minh.
