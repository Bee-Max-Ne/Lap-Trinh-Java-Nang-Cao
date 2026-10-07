# Luồng sự kiện và GUI

## Sự kiện mô phỏng

`SimulationService` đảm nhiệm vai trò publisher trong API event của ứng dụng.
Listener đăng ký bằng `addListener()` và hủy đăng ký bằng `removeListener()`.
Khi phát, service duyệt danh sách listener và gọi
`SimulationListener.onUpdate(event)` đồng bộ trên luồng gọi service.

| Loại event | Thời điểm phát | Payload chính |
|---|---|---|
| `CHECKPOINT_CREATED` | Giao dịch tạo được checkpoint | TID, giao dịch, checkpoint, kết quả global/local và vector trình bày |
| `GLOBAL_DRIFT` | Global strategy phát hiện drift | Kết quả có kiểu, ngữ cảnh checkpoint và mô tả hiển thị |
| `LOCAL_DRIFT` | Local strategy phát hiện drift | Kết quả có kiểu, ngữ cảnh checkpoint và itemset bị ảnh hưởng |
| `TRANSACTION_PROCESSED` | Worker phát tiến độ | TID, tổng số ước lượng và tốc độ xử lý |
| `SIMULATION_FINISHED` | Worker đọc xong luồng bình thường | TID cuối |
| `SIMULATION_ERROR` | Worker báo lỗi xử lý | TID và nội dung lỗi |

Payload được mang trong `SimulationEvent`. Danh sách vector itemset được sao
chép thành danh sách không thể sửa đổi; event không chứa Swing component.

## Luồng chạy GUI

```text
EDT: người dùng nhấn RUN
  ├── đọc trường cấu hình và chụp nội dung Running Example
  ├── tạo SimulationWorker
  └── SwingWorker.execute()
          │
          ├── nền: mở/đọc luồng, parse, khai phá, phát hiện drift
          ├── nền: SimulationService phát event tới listener
          ├── listener: SwingWorker.publish(update)
          └── EDT: SwingWorker.process(updates)
                    ├── tiến độ và thẻ chỉ số
                    ├── bảng HUI/drift và báo cáo tổng kết
                    ├── nhật ký xử lý
                    └── ChartPanel
```

Vùng nhập Running Example được đọc khi xử lý RUN trên EDT và truyền cho worker
dưới dạng `String`. Tác vụ nền dùng snapshot này thay vì đọc Swing component.

## Các nút điều khiển vòng đời

- **RUN:** kiểm tra parse số, đặt lại phần trình bày, chụp dữ liệu nhập tay, tạo
  worker và chạy ở luồng nền.
- **PAUSE:** bật cờ tạm dừng; worker chờ giữa các dòng đầu vào.
- **RESUME:** tắt cờ và đánh thức worker qua pause lock.
- **STOP:** hủy SwingWorker và cập nhật trạng thái nút/tiến độ.
- **RESET:** xóa các bảng, biểu đồ, chỉ số, tổng kết và nhật ký trên giao diện.
- **Kết thúc bình thường/lỗi:** `done()` đọc kết quả worker rồi cập nhật trạng
  thái cuối cùng trên giao diện.

Tạm dừng, hủy, lọc, tương tác biểu đồ và xuất tệp là các hành vi riêng. Việc
nhìn thấy cửa sổ GUI hoặc biên dịch thành công không chứng minh mọi nhánh điều
khiển hoạt động.

## Các chức năng GUI thể hiện trong mã nguồn

- Chọn tập dữ liệu, tệp giao dịch tùy chỉnh hoặc dữ liệu Running Example.
- Bảng HUI/drift, thanh tiến độ, thẻ trạng thái, báo cáo tổng kết và nhật ký.
- Điều khiển tìm kiếm/lọc và chọn chỉ số biểu đồ.
- `ChartPanel` sắp xếp checkpoint theo TID số tăng dần, bố trí trục X theo
  khoảng TID thực và dùng biến đổi log1p có dấu cho trục Y; nhãn trục và
  tooltip thể hiện lại giá trị DISHS/HUI gốc.
- Điểm đỏ chỉ đại diện cho `DriftResult` đã phát hiện loại global ở checkpoint
  đó. Drift là kết quả so sánh thống kê giữa các checkpoint, không phải điều
  kiện `DISHS > một ngưỡng`; tooltip hiển thị statistic và threshold để phân
  biệt mức DISHS với quyết định drift.
- Menu xuất HUI CSV, Drift CSV, báo cáo tổng kết TXT và nhật ký xử lý TXT.
- Tùy chọn trace từng giao dịch và thanh trượt độ trễ.

Đây là các chức năng đã được xác nhận qua source; không có nghĩa tất cả tương
tác hoặc định dạng xuất đã được kiểm thử thủ công.

`ChartPanelTest` kiểm tra sắp xếp TID bằng số, thứ tự tọa độ X, tính chất nén
giá trị của log1p, phép đổi ngược cho nhãn trục và điều kiện chỉ đánh dấu drift
toàn cục. Đây là kiểm thử logic component, không thay thế smoke test desktop.

## Giới hạn xác minh GUI

Smoke test đã khởi chạy `java -cp out huddtds.demo.HUDD_TDS_GUI`, quan sát cửa
sổ phản hồi, tiến độ, nhật ký, chỉ số và biểu đồ trong một lần chạy Running
Example. Trong lần thử giới hạn giao dịch, `Max Tx = 20` cho báo cáo 21 giao
dịch và làm lộ lỗi lệch một đơn vị. Kiểm tra giới hạn đã được sửa; biên dịch
sạch và các kiểm thử hồi quy sau đó đều thành công.

Chưa hoàn tất đáng tin cậy chuỗi tương tác RUN, PAUSE, RESUME, STOP, RESET,
tìm kiếm/lọc và xuất tệp sau khi sửa. Vì vậy trạng thái smoke test GUI tổng thể
là **CHƯA KIỂM THỬ ĐẦY ĐỦ**; không kết luận GUI ĐẠT.
