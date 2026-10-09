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
| Mô hình tối ưu theo phase, công thức, quy trình và trạng thái | [Mục lộ trình tối ưu trong README](../README.md) |
| Hồ sơ baseline chi tiết | [baseline/](./baseline/) |

## Tình trạng xác minh

- Các kết quả build/test là hồ sơ lịch sử theo lần chạy cụ thể, không phải xác
  nhận tự động cho mọi thay đổi mới. Trong lần rà soát tài liệu ngày 2026-10-09,
  source đã được đọc đối chiếu nhưng không chạy lại `javac` hay runner; hãy chạy
  lệnh tương ứng trước khi xác nhận trạng thái hiện tại. Xem
  [validation-and-baseline.md](./validation-and-baseline.md).
- `StrategyInjectionTest`, `SimulationServiceEventTest`, `FacadePatternTest` và
  `BuilderPatternTest` lần lượt kiểm tra bốn mẫu thiết kế Strategy, Observer,
  Facade và Builder ở mức engine/application; xem từng tài liệu pattern để biết
  chính xác phạm vi assertion.
  Các kiểm tra đó không thay thế GUI smoke test đầy đủ.
- `CheckpointRetentionTest`, `GlobalDriftDetectorStateTest` và
  `CheckpointHistoryWriterTest` kiểm tra giới hạn lịch sử engine, tính tương đương
  trạng thái Global Drift với oracle và cấu trúc CSV ở mức writer.
- Các runner kiểm thử là chương trình Java độc lập có `main`, không phải JUnit.
  Chạy lại từ source hiện tại để xác nhận thay vì coi ghi chú lịch sử là kết quả
  mới của mỗi lần thay đổi.
- Kết quả Running Example và các kết quả Chess được chọn khớp với lần đối chiếu
  phiên bản lịch sử đã ghi nhận.
- GUI đã được khởi chạy; tiến độ, nhật ký, chỉ số và biểu đồ được quan sát trong
  một lần smoke test một phần. Chưa xác minh đầy đủ RUN/PAUSE/RESUME/STOP/RESET,
  tìm kiếm/lọc và xuất tệp; không được xem GUI là đã kiểm thử hoàn chỉnh.
- Benchmark nhiều tập dữ liệu hiện tại chạy theo cấu hình 9 tập của dự án,
  không phải thực nghiệm Chainstore đầy đủ trong bài báo.
- `logs/` bị Git bỏ qua; log benchmark/process được sinh trên máy chạy và không
  tự trở thành hồ sơ version control.

[README.md](../README.md) ở thư mục gốc tiếp tục là hướng dẫn cài đặt và sử dụng.
Các tài liệu trong thư mục này trình bày riêng kiến trúc, thuật toán, mẫu thiết
kế, luồng xử lý và kết quả xác minh.
