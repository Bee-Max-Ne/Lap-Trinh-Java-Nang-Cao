# Bốn mẫu thiết kế trong HUDD-TDS

Mã nguồn hiện áp dụng trực tiếp bốn mẫu thiết kế: **Strategy** trong bộ điều
phối thuật toán, **Observer** trong dịch vụ mô phỏng, **Facade** tại ranh giới
giữa GUI với các dịch vụ ứng dụng, và **Builder** trong khởi tạo các đối tượng
phức tạp. Mỗi mẫu có một trách nhiệm riêng; không thêm pattern chỉ để trang trí
kiến trúc.

## Strategy

### Vì sao Strategy phù hợp?

Engine có ba lựa chọn thuật toán có thể thay đổi độc lập, trong khi vòng đời xử
lý luồng dữ liệu vẫn giữ nguyên:

1. Cách khai phá tập mục có độ lợi cao từ các giao dịch đang lưu.
2. Cách đánh giá drift toàn cục từ khoảng cách giữa các checkpoint.
3. Cách đánh giá drift cục bộ giữa hai checkpoint liền kề.

Phần ổn định là `HUDD_TDS`: nhận giao dịch, lưu lịch sử, tạo checkpoint theo
chu kỳ và điều phối phép tính. Các phần có thể thay đổi được biểu diễn bằng
contract/interface, nhờ đó chi tiết tạo candidate và kiểm định thống kê không
bị nhúng cứng vào phần điều phối.

### Các điểm có thể thay đổi và contract tương ứng

| Điểm thay đổi | Contract | Cài đặt mặc định | Phương thức contract |
|---|---|---|---|
| Khai phá HUI | `HUIItemsetMiner` | `HUIDiscovery` | `discover(memory, currentTid)` |
| Phát hiện drift toàn cục | `GlobalDriftStrategy` | `GlobalDriftDetector` | `updateAndCheck(observation, oldCheckpointTid, newCheckpointTid)` |
| Phát hiện drift cục bộ | `LocalDriftStrategy` | `LocalDriftDetector` | `detect(previousCheckpoint, currentCheckpoint)` |

Các contract cũng nhận trace listener để chuyển thông tin chẩn đoán mà không
khiến engine phụ thuộc vào logger hay giao diện cụ thể.

### Tiêm phụ thuộc và ủy quyền xử lý

Các constructor thông thường của `HUDD_TDS` tạo các cài đặt mặc định. Constructor
tiêm phụ thuộc nhận đủ ba interface:

```java
new HUDD_TDS(
    externalUtilities, minutil, interval, windowSize, alpha,
    huiMiner, globalDriftStrategy, localDriftStrategy
);
```

Khi đến checkpoint, `HUDD_TDS` ủy quyền việc khai phá HUI cho miner. Khi đã có
ít nhất hai checkpoint, các phương thức trả kết quả drift ủy quyền quan sát
checkpoint liền kề cho strategy toàn cục và cục bộ. `SimulationService` dùng
các API trả về `DriftResult` rồi đưa kết quả vào event.

### Thêm một biến thể như thế nào?

Muốn thay cách khai phá HUI, cài đặt `HUIItemsetMiner` rồi tiêm vào engine.
Muốn thay cách phát hiện drift toàn cục hoặc cục bộ, cài đặt interface tương
ứng rồi tiêm vào. Không cần viết lại việc lưu luồng và điều phối checkpoint.
Các constructor hiện hữu vẫn dùng những cài đặt mặc định đang có.

Đây là điểm mở rộng ở cấp thuật toán, không phải cơ chế plugin nạp động khi
chương trình đang chạy; implementation được truyền vào bằng mã Java lúc khởi
tạo.

### Tương thích

`HUDD_TDS.checkGlobalDrift()` và `checkLocalDrift()` vẫn trả chuỗi mô tả cũ hoặc
`null`. Các phương thức kiểu mới trả về `DriftResult`. Bộ kiểm thử kiểm tra
riêng hai luồng API này. Do các phương thức gọi strategy phát hiện drift có
trạng thái, caller nên chỉ gọi một dạng API cho mỗi checkpoint, không gọi cả
API chuỗi và API kiểu cho cùng một checkpoint.

### Kiểm chứng

`StrategyInjectionTest` truyền miner và strategy giả vào engine, kiểm tra việc
ủy quyền, kết quả có kiểu và chuỗi API cũ. Xem mã nguồn
[StrategyInjectionTest.java](../test/StrategyInjectionTest.java).

## Observer

### Các thành phần tham gia

| Vai trò | Kiểu trong dự án | Trách nhiệm |
|---|---|---|
| Publisher / Subject (đối tượng phát thông báo) | `SimulationService` | Lưu danh sách listener và phát cập nhật mô phỏng |
| Contract Observer (bên nhận thông báo) | `SimulationListener` | Nhận `onUpdate(SimulationEvent)` |
| Loại event | `EventType` | Định danh loại thông báo |
| Payload event | `SimulationEvent` | Mang TID và dữ liệu giao dịch, checkpoint, drift, tiến độ hoặc thông báo liên quan |
| Subscriber GUI hiện tại | Listener do `HUDD_TDS_GUI.SimulationWorker` đăng ký | Chuyển thông báo vào luồng cập nhật giao diện của SwingWorker |

