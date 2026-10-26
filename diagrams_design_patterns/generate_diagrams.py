import os
import subprocess
import time
from PIL import Image

output_dir = r"F:\Lap-Trinh-Java-Nang-Cao\diagrams_design_patterns"
os.makedirs(output_dir, exist_ok=True)

# Edge path
edge_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

# We will define the 8 HTML contents
diagrams = [
    {
        "filename": "01_facade_pattern.png",
        "title": "MẪU THIẾT KẾ #01: FACADE PATTERN",
        "subtitle": "Kiến trúc Bao Đóng Hệ Thống Con & Đơn Điểm Truy Cập (Single Entry Point)",
        "layer": "TẦNG 2: APPLICATION LAYER",
        "badge": "Giảm 100% Khớp Nối (Loose Coupling)",
        "content_html": """
        <div class="h-full flex flex-col justify-between">
          <!-- Top Client -->
          <div class="bg-blue-50 border-2 border-blue-400 rounded-xl p-3 flex items-center justify-between shadow-sm">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-lg bg-blue-600 flex items-center justify-center text-white font-bold text-lg shadow">🖥</div>
              <div>
                <div class="text-xs font-bold uppercase text-blue-700 tracking-wider">Tầng 1: Presentation Layer (Client)</div>
                <div class="text-sm font-bold text-slate-800 font-mono">HUDD_TDS_GUI & ChartPanel</div>
              </div>
            </div>
            <span class="text-xs font-mono font-semibold bg-blue-100 text-blue-800 px-2.5 py-1 rounded-md border border-blue-300">Giao diện người dùng</span>
          </div>

          <!-- Arrow Down -->
          <div class="flex flex-col items-center justify-center -my-1 text-indigo-600 font-bold text-xs">
            <div class="flex items-center gap-1.5 bg-indigo-50 border border-indigo-200 px-3 py-1 rounded-full shadow-xs">
              <span>↓</span> Giao tiếp độc quyền qua 1 cổng API thống nhất <span>↓</span>
            </div>
          </div>

          <!-- Central Facade -->
          <div class="bg-gradient-to-r from-purple-50 via-indigo-50 to-blue-50 border-2 border-purple-500 rounded-xl p-3.5 shadow-md">
            <div class="flex items-center justify-between border-b border-purple-200 pb-2 mb-2">
              <div class="flex items-center gap-2">
                <span class="w-3 h-3 rounded-full bg-purple-600 animate-pulse"></span>
                <span class="font-mono text-base font-bold text-purple-900">SimulationFacade (Cửa ngõ Hệ thống)</span>
              </div>
              <span class="text-xs bg-purple-200 text-purple-900 font-bold px-2 py-0.5 rounded">Design Pattern: Facade</span>
            </div>
            <div class="grid grid-cols-4 gap-2 text-xs font-mono">
              <div class="bg-white p-2 rounded border border-purple-200 text-purple-950 font-semibold text-center shadow-xs">
                startSimulation()
              </div>
              <div class="bg-white p-2 rounded border border-purple-200 text-purple-950 font-semibold text-center shadow-xs">
                stopSimulation()
              </div>
              <div class="bg-white p-2 rounded border border-purple-200 text-purple-950 font-semibold text-center shadow-xs">
                resetSimulation()
              </div>
              <div class="bg-white p-2 rounded border border-purple-200 text-purple-950 font-semibold text-center shadow-xs">
                exportResults()
              </div>
            </div>
          </div>

          <!-- Subsystem Boundary -->
          <div class="border-2 border-dashed border-slate-300 rounded-xl p-3 bg-slate-50/80">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                <span class="w-2 h-2 rounded-full bg-slate-400"></span>
                Bao đóng ranh giới hệ thống con (Encapsulated Subsystems)
              </span>
              <span class="text-[11px] text-slate-400 italic">GUI hoàn toàn không biết chi tiết các lớp bên dưới</span>
            </div>
            <div class="grid grid-cols-4 gap-2 text-xs font-mono">
              <div class="bg-white p-2.5 rounded-lg border border-slate-200 shadow-xs flex flex-col justify-between">
                <span class="font-bold text-slate-800">DatasetService</span>
                <span class="text-[10px] text-slate-500 font-sans mt-1">Đọc & cung cấp luồng giao dịch stream</span>
              </div>
              <div class="bg-white p-2.5 rounded-lg border border-slate-200 shadow-xs flex flex-col justify-between">
                <span class="font-bold text-slate-800">DatasetValidator</span>
                <span class="text-[10px] text-slate-500 font-sans mt-1">Kiểm định ma trận trọng số & tính hợp lệ</span>
              </div>
              <div class="bg-white p-2.5 rounded-lg border border-slate-200 shadow-xs flex flex-col justify-between">
                <span class="font-bold text-slate-800">TransactionParser</span>
                <span class="text-[10px] text-slate-500 font-sans mt-1">Phân tích cú pháp chuỗi thành Object</span>
              </div>
              <div class="bg-white p-2.5 rounded-lg border border-purple-200 shadow-xs flex flex-col justify-between bg-purple-50/40">
                <span class="font-bold text-purple-900">HUDD_TDS Engine</span>
                <span class="text-[10px] text-purple-700 font-sans mt-1">Lõi tính toán khai phá & kiểm định trôi dạt</span>
              </div>
            </div>
          </div>
        </div>
        """
    },
    {
        "filename": "02_builder_pattern.png",
        "title": "MẪU THIẾT KẾ #02: BUILDER PATTERN",
        "subtitle": "Khởi Tạo Tham Số Dạng Fluent & Rào Chắn Kiểm Định Fail-Fast",
        "layer": "TẦNG 2 & 3: APPLICATION & ALGORITHM LAYER",
        "badge": "Loại Bỏ Telescoping Constructor 10 Tham Số",
        "content_html": """
        <div class="h-full flex flex-col justify-between">
          <!-- Step Chain Header -->
          <div class="text-xs font-bold text-slate-600 uppercase tracking-wider mb-1 flex items-center justify-between">
            <span>Chuỗi phương thức cấu hình liên tiếp (Fluent Method Chaining)</span>
            <span class="text-indigo-600 font-mono font-semibold">SimulationConfiguration.builder()</span>
          </div>

          <!-- Horizontal Fluent Steps -->
          <div class="grid grid-cols-5 gap-2 text-xs font-mono">
            <div class="bg-indigo-50 border-2 border-indigo-200 rounded-lg p-2.5 shadow-xs flex flex-col justify-between">
              <span class="text-[10px] uppercase font-sans font-bold text-indigo-600">Bước 1</span>
              <span class="font-bold text-indigo-950 mt-1">.builder()</span>
              <span class="text-[10px] text-slate-500 font-sans mt-0.5">Khởi tạo đối tượng Builder tạm</span>
            </div>
            <div class="bg-indigo-50 border-2 border-indigo-200 rounded-lg p-2.5 shadow-xs flex flex-col justify-between">
              <span class="text-[10px] uppercase font-sans font-bold text-indigo-600">Bước 2</span>
              <span class="font-bold text-indigo-950 mt-1">.windowSize(1000)</span>
              <span class="text-[10px] text-slate-500 font-sans mt-0.5">Kích thước cửa sổ trượt</span>
            </div>
            <div class="bg-indigo-50 border-2 border-indigo-200 rounded-lg p-2.5 shadow-xs flex flex-col justify-between">
              <span class="text-[10px] uppercase font-sans font-bold text-indigo-600">Bước 3</span>
              <span class="font-bold text-indigo-950 mt-1">.threshold(0.02)</span>
              <span class="text-[10px] text-slate-500 font-sans mt-0.5">Ngưỡng tiện ích tối thiểu</span>
            </div>
            <div class="bg-indigo-50 border-2 border-indigo-200 rounded-lg p-2.5 shadow-xs flex flex-col justify-between">
              <span class="text-[10px] uppercase font-sans font-bold text-indigo-600">Bước 4</span>
              <span class="font-bold text-indigo-950 mt-1">.decayRate(0.95)</span>
              <span class="text-[10px] text-slate-500 font-sans mt-0.5">Hệ số suy giảm tiện ích</span>
            </div>
            <div class="bg-indigo-50 border-2 border-indigo-200 rounded-lg p-2.5 shadow-xs flex flex-col justify-between">
              <span class="text-[10px] uppercase font-sans font-bold text-indigo-600">Bước 5</span>
              <span class="font-bold text-indigo-950 mt-1">.batchInterval(100)</span>
              <span class="text-[10px] text-slate-500 font-sans mt-0.5">Tần suất kiểm tra trôi dạt</span>
            </div>
          </div>

          <!-- Arrow to Validation Gate -->
          <div class="flex items-center justify-center my-0.5 text-slate-500 font-bold text-xs">
            <span>↓ Gọi phương thức kết thúc: </span><code class="ml-1 px-2 py-0.5 bg-amber-100 text-amber-900 rounded font-mono font-bold">.build()</code>
          </div>

          <!-- Two Paths: Gatekeeper Validation -->
          <div class="grid grid-cols-12 gap-3 items-stretch">
            <!-- Left: Validation Rules -->
            <div class="col-span-6 bg-amber-50/70 border-2 border-amber-300 rounded-xl p-3 shadow-xs">
              <div class="flex items-center gap-2 text-xs font-bold text-amber-900 uppercase tracking-wide mb-2">
                <span class="w-2.5 h-2.5 rounded-full bg-amber-500"></span>
                Rào Chắn Kiểm Định Fail-Fast (Validation Gate)
              </div>
              <ul class="text-xs space-y-1.5 font-mono text-slate-700">
                <li class="flex items-center gap-1.5">
                  <span class="text-emerald-600 font-bold">✓</span>
                  <span>windowSize &gt; 0</span>
                  <span class="text-[10px] font-sans text-slate-500">(Cửa sổ phải hợp lệ)</span>
                </li>
                <li class="flex items-center gap-1.5">
                  <span class="text-emerald-600 font-bold">✓</span>
                  <span>0 &lt; minUtilityRatio &lt; 1</span>
                  <span class="text-[10px] font-sans text-slate-500">(Ngưỡng tiện ích)</span>
                </li>
                <li class="flex items-center gap-1.5">
                  <span class="text-emerald-600 font-bold">✓</span>
                  <span>0 &lt; decayRate &lt;= 1.0</span>
                  <span class="text-[10px] font-sans text-slate-500">(Hệ số suy giảm)</span>
                </li>
              </ul>
              <div class="mt-2 text-[11px] text-red-600 font-sans font-semibold bg-red-100/60 px-2 py-1 rounded border border-red-200">
                ⚠ Nếu vi phạm ➔ Ném ngay IllegalArgumentException
              </div>
            </div>

            <!-- Right: Immutable Output -->
            <div class="col-span-6 bg-emerald-50/70 border-2 border-emerald-400 rounded-xl p-3 shadow-xs flex flex-col justify-between">
              <div class="flex items-center gap-2 text-xs font-bold text-emerald-900 uppercase tracking-wide">
                <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                Đối Tượng Bất Biến (Immutable Configuration)
              </div>
              <p class="text-xs text-slate-600 font-sans leading-relaxed">
                Sau khi vượt qua kiểm định, trả về một thể hiện <b>bất biến hoàn toàn</b> (chỉ có getter, không có setter).
              </p>
              <div class="bg-white border border-emerald-300 rounded p-2 text-xs font-mono text-emerald-950 font-bold text-center">
                Thread-Safe: Không thể bị sửa đổi trong suốt vòng đời stream
              </div>
            </div>
          </div>
        </div>
        """
    },
    {
        "filename": "03_observer_pattern.png",
        "title": "MẪU THIẾT KẾ #03: OBSERVER PATTERN",
        "subtitle": "Mô Hình Sự Kiện Đa Luồng Bất Đồng Bộ (Asynchronous Event-Driven)",
        "layer": "TẦNG 1 & 2: PRESENTATION & APPLICATION LAYER",
        "badge": "100% Non-blocking UI • Tách Rời Tuyệt Đối",
        "content_html": """
        <div class="h-full flex flex-col justify-between">
          <!-- Top Subject/Publisher -->
          <div class="bg-gradient-to-r from-blue-50 to-indigo-50 border-2 border-blue-500 rounded-xl p-3 shadow-xs">
            <div class="flex items-center justify-between border-b border-blue-200 pb-1.5 mb-2">
              <div class="flex items-center gap-2">
                <span class="w-3 h-3 rounded-full bg-blue-600"></span>
                <span class="font-mono text-sm font-bold text-blue-900">Publisher: SimulationService</span>
                <span class="text-xs bg-blue-200 text-blue-900 font-bold px-2 py-0.5 rounded font-sans">Luồng Nền (Worker Thread)</span>
              </div>
              <span class="text-xs font-mono text-slate-500">Subject / Event Source</span>
            </div>
            <div class="flex items-center justify-between text-xs font-mono text-slate-700">
              <span>Đang tính toán luồng Stream: Batch #k...</span>
              <span class="bg-white px-2.5 py-1 rounded border border-blue-300 font-bold text-blue-700">
                notifyListeners(SimulationEvent)
              </span>
            </div>
          </div>

          <!-- Intermediate Interface -->
          <div class="flex items-center justify-center -my-1">
            <div class="bg-slate-100 border border-slate-300 rounded-lg px-4 py-1 text-xs font-mono font-bold text-slate-700 flex items-center gap-2 shadow-xs">
              <span class="text-indigo-600">&lt;&lt;interface&gt;&gt;</span>
              <span>SimulationListener (onProgressUpdate, onDriftDetected, onCompleted)</span>
            </div>
          </div>

          <!-- Two Subscribers Grid -->
          <div class="grid grid-cols-12 gap-3 items-stretch">
            
            <!-- Subscriber 1: UI on EDT -->
            <div class="col-span-7 bg-emerald-50/80 border-2 border-emerald-400 rounded-xl p-3 shadow-xs flex flex-col justify-between">
              <div class="flex items-center justify-between border-b border-emerald-200 pb-1.5 mb-1.5">
                <span class="text-xs font-bold uppercase text-emerald-900 tracking-wide flex items-center gap-1.5">
                  <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
                  Observer 1: Giao Diện Người Dùng (GUI)
                </span>
                <span class="text-[10px] font-mono bg-emerald-200 text-emerald-900 px-2 py-0.5 rounded font-bold">Event Dispatch Thread (EDT)</span>
              </div>
              <div class="space-y-1 text-xs">
                <div class="font-mono font-bold text-slate-800">• HUDD_TDS_GUI & ChartPanel</div>
                <div class="text-slate-600">Đón nhận sự kiện thông qua cơ chế <code class="font-mono text-indigo-700 bg-indigo-50 px-1 rounded">SwingWorker</code>, cập nhật thanh tiến trình và biểu đồ trôi dạt.</div>
              </div>
              <div class="mt-2 bg-white border border-emerald-300 rounded p-1.5 text-[11px] font-semibold text-emerald-800 text-center">
                ✓ Non-blocking: Giao diện giữ 60 FPS, không bị đóng băng khi tính toán lớn
              </div>
            </div>

            <!-- Subscriber 2: Checkpoint Writer on I/O -->
            <div class="col-span-5 bg-purple-50/80 border-2 border-purple-400 rounded-xl p-3 shadow-xs flex flex-col justify-between">
              <div class="flex items-center justify-between border-b border-purple-200 pb-1.5 mb-1.5">
                <span class="text-xs font-bold uppercase text-purple-900 tracking-wide flex items-center gap-1.5">
                  <span class="w-2 h-2 rounded-full bg-purple-500"></span>
                  Observer 2: Lưu Trữ Log
                </span>
                <span class="text-[10px] font-mono bg-purple-200 text-purple-900 px-2 py-0.5 rounded font-bold">I/O Thread</span>
              </div>
              <div class="space-y-1 text-xs">
                <div class="font-mono font-bold text-slate-800">• CheckpointHistoryWriter</div>
                <div class="text-slate-600">Ghi log các mốc cảnh báo trôi dạt định kỳ ra tệp CSV bất đồng bộ.</div>
              </div>
              <div class="mt-2 bg-white border border-purple-300 rounded p-1.5 text-[11px] font-semibold text-purple-800 text-center">
                ✓ Phân tách ghi đĩa, không cản trở luồng stream
              </div>
            </div>

          </div>
        </div>
        """
    },
    {
        "filename": "04_strategy_pattern.png",
        "title": "MẪU THIẾT KẾ #04: STRATEGY PATTERN",
        "subtitle": "Hoán Đổi Linh Hoạt Thuật Toán Khai Phá & Bộ Kiểm Định Trôi Dạt",
        "layer": "TẦNG 3: ALGORITHM LAYER",
        "badge": "Tuân Thủ Tuyệt Đối Open/Closed Principle (OCP)",
        "content_html": """
        <div class="h-full flex flex-col justify-between">
          <!-- Context Box -->
          <div class="bg-gradient-to-r from-slate-900 to-indigo-950 text-white rounded-xl p-3 shadow-md border-2 border-indigo-400">
            <div class="flex items-center justify-between border-b border-slate-700 pb-1.5 mb-2">
              <div class="flex items-center gap-2">
                <span class="w-2.5 h-2.5 rounded-full bg-indigo-400 animate-pulse"></span>
                <span class="font-mono font-bold text-sm text-indigo-200">Context: HUDD_TDS Engine</span>
              </div>
              <span class="text-xs bg-indigo-900 text-indigo-200 font-bold px-2 py-0.5 rounded font-mono">Lõi Điều Phối Thuật Toán</span>
            </div>
            <div class="grid grid-cols-3 gap-2 text-xs font-mono text-slate-300">
              <div class="bg-slate-800/80 p-2 rounded border border-slate-700">
                - minerStrategy: <span class="text-indigo-300 font-bold">HUIItemsetMiner</span>
              </div>
              <div class="bg-slate-800/80 p-2 rounded border border-slate-700">
                - globalStrategy: <span class="text-indigo-300 font-bold">GlobalDriftStrategy</span>
              </div>
              <div class="bg-slate-800/80 p-2 rounded border border-slate-700">
                - localStrategy: <span class="text-indigo-300 font-bold">LocalDriftStrategy</span>
              </div>
            </div>
          </div>

          <!-- Arrows down -->
          <div class="flex items-center justify-center -my-1 text-slate-400 font-bold text-xs gap-12">
            <span>↓ Implements</span>
            <span>↓ Implements</span>
            <span>↓ Implements</span>
          </div>

          <!-- 3 Interchangeable Strategies Columns -->
          <div class="grid grid-cols-3 gap-3">
            
            <!-- Strategy 1 -->
            <div class="bg-blue-50 border-2 border-blue-300 rounded-xl p-3 shadow-xs flex flex-col justify-between">
              <div>
                <div class="text-[11px] font-mono font-bold text-blue-800 bg-blue-100 px-2 py-0.5 rounded border border-blue-200 text-center mb-2">
                  &lt;&lt;interface&gt;&gt; HUIItemsetMiner
                </div>
                <div class="bg-white border border-blue-200 rounded p-2 text-xs font-mono font-bold text-blue-950 shadow-2xs">
                  HUIDiscovery
                </div>
                <p class="text-[11px] text-slate-600 mt-2 leading-relaxed">
                  Thuật toán khai phá tập mục tiện ích cao dựa trên danh sách tiện ích (Utility List) tối ưu.
                </p>
              </div>
              <div class="text-[10px] text-blue-700 font-semibold mt-2 pt-1.5 border-t border-blue-200">
                Có thể hoán đổi: EFIM-Stream, TKU
              </div>
            </div>

            <!-- Strategy 2 -->
            <div class="bg-purple-50 border-2 border-purple-300 rounded-xl p-3 shadow-xs flex flex-col justify-between">
              <div>
                <div class="text-[11px] font-mono font-bold text-purple-800 bg-purple-100 px-2 py-0.5 rounded border border-purple-200 text-center mb-2">
                  &lt;&lt;interface&gt;&gt; GlobalDriftStrategy
                </div>
                <div class="bg-white border border-purple-200 rounded p-2 text-xs font-mono font-bold text-purple-950 shadow-2xs">
                  GlobalDriftDetector
                </div>
                <p class="text-[11px] text-slate-600 mt-2 leading-relaxed">
                  Kiểm định trôi dạt tổng thể toàn cục dựa trên <b>bất đẳng thức chặn Hoeffding</b> với độ tin cậy nghiêm ngặt.
                </p>
              </div>
              <div class="text-[10px] text-purple-700 font-semibold mt-2 pt-1.5 border-t border-purple-200">
                Có thể hoán đổi: DDM, EDDM, ADWIN
              </div>
            </div>

            <!-- Strategy 3 -->
            <div class="bg-emerald-50 border-2 border-emerald-300 rounded-xl p-3 shadow-xs flex flex-col justify-between">
              <div>
                <div class="text-[11px] font-mono font-bold text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded border border-emerald-200 text-center mb-2">
                  &lt;&lt;interface&gt;&gt; LocalDriftStrategy
                </div>
                <div class="bg-white border border-emerald-200 rounded p-2 text-xs font-mono font-bold text-emerald-950 shadow-2xs">
                  LocalDriftDetector
                </div>
                <p class="text-[11px] text-slate-600 mt-2 leading-relaxed">
                  Kiểm định trôi dạt cục bộ trên từng Itemset với <b>hiệu chỉnh Bonferroni</b> chống báo động sai.
                </p>
              </div>
              <div class="text-[10px] text-emerald-700 font-semibold mt-2 pt-1.5 border-t border-emerald-200">
                Có thể hoán đổi: FDR, Benjamini-Hochberg
              </div>
            </div>

          </div>
        </div>
        """
    },
    {
        "filename": "05_flyweight_pattern.png",
        "title": "MẪU THIẾT KẾ #05: FLYWEIGHT PATTERN",
        "subtitle": "Sơ Đồ Phân Bổ Bộ Nhớ JVM Heap Trước Và Sau Khi Tối Ưu",
        "layer": "TẦNG 5: DOMAIN MODEL & OPTIMIZATION LAYER",
        "badge": "Tiết Kiệm 60% – 75% RAM Thực Nghiệm",
        "content_html": """
        <div class="h-full flex flex-col justify-between">
          <!-- BEFORE CARD -->
          <div class="bg-red-50/70 border-2 border-red-300 rounded-xl p-3 shadow-xs">
            <div class="flex items-center justify-between border-b border-red-200 pb-1.5 mb-2">
              <span class="text-xs font-bold text-red-900 uppercase tracking-wide flex items-center gap-1.5">
                <span class="w-2.5 h-2.5 rounded-full bg-red-600"></span>
                ❌ Khi CHƯA có Flyweight (Nhân bản đối tượng lặp lại)
              </span>
              <span class="text-xs font-mono font-bold text-red-700 bg-red-100 px-2 py-0.5 rounded border border-red-300">Heap: ~350 MB RAM</span>
            </div>
            <div class="grid grid-cols-4 gap-2 text-center text-xs font-mono">
              <div class="bg-white border border-red-300 rounded-lg p-2 shadow-2xs">
                <div class="font-bold text-slate-800">Element #1</div>
                <div class="text-[11px] text-red-700 mt-1">Item [ID:10, Milk, $2.5]</div>
                <div class="text-[10px] text-slate-500 font-sans">qty: 2</div>
              </div>
              <div class="bg-white border border-red-300 rounded-lg p-2 shadow-2xs">
                <div class="font-bold text-slate-800">Element #2</div>
                <div class="text-[11px] text-red-700 mt-1">Item [ID:10, Milk, $2.5]</div>
                <div class="text-[10px] text-slate-500 font-sans">qty: 5</div>
              </div>
              <div class="bg-white border border-red-300 rounded-lg p-2 shadow-2xs flex flex-col justify-center">
                <div class="font-bold text-slate-400">. . .</div>
                <div class="text-[10px] text-slate-400 font-sans">Hàng triệu Node</div>
              </div>
              <div class="bg-white border border-red-300 rounded-lg p-2 shadow-2xs">
                <div class="font-bold text-slate-800">Element #1.000.000</div>
                <div class="text-[11px] text-red-700 mt-1">Item [ID:10, Milk, $2.5]</div>
                <div class="text-[10px] text-slate-500 font-sans">qty: 1</div>
              </div>
            </div>
            <div class="text-[11px] text-red-700 text-center font-sans mt-1.5 font-medium">
              ⚠ Lãng phí hàng triệu bản sao String & Object giống hệt nhau ➔ Dẫn tới OutOfMemoryError & GC Pause
            </div>
          </div>

          <!-- ARROW -->
          <div class="flex items-center justify-center -my-1 text-emerald-700 font-bold text-xs gap-1.5">
            <span>↓</span> Áp dụng Flyweight: Tách trạng thái Nội vi (quantity) & Ngoại vi (Item) <span>↓</span>
          </div>

          <!-- AFTER CARD -->
          <div class="bg-emerald-50/70 border-2 border-emerald-400 rounded-xl p-3 shadow-xs">
            <div class="flex items-center justify-between border-b border-emerald-200 pb-1.5 mb-2">
              <span class="text-xs font-bold text-emerald-900 uppercase tracking-wide flex items-center gap-1.5">
                <span class="w-2.5 h-2.5 rounded-full bg-emerald-600"></span>
                ✅ Khi CÓ Flyweight (Chia sẻ đối tượng qua ItemFactory)
              </span>
              <span class="text-xs font-mono font-bold text-emerald-800 bg-emerald-100 px-2.5 py-0.5 rounded border border-emerald-300">Heap: ~95 MB RAM (Tiết kiệm 72%)</span>
            </div>

            <!-- Factory Shared Single Instance -->
            <div class="bg-white border-2 border-blue-400 rounded-lg p-2 mb-2 flex items-center justify-between shadow-2xs">
              <div class="flex items-center gap-2">
                <span class="w-2 h-2 rounded-full bg-blue-600"></span>
                <span class="font-mono text-xs font-bold text-blue-950">ItemFactory (ConcurrentHashMap Pool):</span>
              </div>
              <span class="text-xs font-mono font-bold text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded border border-emerald-300">
                Duy nhất 1 thực thể gốc: ItemFlyweight [ID: 10, Name: "Milk", Price: 2.5]
              </span>
            </div>

            <!-- Lightweight Elements -->
            <div class="grid grid-cols-4 gap-2 text-center text-xs font-mono">
              <div class="bg-white border border-emerald-300 rounded-lg p-2 shadow-2xs">
                <div class="font-bold text-slate-800">Element #1</div>
                <div class="text-[11px] text-blue-700 font-bold">ptr → [Milk]</div>
                <div class="text-[10px] text-slate-600 font-sans">qty: 2 (nội vi)</div>
              </div>
              <div class="bg-white border border-emerald-300 rounded-lg p-2 shadow-2xs">
                <div class="font-bold text-slate-800">Element #2</div>
                <div class="text-[11px] text-blue-700 font-bold">ptr → [Milk]</div>
                <div class="text-[10px] text-slate-600 font-sans">qty: 5 (nội vi)</div>
              </div>
              <div class="bg-white border border-emerald-300 rounded-lg p-2 shadow-2xs flex flex-col justify-center">
                <div class="font-bold text-slate-400">. . .</div>
                <div class="text-[10px] text-slate-400 font-sans">Chỉ tốn con trỏ 8B</div>
              </div>
              <div class="bg-white border border-emerald-300 rounded-lg p-2 shadow-2xs">
                <div class="font-bold text-slate-800">Element #1.000.000</div>
                <div class="text-[11px] text-blue-700 font-bold">ptr → [Milk]</div>
                <div class="text-[10px] text-slate-600 font-sans">qty: 1 (nội vi)</div>
              </div>
            </div>
          </div>
        </div>
        """
    },
    {
        "filename": "06_object_pool_pattern.png",
        "title": "MẪU THIẾT KẾ #06: OBJECT POOL PATTERN",
        "subtitle": "Vòng Đời Tái Chế Đối Tượng Tuần Hoàn & Triệt Tiêu Cấp Phát Bộ Nhớ",
        "layer": "TẦNG 5: DOMAIN MODEL & OPTIMIZATION LAYER",
        "badge": "Giảm >80% Áp Lực Dọn Rác (GC Pauses)",
        "content_html": """
        <div class="h-full flex flex-col justify-between">
          <!-- Top Description -->
          <div class="text-xs text-slate-600 flex items-center justify-between border-b border-slate-200 pb-1.5">
            <span>Cơ chế quản trị tài nguyên đối tượng giao dịch trong <b class="text-indigo-900 font-mono">TransactionPool</b></span>
            <span class="text-xs font-mono font-bold bg-indigo-100 text-indigo-900 px-2 py-0.5 rounded border border-indigo-200">Sức chứa cố định: 10.000 Objects</span>
          </div>

          <!-- Circular 4-Step Diagram Layout -->
          <div class="grid grid-cols-4 gap-3 my-auto items-stretch">
            
            <!-- Step 1: Kho Pool -->
            <div class="bg-blue-50 border-2 border-blue-400 rounded-xl p-3 shadow-xs flex flex-col justify-between">
              <div>
                <div class="flex items-center justify-between mb-1">
                  <span class="text-xs font-bold uppercase text-blue-900 tracking-wider">Trạm 1: Kho Pool</span>
                  <span class="w-5 h-5 rounded-full bg-blue-600 text-white font-bold text-xs flex items-center justify-center">1</span>
                </div>
                <div class="font-mono text-xs font-bold text-blue-950 mt-1">TransactionPool</div>
                <p class="text-[11px] text-slate-600 font-sans mt-1.5 leading-relaxed">
                  Lưu trữ sẵn 10.000 đối tượng trong <code class="font-mono text-blue-800 bg-blue-100 px-1 rounded">ArrayBlockingQueue</code> an toàn đa luồng.
                </p>
              </div>
              <div class="mt-2 text-[10px] font-bold text-blue-700 bg-white border border-blue-200 p-1 rounded text-center">
                Không tạo mới đối tượng
              </div>
            </div>

            <!-- Step 2: Mượn (Acquire) -->
            <div class="bg-amber-50 border-2 border-amber-400 rounded-xl p-3 shadow-xs flex flex-col justify-between">
              <div>
                <div class="flex items-center justify-between mb-1">
                  <span class="text-xs font-bold uppercase text-amber-900 tracking-wider">Trạm 2: Mượn</span>
                  <span class="w-5 h-5 rounded-full bg-amber-600 text-white font-bold text-xs flex items-center justify-center">2</span>
                </div>
                <div class="font-mono text-xs font-bold text-amber-950 mt-1">acquire()</div>
                <p class="text-[11px] text-slate-600 font-sans mt-1.5 leading-relaxed">
                  Dữ liệu stream mới đến ➔ Rút đối tượng có sẵn từ Pool để đóng gói dữ liệu mà không tốn lệnh <code class="font-mono text-red-700">new</code>.
                </p>
              </div>
              <div class="mt-2 text-[10px] font-bold text-amber-700 bg-white border border-amber-200 p-1 rounded text-center">
                Tiết kiệm thời gian cấp phát
              </div>
            </div>

            <!-- Step 3: Tính toán trong Cửa Sổ Trượt -->
            <div class="bg-purple-50 border-2 border-purple-400 rounded-xl p-3 shadow-xs flex flex-col justify-between">
              <div>
                <div class="flex items-center justify-between mb-1">
                  <span class="text-xs font-bold uppercase text-purple-900 tracking-wider">Trạm 3: Sử Dụng</span>
                  <span class="w-5 h-5 rounded-full bg-purple-600 text-white font-bold text-xs flex items-center justify-center">3</span>
                </div>
                <div class="font-mono text-xs font-bold text-purple-950 mt-1">Sliding Window</div>
                <p class="text-[11px] text-slate-600 font-sans mt-1.5 leading-relaxed">
                  Giao dịch tham gia tính toán danh sách tiện ích HUI trong phạm vi cửa sổ trượt kích thước $W$.
                </p>
              </div>
              <div class="mt-2 text-[10px] font-bold text-purple-700 bg-white border border-purple-200 p-1 rounded text-center">
                Hoạt động khai phá chính
              </div>
            </div>

            <!-- Step 4: Tái chế (Reset & Release) -->
            <div class="bg-emerald-50 border-2 border-emerald-400 rounded-xl p-3 shadow-xs flex flex-col justify-between">
              <div>
                <div class="flex items-center justify-between mb-1">
                  <span class="text-xs font-bold uppercase text-emerald-900 tracking-wider">Trạm 4: Tái Chế</span>
                  <span class="w-5 h-5 rounded-full bg-emerald-600 text-white font-bold text-xs flex items-center justify-center">4</span>
                </div>
                <div class="font-mono text-xs font-bold text-emerald-950 mt-1">reset(newTid) & release()</div>
                <p class="text-[11px] text-slate-600 font-sans mt-1.5 leading-relaxed">
                  Cửa sổ trượt qua ➔ Xóa sạch dữ liệu cũ qua hàm <code class="font-mono text-emerald-800 bg-emerald-100 px-1 rounded">reset()</code> và nạp lại vào Pool.
                </p>
              </div>
              <div class="mt-2 text-[10px] font-bold text-emerald-700 bg-white border border-emerald-200 p-1 rounded text-center">
                Tránh Garbage Collection
              </div>
            </div>

          </div>

          <!-- Bottom Loop Connection Bar -->
          <div class="bg-slate-100 border border-slate-300 rounded-lg p-2 flex items-center justify-between text-xs text-slate-700">
            <span class="font-bold flex items-center gap-1.5">
              <span class="text-emerald-600">🔄</span> Vòng lặp kín (Zero-Allocation Loop):
            </span>
            <span>Kho Pool ➔ Mượn ➔ Xử lý trong Cửa sổ ➔ Reset sạch ➔ Trả lại Kho Pool (Lặp lại vô hạn)</span>
          </div>
        </div>
        """
    },
    {
        "filename": "07_template_method_pattern.png",
        "title": "MẪU THIẾT KẾ #07: TEMPLATE METHOD PATTERN",
        "subtitle": "Khung Xương Kiểm Định 6 Bước Cố Định & Các Điểm Nối (Hooks)",
        "layer": "TẦNG 3: ALGORITHM LAYER",
        "badge": "Bảo Toàn Quy Trình Khoa Học (Khóa 'final')",
        "content_html": """
        <div class="h-full flex flex-col justify-between">
          <!-- Abstract Parent Class Skeleton -->
          <div class="bg-slate-900 text-slate-100 border-2 border-indigo-500 rounded-xl p-3 shadow-md">
            <div class="flex items-center justify-between border-b border-slate-700 pb-1.5 mb-2">
              <div class="flex items-center gap-2">
                <span class="w-2.5 h-2.5 rounded-full bg-amber-400"></span>
                <span class="font-mono font-bold text-sm text-indigo-200">Lớp trừu tượng cha: AbstractDriftDetector</span>
              </div>
              <span class="text-xs bg-amber-900/80 text-amber-200 font-bold px-2 py-0.5 rounded font-mono border border-amber-700">
                public final boolean runDetection(...)
              </span>
            </div>
            
            <!-- 6 Sequential Steps Pipeline -->
            <div class="grid grid-cols-6 gap-2 text-center text-xs font-mono">
              <div class="bg-slate-800 border border-slate-700 p-2 rounded flex flex-col justify-between">
                <span class="text-[10px] text-slate-400 font-sans">Bước 1 [Locked]</span>
                <span class="text-slate-200 font-bold mt-1">updateStats()</span>
                <span class="text-[9px] text-slate-400 font-sans">Cập nhật kỳ vọng</span>
              </div>
              <div class="bg-amber-950/70 border-2 border-amber-400 p-2 rounded flex flex-col justify-between">
                <span class="text-[10px] text-amber-300 font-sans font-bold">Bước 2 [Hook]</span>
                <span class="text-amber-200 font-bold mt-1">computeBound()</span>
                <span class="text-[9px] text-amber-300 font-sans">Tính chặn trôi dạt</span>
              </div>
              <div class="bg-slate-800 border border-slate-700 p-2 rounded flex flex-col justify-between">
                <span class="text-[10px] text-slate-400 font-sans">Bước 3 [Locked]</span>
                <span class="text-slate-200 font-bold mt-1">compare()</span>
                <span class="text-[9px] text-slate-400 font-sans">So sánh thống kê</span>
              </div>
              <div class="bg-amber-950/70 border-2 border-amber-400 p-2 rounded flex flex-col justify-between">
                <span class="text-[10px] text-amber-300 font-sans font-bold">Bước 4 [Hook]</span>
                <span class="text-amber-200 font-bold mt-1">checkCondition()</span>
                <span class="text-[9px] text-amber-300 font-sans">Kiểm tra ngưỡng</span>
              </div>
              <div class="bg-slate-800 border border-slate-700 p-2 rounded flex flex-col justify-between">
                <span class="text-[10px] text-slate-400 font-sans">Bước 5 [Locked]</span>
                <span class="text-slate-200 font-bold mt-1">logAlert()</span>
                <span class="text-[9px] text-slate-400 font-sans">Báo động trôi dạt</span>
              </div>
              <div class="bg-slate-800 border border-slate-700 p-2 rounded flex flex-col justify-between">
                <span class="text-[10px] text-slate-400 font-sans">Bước 6 [Locked]</span>
                <span class="text-slate-200 font-bold mt-1">resetWindow()</span>
                <span class="text-[9px] text-slate-400 font-sans">Đặt lại cửa sổ</span>
              </div>
            </div>
          </div>

          <!-- Arrow pointing to concrete hooks -->
          <div class="flex items-center justify-center -my-1 text-slate-500 font-bold text-xs gap-2">
            <span>↳</span> 2 Lớp con chỉ cần ghi đè (Override) đúng các hàm Hook màu vàng <span>↲</span>
          </div>

          <!-- Concrete Subclasses Implementation -->
          <div class="grid grid-cols-2 gap-3 items-stretch">
            
            <!-- Global Drift Subclass -->
            <div class="bg-purple-50 border-2 border-purple-300 rounded-xl p-3 shadow-xs">
              <div class="flex items-center justify-between border-b border-purple-200 pb-1 mb-2">
                <span class="font-mono text-xs font-bold text-purple-900">GlobalDriftDetector (Lớp con 1)</span>
                <span class="text-[10px] bg-purple-200 text-purple-900 font-bold px-2 py-0.5 rounded">Toàn cục</span>
              </div>
              <p class="text-xs text-slate-700 leading-relaxed">
                Điền công thức toán học tính chặn bất đẳng thức <b>Hoeffding Bound</b>:
              </p>
              <div class="mt-2 bg-white border border-purple-200 rounded p-2 text-xs font-mono text-purple-950 font-bold text-center">
                ε = √ [ (R² · ln(1/δ)) / (2 · n) ]
              </div>
            </div>

            <!-- Local Drift Subclass -->
            <div class="bg-blue-50 border-2 border-blue-300 rounded-xl p-3 shadow-xs">
              <div class="flex items-center justify-between border-b border-blue-200 pb-1 mb-2">
                <span class="font-mono text-xs font-bold text-blue-900">LocalDriftDetector (Lớp con 2)</span>
                <span class="text-[10px] bg-blue-200 text-blue-900 font-bold px-2 py-0.5 rounded">Cục bộ</span>
              </div>
              <p class="text-xs text-slate-700 leading-relaxed">
                Điền thuật toán kiểm định nhiều giả thuyết với <b>hiệu chỉnh Bonferroni</b>:
              </p>
              <div class="mt-2 bg-white border border-blue-200 rounded p-2 text-xs font-mono text-blue-950 font-bold text-center">
                α_adjusted = α_overall / |M| (với |M| là số lượng HUI)
              </div>
            </div>

          </div>
        </div>
        """
    },
    {
        "filename": "08_chain_of_responsibility_pattern.png",
        "title": "MẪU THIẾT KẾ #08: CHAIN OF RESPONSIBILITY PATTERN",
        "subtitle": "Chuỗi Đường Ống Phân Tích Cú Pháp Dữ Liệu Đa Định Dạng (Data Parsing Pipeline)",
        "layer": "TẦNG 4: DATA PIPELINE LAYER",
        "badge": "Dễ Dàng Mở Rộng Thêm Định Dạng Mới (OCP)",
        "content_html": """
        <div class="h-full flex flex-col justify-between">
          <!-- Input Header -->
          <div class="flex items-center justify-between border-b border-slate-200 pb-1.5">
            <span class="text-xs font-bold text-slate-600 uppercase tracking-wide">
              Luồng đầu vào: Chuỗi văn bản thô (Raw String Stream) từ File / Socket
            </span>
            <span class="text-xs font-mono font-bold bg-slate-100 text-slate-800 px-2 py-0.5 rounded border border-slate-300">
              TransactionParsingPipeline
            </span>
          </div>

          <!-- Pipeline Stages Flow -->
          <div class="grid grid-cols-12 gap-2 my-auto items-center">
            
            <!-- Raw Input Box -->
            <div class="col-span-3 bg-slate-100 border-2 border-slate-300 rounded-xl p-2.5 shadow-xs text-center">
              <div class="text-[10px] uppercase font-bold text-slate-500">Đầu vào thô</div>
              <div class="font-mono text-xs font-bold text-slate-800 mt-1 truncate">"1 2 3:15:35"</div>
              <div class="text-[10px] text-slate-500 mt-1">Dòng dữ liệu từ tệp</div>
            </div>

            <!-- Arrow 1 -->
            <div class="col-span-1 text-center font-bold text-indigo-600 text-sm">➔</div>

            <!-- Handler 1 -->
            <div class="col-span-2 bg-blue-50 border-2 border-blue-400 rounded-xl p-2.5 shadow-xs text-center flex flex-col justify-between h-32">
              <div class="text-[10px] font-bold text-blue-800 uppercase">Trạm 1: Lọc Dòng</div>
              <div class="font-mono text-[11px] font-bold text-blue-950 mt-1">EmptyAndComment FilterHandler</div>
              <div class="text-[9px] text-slate-500 mt-1">Bỏ qua dòng trống hoặc bắt đầu bằng '#'</div>
            </div>

            <!-- Arrow 2 -->
            <div class="col-span-1 text-center font-bold text-indigo-600 text-sm">➔</div>

            <!-- Handler 2 -->
            <div class="col-span-2 bg-indigo-50 border-2 border-indigo-400 rounded-xl p-2.5 shadow-xs text-center flex flex-col justify-between h-32">
              <div class="text-[10px] font-bold text-indigo-800 uppercase">Trạm 2: SPMF Chuẩn</div>
              <div class="font-mono text-[11px] font-bold text-indigo-950 mt-1">SpmfFormat Handler</div>
              <div class="text-[9px] text-slate-500 mt-1">Khớp cú pháp item:utility:trans_utility</div>
            </div>

            <!-- Arrow 3 -->
            <div class="col-span-1 text-center font-bold text-indigo-600 text-sm">➔</div>

            <!-- Handler 3 -->
            <div class="col-span-2 bg-purple-50 border-2 border-purple-400 rounded-xl p-2.5 shadow-xs text-center flex flex-col justify-between h-32">
              <div class="text-[10px] font-bold text-purple-800 uppercase">Trạm 3: Legacy Format</div>
              <div class="font-mono text-[11px] font-bold text-purple-950 mt-1">LegacyFormat Handler</div>
              <div class="text-[9px] text-slate-500 mt-1">Khớp cú pháp giao dịch cũ tùy biến</div>
            </div>

          </div>

          <!-- Two Possible Outcomes (Success vs Failure) -->
          <div class="grid grid-cols-12 gap-3 items-stretch">
            <div class="col-span-8 bg-emerald-50 border-2 border-emerald-400 rounded-xl p-2.5 shadow-xs flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="w-2.5 h-2.5 rounded-full bg-emerald-600"></span>
                <div>
                  <div class="text-xs font-bold text-emerald-900">Nếu 1 trạm xử lý thành công:</div>
                  <div class="text-[11px] text-slate-600 font-sans">Đóng gói và trả về ngay đối tượng <code class="font-mono font-bold text-emerald-800 bg-white px-1 rounded">Transaction</code> chuẩn cho hệ thống.</div>
                </div>
              </div>
              <span class="text-xs font-mono font-bold text-emerald-800 bg-emerald-100 px-2 py-1 rounded border border-emerald-300">return Transaction</span>
            </div>

            <div class="col-span-4 bg-red-50 border-2 border-red-300 rounded-xl p-2.5 shadow-xs flex items-center justify-between">
              <div>
                <div class="text-xs font-bold text-red-900">Nếu hết chuỗi không khớp:</div>
                <div class="text-[10px] text-red-700">Ném ngoại lệ kiểm tra</div>
              </div>
              <span class="text-[10px] font-mono font-bold text-red-800 bg-red-100 px-1.5 py-1 rounded border border-red-200">ParseException</span>
            </div>
          </div>
        </div>
        """
    }
]

