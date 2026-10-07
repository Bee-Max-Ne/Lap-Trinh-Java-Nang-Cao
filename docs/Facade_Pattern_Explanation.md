# Facade trong HUDD-TDS

## Mục đích

Facade là mẫu thiết kế cấu trúc, cung cấp một điểm truy cập đơn giản cho một
nhóm thao tác từ các thành phần con. Trong dự án này, `SimulationFacade` là
điểm truy cập ở tầng ứng dụng mà GUI dùng để phối hợp thao tác dataset và
khởi tạo mô phỏng.

Facade không làm cho hệ thống trở thành Clean Architecture hoàn chỉnh, không
thay thế các service con và không chứa thuật toán khai phá HUI hoặc phát hiện
drift.

## Luồng sử dụng hiện tại

```text
HUDD_TDS_GUI
    │
    ▼
SimulationFacade
    ├── DatasetService ──> DatasetManager / Validator / InvestmentLoader
    └── tạo SimulationService ──> TransactionParser ──> HUDD_TDS
                                                       ├── HUIItemsetMiner
                                                       ├── GlobalDriftStrategy
                                                       └── LocalDriftStrategy
```

GUI dùng Facade để:

- lấy danh sách dataset và kiểm định dataset;
- tạo `SimulationService` với tham số mô phỏng;
- mở transaction stream hoặc stream Running Example;
- ước lượng số giao dịch để hiển thị tiến độ.

`SimulationFacade.exportToCSV(...)` cũng có trong API Facade, nhưng GUI hiện
đang thực hiện các thao tác xuất tại chính tầng trình bày. Không nên xem chức
năng này là đã được tích hợp/kiểm thử chỉ vì phương thức tồn tại.

Facade trả về `BufferedReader`; caller sở hữu reader và phải đóng reader, thường
bằng try-with-resources. `SimulationFacade` hiện được GUI sử dụng; `DemoRunner`
vẫn gọi engine trực tiếp.

## Quan hệ với Strategy và Observer

Ba mẫu thiết kế có phạm vi riêng và nối tiếp nhau trong luồng mô phỏng:

1. **Facade** đơn giản hóa cách GUI truy cập dịch vụ ứng dụng.
2. **Strategy** cho phép `HUDD_TDS` ủy quyền HUI mining, global drift và local
   drift qua ba interface có thể tiêm phụ thuộc.
3. **Observer** cho phép `SimulationService` gửi event đến listener mà không
   phụ thuộc trực tiếp vào Swing.

Facade tạo `SimulationService`, nhưng không thay thế Observer. Service vẫn
phát event; GUI listener chuyển event qua `SwingWorker.publish()` để cập nhật
component trên EDT. Tương tự, Facade không thay thế Strategy: engine vẫn chọn
các implementation mặc định hoặc nhận implementation được tiêm qua constructor
của `HUDD_TDS`.

## Kiểm thử

`FacadePatternTest` kiểm tra discovery/validation, tạo service, mở stream và
xử lý giao dịch qua service do Facade tạo, đồng thời xác nhận event checkpoint
được phát. `StrategyInjectionTest` và `SimulationServiceEventTest` kiểm tra
riêng delegation Strategy và hành vi Observer. Các test này không thay thế
kiểm thử GUI runtime đầy đủ.

Xem thêm [design-patterns.md](./design-patterns.md),
[architecture.md](./architecture.md) và
[FacadePatternTest.java](../test/FacadePatternTest.java).