`SimulationEvent` sao chép `itemsetVectors` thành danh sách không sửa được;
các tham chiếu đến model bên trong payload không được deep-copy.

Service quản lý đăng ký bằng `addListener()` / `removeListener()` và
`subscribe()`, lưu listener trong `CopyOnWriteArrayList`. `subscribe()` trả về
`Subscription` để hủy đăng ký idempotent theo vòng đời. Khi phát event, service
gọi listener đồng bộ trên luồng đang gọi service. Nếu listener ném
`RuntimeException`, service vẫn gọi listener còn lại rồi ném lỗi đầu tiên,
đính kèm các lỗi tiếp theo làm suppressed exceptions; lỗi không bị nuốt.

### Các event

`EventType` gồm:

- `CHECKPOINT_CREATED`
- `GLOBAL_DRIFT`
- `LOCAL_DRIFT`
- `TRANSACTION_PROCESSED`
- `SIMULATION_FINISHED`
- `SIMULATION_ERROR`

Event checkpoint/drift do `SimulationService` phát sau khi engine xử lý. Event
tiến độ, hoàn tất và lỗi được caller (hiện là GUI worker) phát qua các phương
thức của service. `SimulationEvent` là payload thông báo có kiểu, không phải
event của Swing.

### Observer giảm coupling với GUI như thế nào?

Nếu không có contract listener, application hoặc thuật toán phải gọi trực tiếp
widget GUI hoặc phụ thuộc vào callback gắn với lớp giao diện. Thay vào đó:

- `SimulationService` biết các contract listener/event, không biết
  `HUDD_TDS_GUI`.
- GUI đăng ký nhận thông báo rồi tự diễn giải để trình bày.
- Test có thể gắn listener thu thập event hoặc strategy giả mà không tạo widget
  Swing.
- Có thể thêm subscriber mà không đưa phụ thuộc giao diện vào publisher hoặc
  engine.

Observer giảm coupling tại ranh giới thông báo; GUI vẫn đọc các kiểu model và
kết quả dùng chung trong payload event.

### Phân biệt event và luồng xử lý

Observer không tự tạo thread. Callback listener chạy đồng bộ trên luồng phát
event. Trong GUI, listener gọi `SwingWorker.publish()`; `SwingWorker.process()`
cập nhật component trên Swing Event Dispatch Thread (EDT). `SwingWorker` đảm
nhiệm concurrency; Observer đảm nhiệm thông báo tách rời.

### Kiểm chứng

`SimulationServiceEventTest` kiểm tra payload và loại event, các thông báo
drift/tiến độ/hoàn tất/lỗi, việc gỡ listener, subscription idempotent và dispatch
tiếp khi một listener ném `RuntimeException`. Test này kiểm tra hành vi event
của application service, không bao phủ toàn bộ tương tác thread của Swing
runtime. Xem [SimulationServiceEventTest.java](../test/SimulationServiceEventTest.java).

## Facade

### Vì sao Facade phù hợp?

GUI cần phối hợp nhiều thao tác ứng dụng liên quan đến dataset và mô phỏng:
liệt kê/kiểm định dataset, nạp investment, tạo engine và `SimulationService`,
mở transaction stream, ước lượng số giao dịch. `SimulationFacade` cung cấp một
điểm truy cập đơn giản cho các thao tác này, để GUI không phải tự điều phối
`DatasetService`, `DatasetManager`, `InvestmentLoader` và `HUDD_TDS`.

Facade không thay thế các service bên dưới và cũng không chứa thuật toán mining
hay drift. Nó gọi `DatasetService` và khởi tạo `SimulationService`; engine bên
trong service tiếp tục sử dụng ba Strategy. `HUDD_TDS_GUI` là client trực tiếp
đã được nối với Facade. `DemoRunner` vẫn gọi engine trực tiếp cho luồng demo
dòng lệnh.

### Phạm vi hiện thực

`SimulationFacade` cung cấp các thao tác:

- `getAvailableDatasets()` và `validateDataset(...)`;
- `createSimulationService(...)`;
- `openTransactionStream(...)`;
- `estimateTransactionCount(...)`;
- `exportToCSV(...)`.

GUI gọi Facade cho discovery, validation, tạo service, mở stream, ước lượng
giao dịch và xuất CSV của bảng HUI/drift. `SimulationConfiguration` gom cấu hình
dataset, tham số thuật toán và các Strategy tùy chọn trước khi Facade tạo service.
Reader do Facade trả về vẫn thuộc trách nhiệm đóng của caller, thường bằng
try-with-resources. Các báo cáo TXT và thao tác sao chép log/lịch sử vẫn thuộc
tầng trình bày vì chúng gắn với trạng thái giao diện hoặc tệp phiên hiện tại.

