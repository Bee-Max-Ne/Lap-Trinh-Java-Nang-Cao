# Kiến trúc hệ thống

## Phạm vi và cấu trúc kiến trúc

Kho mã là một ứng dụng Java được tổ chức thành các gói, không phải tập hợp các
module có thể triển khai độc lập. Chiều phụ thuộc quan sát được trong mã nguồn:

```text
demo / trình bày
        │
        ▼
dịch vụ ứng dụng và API sự kiện
        │
        ▼
algorithm ───── data ───── math
        \         |         /
         └────────┴────────┘
                   ▼
                 model
```

Mũi tên biểu thị việc sử dụng lớp hoặc interface trong lúc biên dịch.
`DemoRunner` là điểm khởi chạy dòng lệnh và trực tiếp tạo engine thuật toán;
GUI Swing sử dụng các dịch vụ ứng dụng để truy cập tập dữ liệu và chạy mô phỏng.

## Sơ đồ kiến trúc hệ thống

Sơ đồ dưới đây tóm tắt các tầng và quan hệ phụ thuộc chính trong source. Các
strategy là những điểm được tiêm vào `HUDD_TDS`; `HUIDiscovery` cùng hai
detector là các implementation mặc định. Các gói `data`, `math` và
`algorithm` đều dùng các kiểu thuộc `model`.

```mermaid
flowchart TD
    subgraph Presentation["Tầng trình bày"]
        GUI["HUDD_TDS_GUI"]
        CLI["DemoRunner"]
        CHART["ChartPanel"]
        GUI --> CHART
    end

    subgraph Application["Tầng ứng dụng"]
        DS["DatasetService"]
        SS["SimulationService"]
        EVENTS["SimulationEvent / SimulationListener / EventType"]
        DS --> SS
        SS --> EVENTS
    end

    subgraph Algorithm["Tầng thuật toán"]
        ENGINE["HUDD_TDS"]
        MINER["HUIItemsetMiner"]
        GLOBAL["GlobalDriftStrategy"]
        LOCAL["LocalDriftStrategy"]
        HUI_IMPL["HUIDiscovery"]
        GLOBAL_IMPL["GlobalDriftDetector"]
        LOCAL_IMPL["LocalDriftDetector"]
        ENGINE --> MINER
        ENGINE --> GLOBAL
        ENGINE --> LOCAL
        HUI_IMPL -. "cài đặt mặc định" .-> MINER
        GLOBAL_IMPL -. "cài đặt mặc định" .-> GLOBAL
        LOCAL_IMPL -. "cài đặt mặc định" .-> LOCAL
    end

    subgraph DataMath["Tầng dữ liệu và toán học"]
        DATA["TransactionParser / DatasetManager / DatasetValidator / InvestmentLoader"]
        MATH["UtilityMetrics"]
    end

    subgraph Domain["Tầng mô hình miền"]
        MODEL["Transaction / Element / Checkpoint / HighUtilityItemset / DriftResult / ItemsetVector"]
    end

    GUI --> DS
    GUI --> SS
    CLI --> ENGINE
    SS --> DATA
    SS --> ENGINE
    HUI_IMPL --> MATH
    GLOBAL_IMPL --> MATH
    LOCAL_IMPL --> MATH
    DATA --> MODEL
    MATH --> MODEL
    ENGINE --> MODEL
```

## Trách nhiệm của các gói

| Gói | Trách nhiệm chính | Một số kiểu tiêu biểu |
|---|---|---|
| `huddtds.demo` | Giao diện Swing và chương trình minh họa dòng lệnh | `HUDD_TDS_GUI`, `ChartPanel`, `DemoRunner` |
| `huddtds.application` | Ranh giới ứng dụng cho thao tác tập dữ liệu và mô phỏng luồng | `DatasetService`, `SimulationService` |
| `huddtds.application.event` | Thông báo mô phỏng có kiểu dữ liệu rõ ràng | `EventType`, `SimulationEvent`, `SimulationListener` |
| `huddtds.algorithm` | Điều phối luồng/checkpoint và các cài đặt thuật toán mặc định | `HUDD_TDS`, `HUIDiscovery`, `GlobalDriftDetector`, `LocalDriftDetector` |
| `huddtds.algorithm.mining` | Hợp đồng chiến lược khai phá HUI | `HUIItemsetMiner` |
| `huddtds.algorithm.drift` | Hợp đồng chiến lược phát hiện drift | `GlobalDriftStrategy`, `LocalDriftStrategy` |
| `huddtds.data` | Phân tích cú pháp, tìm tập dữ liệu, nạp investment và kiểm định | `TransactionParser`, `DatasetManager`, `DatasetValidator`, `InvestmentLoader` |
| `huddtds.math` | Suy giảm utility, thống kê cắt tỉa/phát hiện drift, khoảng cách và vector | `UtilityMetrics` |
| `huddtds.model` | Đối tượng dữ liệu và kết quả dùng chung giữa các tầng | `Transaction`, `Checkpoint`, `HighUtilityItemset`, `DriftResult` |

## Các quan hệ phụ thuộc chính

```text
HUDD_TDS_GUI
  ├── DatasetService
  ├── SimulationService
  ├── SimulationEvent / SimulationListener
  ├── các kiểu model dùng để hiển thị
  └── ChartPanel

DatasetService ──> DatasetManager / DatasetValidator / InvestmentLoader
SimulationService
  ├──> TransactionParser
  ├──> HUDD_TDS
  ├──> UtilityMetrics
  └──> các kiểu model

HUDD_TDS
  ├──> HUIItemsetMiner       (mặc định: HUIDiscovery)
  ├──> GlobalDriftStrategy   (mặc định: GlobalDriftDetector)
  └──> LocalDriftStrategy    (mặc định: LocalDriftDetector)

algorithm / data / math ──> model
```

GUI dùng `SimulationService` làm ranh giới ứng dụng để xử lý giao dịch và phát
sự kiện. Service gọi parser và engine bên trong; GUI không cần tự tạo các lớp
cài đặt thuật toán/dữ liệu cho từng giao dịch. Đây không phải service chạy ở
tiến trình riêng hay hệ thống nạp plugin.

## Các ràng buộc kiến trúc thể hiện trong mã nguồn

- `HUDD_TDS` điều phối việc lưu giao dịch, tạo checkpoint và gọi các strategy.
  Chi tiết khai phá và tính toán drift có thể thay thế qua interface chiến lược.
- GUI sử dụng `DatasetService` cho việc tìm và truy cập tệp, nhưng tầng dữ liệu
  vẫn dùng trực tiếp các lớp cài đặt cụ thể; chưa có repository interface hay
  factory tổng quát cho nhiều nguồn dữ liệu.
- API sự kiện không phụ thuộc Swing. `SwingWorker` và GUI chịu trách nhiệm chạy
  nền cũng như cập nhật component.
- Các đối tượng model được dùng chung giữa nhiều tầng. Java module chưa được
  dùng để áp đặt ranh giới giữa các gói.
- Các phương thức drift dạng chuỗi cũ vẫn được giữ để tương thích; application
  service sử dụng các phương thức trả về `DriftResult`.

Chi tiết hành vi xem [algorithm-and-data-flow.md](./algorithm-and-data-flow.md)
và [event-and-gui-flow.md](./event-and-gui-flow.md).
