# BÁO CÁO TỔNG KẾT SO SÁNH HIỆU NĂNG HUDD-TDS

**Mô hình so sánh:** Baseline (Chưa áp dụng mẫu) vs. Design Patterns (Object Pool, Flyweight, Facade, Strategy, Pipeline)

**Ngày xuất báo cáo:** 2026-10-18 18:02:01

## 1. BẢNG TỔNG HỢP HIỆU NĂNG & TIẾT KIỆM TÀI NGUYÊN

| Dataset | Giao dịch | Thời gian Base | Thời gian Pattern | Tăng tốc (Speedup) | Đỉnh RAM Base | Đỉnh RAM Pattern | Tiết kiệm RAM | Thông lượng Base | Thông lượng Pattern | HUI & Drift |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Running Example (Mẫu)** | 4 | 0.13s | 0.16s | **0.83x (-20.6%)** | 4.0 MB | 2.9 MB | **28.3%** | 30 tx/s | 25 tx/s | 100% Khớp ✓ |
| **Chess-metadata** | 3,196 | 8.53s | 8.11s | **1.05x (5.0%)** | 184.3 MB | 125.2 MB | **32.0%** | 374 tx/s | 394 tx/s | 100% Khớp ✓ |
| **NewChess-Metadata** | 3,196 | 8.55s | 8.74s | **0.98x (-2.2%)** | 247.2 MB | 173.7 MB | **29.7%** | 373 tx/s | 365 tx/s | 100% Khớp ✓ |
| **mushrooms-metadata** | 8,416 | 5.17s | 2.74s | **1.88x (46.9%)** | 199.0 MB | 93.5 MB | **53.0%** | 1,627 tx/s | 3,068 tx/s | 100% Khớp ✓ |
| **NewMushroom-Metadata** | 8,416 | 5.18s | 2.68s | **1.93x (48.2%)** | 161.1 MB | 81.8 MB | **49.2%** | 1,625 tx/s | 3,137 tx/s | 100% Khớp ✓ |
| **connects-metadata** | 67,557 | 682.76s | 273.58s | **2.50x (59.9%)** | 313.3 MB | 405.5 MB | **-29.4%** | 98 tx/s | 246 tx/s | 100% Khớp ✓ |
| **NewConnects-Metadata** | 67,557 | 705.51s | 346.10s | **2.04x (50.9%)** | 387.4 MB | 407.2 MB | **-5.1%** | 95 tx/s | 195 tx/s | 100% Khớp ✓ |
| **Retail-metadata** | 88,162 | 176.90s | 170.70s | **1.04x (3.5%)** | 496.3 MB | 369.9 MB | **25.5%** | 498 tx/s | 516 tx/s | 100% Khớp ✓ |
| **Accidents-metadata** | 340,183 | 3156.22s | 1490.79s | **2.12x (52.8%)** | 242.5 MB | 111.0 MB | **54.2%** | 107 tx/s | 228 tx/s | 100% Khớp ✓ |
| **Chainstore-metadata** | 1,112,949 | 874.58s | 589.30s | **1.48x (32.6%)** | 480.0 MB | 920.4 MB | **-91.8%** | 1,272 tx/s | 1,888 tx/s | 100% Khớp ✓ |
| **TRUNG BÌNH TOÀN BỘ** | - | - | - | **1.58x** | - | - | **14.6%** | - | **+59.0%** | **100% Bảo toàn** |

## 2. KẾT LUẬN & ĐÁNH GIÁ CHUYÊN MÔN

1. **Tối ưu hóa Thời gian & Tốc độ:** Áp dụng Design Patterns giúp tăng tốc độ trung bình **1.58x**, giảm mạnh độ trễ xử lý stream nhờ Pipeline & Pool.
2. **Giảm thiểu Tiêu thụ Bộ nhớ (RAM Heap):** Tiết kiệm trung bình **14.6%** RAM Heap đỉnh điểm nhờ cơ chế tái sử dụng Transaction trong Object Pool và Item trong Flyweight Factory.
3. **Bảo toàn Tính Đúng đắn của Thuật toán (Algorithmic Fidelity):** 100% kết quả khai phá tập mục độ lợi cao (HUI) và phát hiện trôi dạt khái niệm (Global/Local Drift) khớp hoàn toàn 1:1 giữa hai phiên bản, chứng minh việc tái cấu trúc hoàn toàn an toàn và đáng tin cậy.