### Kiểm chứng

`FacadePatternTest` kiểm tra discovery, validation, tạo service, xử lý một giao
dịch qua service do Facade tạo và nhận event checkpoint, cùng việc mở stream
Running Example. Test còn kiểm chứng cấu hình Strategy qua Facade, luồng
Observer cho drift và CSV UTF-8/escaping. Nó không kiểm thử tương tác Swing
runtime. Xem
[FacadePatternTest.java](../test/FacadePatternTest.java).

## Builder

### Vì sao Builder phù hợp?

Trong hệ thống `HUDD-TDS`, các đối tượng trung tâm như `HUDD_TDS` (Engine) và
`SimulationEvent` (Payload sự kiện) có cấu hình gồm nhiều thuộc tính bắt buộc và
tùy chọn (như `alpha`, các `Strategy` miner/drift, hoặc các trường sự kiện
`checkpoint`, `driftResult`, `progress`, `message`).

Trước khi có Builder Pattern:
- `HUDD_TDS` mắc lỗi *Telescoping Constructors* khi có quá nhiều constructor chồng
  chồng lên nhau, dễ gây nhầm lẫn vị trí tham số.
- `SimulationEvent` có constructor dài với các tham số nullable, dễ sai sót khi
  truyền chuỗi tham số kiểu dữ liệu giống nhau.

Mẫu **Builder Pattern** giúp:
1. Tách biệt quá trình xây dựng đối tượng phức tạp khỏi biểu diễn nội bộ.
2. Kiểm tra tính hợp lệ (validation) của tham số (ví dụ: `minutil >= 0`, `interval > 0`)
   trước khi khởi tạo đối tượng.
3. Cung cấp Fluent API dạng `.builder().withA(...).withB(...).build()` giúp mã nguồn rõ
   ràng, dễ đọc và tự giải thích.

### Phạm vi hiện thực

1. **`SimulationConfiguration.Builder`**:
   - Tạo cấu hình bất biến cho Facade, gom dataset, investment file, tham số
     khai phá và các Strategy tùy chọn.
   - Kiểm tra tên dataset, `minutil`, `interval`, `windowSize`, alpha và giới
     hạn itemset trước khi tạo service.
2. **`HUDD_TDS.Builder`**:
   - Có mặc định cho `externalUtilities`, `minutil`, `interval`, `windowSize`, alpha và giới hạn itemset.
   - Cung cấp `.withExternalUtilities()`, `.withMinutil()`, `.withInterval()`, `.withWindowSize()`, `.withAlphaConfidence()`, `.withMaxItemsetSize()` và các setter tiêm miner/drift strategy.
   - Các setter kiểm tra một số điều kiện đầu vào ngay khi được gọi; minutil phải hữu hạn/không âm, alpha phải trong `(0, 1)` và giới hạn itemset không âm.

3. **`SimulationEvent.Builder`**:
   - Có setter `with...` cho `type`, `tid`, `transaction`, `checkpoint`, ba trường drift, vector itemset, tiến độ/tốc độ và thông điệp.
   - `.build()` yêu cầu `type`, TID không âm, checkpoint payload cho checkpoint event, đúng drift result cho event global/local và metadata tiến độ không âm.
   - `itemsetVectors` được sao chép thành danh sách không sửa được; các tham chiếu model không được deep-copy.

### Kiểm chứng

`BuilderPatternTest` kiểm tra khởi tạo `HUDD_TDS` với cấu hình mặc định và tùy
chỉnh, xác nhận một số setter từ chối tham số ngoài phạm vi và xây dựng
`SimulationEvent` qua fluent API. `FacadePatternTest` kiểm tra cấu hình
`SimulationConfiguration` và việc chuyển các Strategy tới engine.
`SimulationEvent.Builder` xác thực các trường cốt yếu cho loại event; constructor
công khai nhận danh sách tham số vẫn giữ tương thích nhưng không áp dụng các
điều kiện validation của Builder. Các trường tùy chọn còn lại do caller lựa chọn.
Xem [BuilderPatternTest.java](../test/BuilderPatternTest.java) và tài liệu chi tiết
[Builder_Pattern_Explanation.md](./Builder_Pattern_Explanation.md).

## Kiểm chứng tổng hợp

| Mẫu | Kiểm thử chính | Phạm vi xác nhận |
|---|---|---|
| Strategy | `StrategyInjectionTest` | Tiêm đủ ba contract, xác nhận delegation và API drift legacy/typed |
| Observer | `SimulationServiceEventTest` | Loại/payload event, subscription lifecycle, dispatch tiếp khi listener lỗi |
| Facade | `FacadePatternTest` | Dataset, tạo service, event, Strategy injection và CSV export |
| Builder | `BuilderPatternTest`, `FacadePatternTest` | Cấu hình engine/event/mô phỏng, validate tham số và fluent API |

Các kiểm thử trên xác nhận hành vi ở mức Java service/engine. Chúng không thay
thế cho GUI smoke test đầy đủ hay benchmark trên toàn bộ dữ liệu.