# Generate each HTML and screenshot
html_dir = os.path.join(output_dir, "html_sources")
os.makedirs(html_dir, exist_ok=True)

for d in diagrams:
    html_file = os.path.join(html_dir, d["filename"].replace(".png", ".html"))
    png_file = os.path.join(output_dir, d["filename"])
    
    html_content = f"""<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <title>{d["title"]}</title>
  <script src="https://www.gstatic.com/antigravity/web/dev/tailwindcss.min.js"></script>
  <style>
    body {{
      margin: 0;
      padding: 0;
      background-color: #F5F3EE;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    }}
  </style>
</head>
<body class="p-4 flex items-center justify-center">
  <!-- Sized container for crisp export -->
  <div style="width: 920px; height: 560px;" class="bg-white rounded-2xl border-2 border-slate-300 p-5 shadow-xl flex flex-col justify-between">
    <!-- Header -->
    <div class="flex items-start justify-between border-b-2 border-slate-100 pb-2.5 mb-2">
      <div>
        <div class="flex items-center gap-2 mb-1">
          <span class="px-2 py-0.5 text-[11px] font-bold uppercase rounded bg-indigo-100 text-indigo-900 border border-indigo-200">
            {d["layer"]}
          </span>
          <span class="text-[11px] font-bold text-slate-500 font-mono">HUDD-TDS ARCHITECTURE</span>
        </div>
        <h2 class="text-lg font-bold text-slate-900 tracking-tight">
          {d["title"]}
        </h2>
        <div class="text-xs text-slate-500 mt-0.5">
          {d["subtitle"]}
        </div>
      </div>
      <div class="text-right">
        <span class="text-xs font-mono font-bold text-emerald-800 bg-emerald-50 border border-emerald-300 px-2.5 py-1 rounded-md shadow-2xs">
          {d["badge"]}
        </span>
      </div>
    </div>

    <!-- Main Content Area -->
    <div class="flex-1 my-1">
      {d["content_html"]}
    </div>

    <!-- Footer Note -->
    <div class="border-t border-slate-100 pt-2 flex items-center justify-between text-[11px] text-slate-500">
      <div class="flex items-center gap-1.5 font-medium">
        <span class="w-2 h-2 rounded-full bg-indigo-500"></span>
        <span>Sơ đồ kiến trúc & luồng vận hành kỹ thuật — Đề tài HUDD-TDS</span>
      </div>
      <div class="font-mono text-slate-400">Khóa luận Tốt nghiệp CNTT</div>
    </div>
  </div>
</body>
</html>
"""
    with open(html_file, "w", encoding="utf-8") as f:
        f.write(html_content)

print("Created all 8 HTML diagram sources.")
