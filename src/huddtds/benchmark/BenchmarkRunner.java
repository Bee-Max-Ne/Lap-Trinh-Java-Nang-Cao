package huddtds.benchmark;

import huddtds.application.facade.SimulationFacade;
import huddtds.application.SimulationConfiguration;
import huddtds.application.event.SimulationEvent;
import huddtds.application.event.EventType;
import huddtds.application.SimulationService;
import huddtds.demo.ChartPanel;
import huddtds.demo.ReportExporter;
import huddtds.demo.SummaryChartsPanel;
import huddtds.model.DriftResult;
import huddtds.model.Checkpoint;
import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.swing.table.DefaultTableModel;

/**
 * Trình thực thi Benchmark tự động cho toàn bộ bộ dữ liệu của HUDD-TDS.
 * Hỗ trợ thu thập chỉ số hiệu năng, thời gian, RAM, GC, đối tượng cấp phát,
 * và tự động kết xuất báo cáo HTML đầy đủ cùng tệp JSON tóm tắt.
 */
public class BenchmarkRunner {

    private final SimulationFacade facade;
    private final File outputDirectory;

    public BenchmarkRunner(File outputDirectory) {
        this.facade = new SimulationFacade();
        this.outputDirectory = outputDirectory;
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
    }

    private static long getGcCount() {
        long sum = 0;
        for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
            long c = b.getCollectionCount();
            if (c > 0) sum += c;
        }
        return sum;
    }

    private static long getGcTime() {
        long sum = 0;
        for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
            long t = b.getCollectionTime();
            if (t > 0) sum += t;
        }
        return sum;
    }

    public BenchmarkMetrics runDataset(BenchmarkConfig config) throws Exception {
        // 1. Dọn sạch bộ nhớ trước khi đo để đảm bảo tính khách quan
        System.gc();
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {}
        System.gc();

        long startGcCount = getGcCount();
        long startGcTimeMs = getGcTime();
        long startTimeMs = System.currentTimeMillis();

        Runtime rt = Runtime.getRuntime();
        double startRamMb = (rt.totalMemory() - rt.freeMemory()) / (1024.0 * 1024.0);
        double maxRamMb = rt.maxMemory() / (1024.0 * 1024.0);

        final BenchmarkMetrics metrics = new BenchmarkMetrics();
        metrics.datasetName = config.getDatasetName();
        metrics.safeFileName = config.getSafeFileName();
        metrics.architecture = "Design Patterns (Object Pool, Flyweight, Facade, Strategy, Pipeline)";
        metrics.minutil = config.getMinutil();
        metrics.interval = config.getInterval();
        metrics.windowSize = config.getWindowSize();
        metrics.alpha = config.getAlpha();
        metrics.maxPattern = config.getMaxPattern();
        metrics.maxRamMb = maxRamMb;

        final Set<String> distinctHuis = new HashSet<>();
        final List<Double> checkpointTimes = new ArrayList<>();

        ChartPanel chartPanel = new ChartPanel();
        SummaryChartsPanel summaryChartsPanel = new SummaryChartsPanel();

        SimulationConfiguration simConfig = SimulationConfiguration.builder(config.getDatasetName())
                .withMinutil(config.getMinutil())
                .withInterval(config.getInterval())
                .withWindowSize(config.getWindowSize())
                .withAlphaConfidence(config.getAlpha())
                .withMaxItemsetSize(config.getMaxPattern())
                .build();

        SimulationService service = facade.createSimulationService(simConfig);

        service.subscribe(event -> {
            if (event.getType() == EventType.CHECKPOINT_CREATED) {
                Checkpoint cp = event.getCheckpoint();
                long cpNow = System.currentTimeMillis();
                checkpointTimes.add((double) (cpNow - startTimeMs));

                Runtime r = Runtime.getRuntime();
                double curRam = (r.totalMemory() - r.freeMemory()) / (1024.0 * 1024.0);
                long elapsed = Math.max(1, cpNow - startTimeMs);
                int curTps = (int) ((metrics.totalTransactions * 1000.0) / elapsed);

                cp.setRamUsageMb(curRam);
                cp.setThroughputTxPerSec(curTps);

                metrics.peakRamMb = Math.max(metrics.peakRamMb, curRam);
                metrics.peakTps = Math.max(metrics.peakTps, curTps);
                metrics.totalCheckpoints++;
                metrics.totalHuis += cp.getHuis().size();
                metrics.maxHuiPerCp = Math.max(metrics.maxHuiPerCp, cp.getHuis().size());

                metrics.avgDisHs += cp.getGlobalDistance();
                metrics.maxDisHs = Math.max(metrics.maxDisHs, cp.getGlobalDistance());

                for (HighUtilityItemset hui : cp.getHuis()) {
                    distinctHuis.add(hui.getItems().toString());
                    if (hui.getTotalUtility() > metrics.topUtility) {
                        metrics.topUtility = hui.getTotalUtility();
                        metrics.topItemset = hui.getItems().toString();
                    }
                }

                DriftResult gDrift = event.getGlobalDrift();
                DriftResult lDrift = event.getLocalDrift();
                if (gDrift != null && gDrift.isDetected()) {
                    metrics.globalDrifts++;
                }
                if (lDrift != null && lDrift.isDetected()) {
                    metrics.localDrifts++;
                }

                chartPanel.addCheckpoint(cp, gDrift);
            }
        });

        // Đọc và phát luồng giao dịch
        int tid = 0;
        try (BufferedReader reader = facade.openTransactionStream(
                config.getDatasetName(), null, config.getManualInput())) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (config.getMaxTxLimit() > 0 && tid >= config.getMaxTxLimit()) break;
                tid++;
                metrics.totalTransactions = tid;
                Transaction tx = service.processLine(line, tid);
                metrics.totalAllocatedObjects += 1 + (tx != null ? tx.getElements().size() : 0);
            }
        }

        service.finish(tid);

        long endGcCount = getGcCount();
        long endGcTimeMs = getGcTime();
        long totalElapsedMs = Math.max(1, System.currentTimeMillis() - startTimeMs);
        double elapsedSec = totalElapsedMs / 1000.0;

        Runtime finalRt = Runtime.getRuntime();
        double finalRamMb = (finalRt.totalMemory() - finalRt.freeMemory()) / (1024.0 * 1024.0);

        metrics.elapsedSeconds = elapsedSec;
        metrics.avgTps = (int) ((metrics.totalTransactions * 1000.0) / totalElapsedMs);
        metrics.currentRamMb = finalRamMb;
        metrics.peakRamMb = Math.max(metrics.peakRamMb, finalRamMb);
        metrics.gcCount = Math.max(0, endGcCount - startGcCount);
        metrics.gcTimeMs = Math.max(0, endGcTimeMs - startGcTimeMs);
        metrics.gcOverheadPct = Math.min(100.0, (metrics.gcTimeMs * 100.0) / totalElapsedMs);
        metrics.avgCpMs = metrics.totalCheckpoints > 0 ? (totalElapsedMs / (double) metrics.totalCheckpoints) : 0.0;
        metrics.distinctHuis = distinctHuis.size();
        metrics.avgHuiPerCp = metrics.totalCheckpoints > 0 ? ((double) metrics.totalHuis / metrics.totalCheckpoints) : 0.0;
        if (metrics.totalCheckpoints > 0) {
            metrics.avgDisHs = metrics.avgDisHs / metrics.totalCheckpoints;
            metrics.driftRatePct = ((double) (metrics.globalDrifts + metrics.localDrifts) * 100.0) / metrics.totalCheckpoints;
        }

        // Cập nhật SummaryChartsPanel
        summaryChartsPanel.updateData(
                metrics.totalCheckpoints,
                metrics.globalDrifts,
                metrics.localDrifts,
                metrics.currentRamMb,
                metrics.peakRamMb,
                metrics.maxRamMb,
                metrics.avgTps,
                metrics.peakTps,
                metrics.gcCount,
                metrics.gcTimeMs,
                metrics.gcOverheadPct,
                metrics.totalAllocatedObjects
        );

        // Tạo TableModel cho báo cáo
        DefaultTableModel statsTableModel = createStatsTableModel(metrics);

        // Chuẩn bị KPI array
        String[] kpis = new String[]{
                String.format(Locale.US, "%.2fs", metrics.elapsedSeconds),
                String.format(Locale.US, "%,d tx/s", metrics.avgTps),
                String.format(Locale.US, "%.1f MB", metrics.peakRamMb),
                String.format(Locale.US, "%,d obj", metrics.totalAllocatedObjects),
                String.format(Locale.US, "%,d HUI", metrics.distinctHuis),
                String.format(Locale.US, "%.1f%% (%d/%d)", metrics.driftRatePct,
                        (metrics.globalDrifts + metrics.localDrifts), metrics.totalCheckpoints)
        };

        // Chuẩn bị văn bản báo cáo khoa học
        String summaryText = buildSummaryText(metrics);

        // Xuất tệp HTML hoàn chỉnh
        String htmlFileName = config.getSafeFileName() + "_report.html";
        File htmlTargetFile = new File(outputDirectory, htmlFileName);
        ReportExporter.exportReportToFile(
                htmlTargetFile,
                config.getDatasetName(),
                kpis,
                statsTableModel,
                summaryText,
                chartPanel,
                summaryChartsPanel
        );
        metrics.htmlReportFile = htmlFileName;

        // Xuất tệp JSON tóm tắt
        String jsonFileName = config.getSafeFileName() + "_summary.json";
        File jsonTargetFile = new File(outputDirectory, jsonFileName);
        try (PrintWriter writer = new PrintWriter(new FileWriter(jsonTargetFile, StandardCharsets.UTF_8))) {
            writer.print(metrics.toJson());
        }

        return metrics;
    }

    private DefaultTableModel createStatsTableModel(BenchmarkMetrics m) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Chỉ số đánh giá", "Giá trị thực nghiệm"}, 0);

        model.addRow(new Object[]{"▶ I. CẤU HÌNH THỰC NGHIỆM", "--------------------------------------"});
        model.addRow(new Object[]{"  Bộ dữ liệu (Dataset)", m.datasetName});
        model.addRow(new Object[]{"  Tổng số giao dịch (Transactions)", String.format(Locale.US, "%,d", m.totalTransactions)});
        model.addRow(new Object[]{"  Ngưỡng độ lợi tối thiểu (MinUtil)", String.format(Locale.US, "%,.2f", m.minutil)});
        model.addRow(new Object[]{"  Chu kỳ kiểm tra (Interval)", String.format(Locale.US, "%d tx", m.interval)});
        model.addRow(new Object[]{"  Kích thước cửa sổ (Window Size)", String.format(Locale.US, "%d tx", m.windowSize)});
        model.addRow(new Object[]{"  Mức ý nghĩa thống kê (Alpha)", String.format(Locale.US, "%.4f", m.alpha)});
        model.addRow(new Object[]{"  Độ dài tập mục tối đa (Max Pattern)", String.format(Locale.US, "%d", m.maxPattern)});
        model.addRow(new Object[]{"  Mô hình kiến trúc phần mềm", m.architecture});
        model.addRow(new Object[]{"  Cơ chế quản lý đối tượng", "Tái sử dụng qua TransactionPool & ItemFactory Flyweight"});
        model.addRow(new Object[]{"  Trạng thái thực thi", "HOÀN TẤT THÀNH CÔNG ✓"});

        model.addRow(new Object[]{"▶ II. HIỆU NĂNG & TÀI NGUYÊN (RESOURCES)", "--------------------------------------"});
        model.addRow(new Object[]{"  Thời gian thực thi (Elapsed Time)", String.format(Locale.US, "%.2f giây", m.elapsedSeconds)});
        model.addRow(new Object[]{"  Tốc độ thông lượng trung bình", String.format(Locale.US, "%,d tx/giây", m.avgTps)});
        model.addRow(new Object[]{"  Tốc độ thông lượng đỉnh (Peak TPS)", String.format(Locale.US, "%,d tx/giây", m.peakTps)});
        model.addRow(new Object[]{"  Bộ nhớ RAM Heap đang dùng", String.format(Locale.US, "%.2f MB", m.currentRamMb)});
        model.addRow(new Object[]{"  Bộ nhớ RAM Heap đỉnh điểm (Peak Heap)", String.format(Locale.US, "%.2f MB", m.peakRamMb)});
        model.addRow(new Object[]{"  Giới hạn bộ nhớ JVM (Max Heap)", String.format(Locale.US, "%.2f MB", m.maxRamMb)});
        model.addRow(new Object[]{"  Ước tính đối tượng đã xử lý (Processed)", String.format(Locale.US, "%,d đối tượng (tái sử dụng qua Pool)", m.totalAllocatedObjects)});
        model.addRow(new Object[]{"  Thu gom rác (Garbage Collection)", String.format(Locale.US, "%d lần (%d ms pause)", m.gcCount, m.gcTimeMs)});
        model.addRow(new Object[]{"  Tỷ lệ thời gian dừng GC (GC Overhead)", String.format(Locale.US, "%.2f%% tổng thời gian CPU", m.gcOverheadPct)});
        model.addRow(new Object[]{"  Thời gian xử lý TB / Checkpoint", String.format(Locale.US, "%.2f ms/checkpoint", m.avgCpMs)});

        model.addRow(new Object[]{"▶ III. KHAI PHÁ TẬP MỤC ĐỘ LỢI CAO (HUI)", "--------------------------------------"});
        model.addRow(new Object[]{"  Số tập mục HUI phân biệt (Distinct)", String.format(Locale.US, "%,d tập mục duy nhất", m.distinctHuis)});
        model.addRow(new Object[]{"  Tổng số lượt HUI phát hiện", String.format(Locale.US, "%,d lượt", m.totalHuis)});
        model.addRow(new Object[]{"  Số HUI trung bình / Checkpoint", String.format(Locale.US, "%.1f itemsets", m.avgHuiPerCp)});
        model.addRow(new Object[]{"  Số HUI lớn nhất tại 1 Checkpoint", String.format(Locale.US, "%,d itemsets", m.maxHuiPerCp)});
        model.addRow(new Object[]{"  Top HUI có độ lợi lớn nhất", m.topItemset + " (Utility: " + String.format(Locale.US, "%.2f", m.topUtility) + ")"});

        model.addRow(new Object[]{"▶ IV. GIÁM SÁT TRÔI DẠT (CONCEPT DRIFT)", "--------------------------------------"});
        model.addRow(new Object[]{"  Tổng số Checkpoint đã tạo", String.format(Locale.US, "%,d", m.totalCheckpoints)});
        model.addRow(new Object[]{"  Số lần phát hiện Global Drift", String.format(Locale.US, "%,d lần", m.globalDrifts)});
        model.addRow(new Object[]{"  Số lần phát hiện Local Drift", String.format(Locale.US, "%,d lần", m.localDrifts)});
        model.addRow(new Object[]{"  Tần suất xuất hiện Trôi dạt (Rate)", String.format(Locale.US, "%.1f%% (%d/%d checkpoints)", m.driftRatePct, (m.globalDrifts + m.localDrifts), m.totalCheckpoints)});
        model.addRow(new Object[]{"  Khoảng cách DIS_HS trung bình", String.format(Locale.US, "%.4f", m.avgDisHs)});
        model.addRow(new Object[]{"  Khoảng cách DIS_HS đỉnh điểm", String.format(Locale.US, "%.4f", m.maxDisHs)});

        return model;
    }

    private String buildSummaryText(BenchmarkMetrics m) {
        return String.format(Locale.US,
                "================================================================================%n"
                + "       BÁO CÁO TỔNG HỢP THỰC NGHIỆM HUDD-TDS (DESIGN PATTERNS)                 %n"
                + "================================================================================%n"
                + "1. CẤU HÌNH THỰC NGHIỆM:%n"
                + "   - Bộ dữ liệu (Dataset)            : %s%n"
                + "   - Ngưỡng độ lợi tối thiểu (MinUtil): %.2f%n"
                + "   - Chu kỳ kiểm tra (Interval)      : %d giao dịch%n"
                + "   - Kích thước cửa sổ (Window Size) : %d giao dịch%n"
                + "   - Mức ý nghĩa thống kê (Alpha)    : %.4f%n"
                + "   - Độ dài tập mục tối đa (MaxLen)  : %d%n"
                + "   - Mô hình kiến trúc               : %s%n"
                + "   - Quản lý đối tượng               : Tái sử dụng qua TransactionPool & ItemFactory Flyweight%n"
                + "--------------------------------------------------------------------------------%n"
                + "2. HIỆU NĂNG & TÀI NGUYÊN HỆ THỐNG:%n"
                + "   - Tổng số giao dịch đã xử lý      : %,d tx%n"
                + "   - Tổng thời gian thực thi         : %.2f giây%n"
                + "   - Tốc độ thông lượng trung bình   : %,d tx/giây%n"
                + "   - Tốc độ thông lượng đỉnh điểm    : %,d tx/giây%n"
                + "   - Bộ nhớ RAM Heap tiêu thụ        : %.2f MB (Đỉnh: %.2f MB | Max: %.2f MB)%n"
                + "   - Ước tính đối tượng đã xử lý     : %,d đối tượng (Object Pool & Flyweight Active)%n"
                + "   - Hoạt động Garbage Collection    : %d lần thu gom (Tổng dừng: %d ms)%n"
                + "   - Tỷ lệ chi phí thời gian GC      : %.2f%% tổng thời gian CPU%n"
                + "   - Thời gian xử lý TB/Checkpoint   : %.2f ms/checkpoint%n"
                + "--------------------------------------------------------------------------------%n"
                + "3. KẾT QUẢ KHAI PHÁ ĐỘ LỢI CAO (HUI):%n"
                + "   - Số tập mục HUI phân biệt        : %,d tập mục duy nhất%n"
                + "   - Tổng số lượt HUI phát hiện      : %,d lượt%n"
                + "   - Số HUI trung bình / Checkpoint  : %.1f itemsets%n"
                + "   - Số HUI lớn nhất tại Checkpoint  : %,d itemsets%n"
                + "   - Top HUI có độ lợi cao nhất      : %s (Utility: %.2f)%n"
                + "--------------------------------------------------------------------------------%n"
                + "4. GIÁM SÁT TRÔI DẠT (CONCEPT DRIFT):%n"
                + "   - Tổng số Checkpoint đã tạo       : %,d%n"
                + "   - Số lần phát hiện Global Drift   : %,d lần%n"
                + "   - Số lần phát hiện Local Drift    : %,d lần%n"
                + "   - Tần suất xuất hiện Trôi dạt     : %.1f%%%n"
                + "   - Khoảng cách DIS_HS trung bình   : %.4f%n"
                + "   - Khoảng cách DIS_HS đỉnh điểm    : %.4f%n"
                + "--------------------------------------------------------------------------------%n"
                + "Trạng thái thực thi hiện tại: HOÀN TẤT THÀNH CÔNG ✓%n"
                + "================================================================================%n",
                m.datasetName, m.minutil, m.interval, m.windowSize, m.alpha, m.maxPattern,
                m.architecture,
                m.totalTransactions, m.elapsedSeconds, m.avgTps, m.peakTps,
                m.currentRamMb, m.peakRamMb, m.maxRamMb,
                m.totalAllocatedObjects,
                m.gcCount, m.gcTimeMs, m.gcOverheadPct, m.avgCpMs,
                m.distinctHuis, m.totalHuis, m.avgHuiPerCp, m.maxHuiPerCp,
                m.topItemset, m.topUtility,
                m.totalCheckpoints, m.globalDrifts, m.localDrifts,
                m.driftRatePct, m.avgDisHs, m.maxDisHs
        );
    }

    public List<BenchmarkMetrics> runAllSuite() {
        List<BenchmarkConfig> suite = BenchmarkConfig.getStandardSuite();
        List<BenchmarkMetrics> results = new ArrayList<>();

        System.out.println("================================================================================");
        System.out.println("   KHỞI ĐỘNG BENCHMARK TỰ ĐỘNG TOÀN BỘ 10 BỘ DỮ LIỆU (DESIGN PATTERNS)");
        System.out.println("   Thư mục lưu kết quả: " + outputDirectory.getAbsolutePath());
        System.out.println("================================================================================");

        int index = 1;
        long totalBenchmarkStart = System.currentTimeMillis();

        for (BenchmarkConfig cfg : suite) {
            System.out.printf(Locale.US, "[%2d/10] Đang chạy dataset: %-25s | MinUtil=%,.0f, Inter=%d, Win=%d...%n",
                    index, cfg.getDatasetName(), cfg.getMinutil(), cfg.getInterval(), cfg.getWindowSize());
            try {
                BenchmarkMetrics m = runDataset(cfg);
                results.add(m);
                System.out.printf(Locale.US, "       => Xong! Tx=%,d | Thời gian=%.2fs | Thông lượng=%,d tx/s | Đỉnh RAM=%.1f MB | Checkpoints=%d | HUI=%d | File: %s%n",
                        m.totalTransactions, m.elapsedSeconds, m.avgTps, m.peakRamMb, m.totalCheckpoints, m.distinctHuis, m.htmlReportFile);
            } catch (Exception ex) {
                System.err.printf("[ERROR] Lỗi khi chạy dataset %s: %s%n", cfg.getDatasetName(), ex.getMessage());
                ex.printStackTrace();
            }
            index++;
        }

        long totalBenchmarkElapsed = System.currentTimeMillis() - totalBenchmarkStart;
        System.out.println("================================================================================");
        System.out.printf(Locale.US, "   HOÀN TẤT BENCHMARK 10 DATASETS! Tổng thời gian đo: %.2f giây%n", totalBenchmarkElapsed / 1000.0);
        System.out.println("================================================================================");

        return results;
    }

    public static void main(String[] args) {
        File outDir = new File("benchmark_experiments/02_with_design_patterns");
        for (int i = 0; i < args.length; i++) {
            if ("--out".equals(args[i]) && i + 1 < args.length) {
                outDir = new File(args[++i]);
            }
        }
        BenchmarkRunner runner = new BenchmarkRunner(outDir);
        runner.runAllSuite();
    }
}
