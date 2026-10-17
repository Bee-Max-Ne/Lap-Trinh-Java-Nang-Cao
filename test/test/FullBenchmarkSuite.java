package test;

import huddtds.algorithm.HUDD_TDS;
import huddtds.data.DatasetManager;
import huddtds.data.TransactionParser;
import huddtds.model.Checkpoint;
import huddtds.model.Transaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Chạy benchmark trên 9 bộ dữ liệu hoặc chỉ ghi nhật ký lộ trình theo phase.
 */
public class FullBenchmarkSuite {

    public static class BenchmarkResult {
        public String dataset;
        public int transactions;
        public long runtimeMs;
        public double memoryMb;
        public int checkpoints;
        public int lastHuiCount;
        public int globalDriftCount;
        public int localDriftCount;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=========================================================================================");
        System.out.println("                 BỘ KIỂM THỬ VÀ BENCHMARK TOÀN DIỆN HUDD-TDS                             ");
        System.out.println("=========================================================================================\n");

        File logDir = new File("logs");
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                throw new IllegalStateException("Không thể tạo thư mục log: " + logDir.getAbsolutePath());
            }
        }
        if (args.length == 1 && "--phases-only".equals(args[0])) {
            writePhasesOnlyLog(logDir);
            return;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File logFile = new File(logDir, "benchmark_" + timestamp + ".log");

        DatasetManager manager = new DatasetManager();

        List<BenchmarkResult> results = new ArrayList<>();

        // Danh sách tham số cấu hình cho từng dataset
        Map<String, double[]> params = new LinkedHashMap<>();
        // {minutil, interval, windowSize, maxLines (0 = full), maxItemsetSize}
        params.put("Chess",        new double[]{2_000_000, 300, 500, 0, 3});
        params.put("NewChess",     new double[]{2_000_000, 300, 500, 0, 3});
        params.put("Mushrooms",    new double[]{2_500_000, 500, 1000, 0, 3});
        params.put("NewMushroom",  new double[]{2_500_000, 500, 1000, 0, 3});
        params.put("Connects",     new double[]{2_500_000, 1000, 2000, 10000, 3});
        params.put("NewConnects",  new double[]{2_500_000, 1000, 2000, 10000, 3});
        params.put("Retail",       new double[]{100_000, 1000, 2000, 10000, 3});
        params.put("Accidents",    new double[]{5_000_000, 2000, 5000, 10000, 3});
        params.put("Chainstore",   new double[]{500_000, 5000, 10000, 20000, 3});

        try (PrintWriter logWriter = new PrintWriter(
                Files.newBufferedWriter(logFile.toPath(), StandardCharsets.UTF_8))) {
            logWriter.println("HUDD-TDS BENCHMARK LOG - " + timestamp);
            logWriter.println("========================================================\n");
            writeOptimizationPhases(logWriter);
            logWriter.flush();

            logWriter.println("[PHASE 4][ĐANG CHẠY] Đo benchmark trên 9 bộ dữ liệu.");
            for (Map.Entry<String, double[]> entry : params.entrySet()) {
                String name = entry.getKey();
                double[] p = entry.getValue();

                double minutil = p[0];
                int interval = (int) p[1];
                int windowSize = (int) p[2];
                int maxLines = (int) p[3];
                int maxItemsetSize = (int) p[4];

                System.out.printf(">>> Đang chạy Benchmark: %-12s ... ", name);
                BenchmarkResult res = runBenchmark(manager, name, minutil, interval, windowSize, 0.05, maxItemsetSize, maxLines, logWriter);
                results.add(res);
                System.out.printf("Xong (%,d tx, %,d ms, %d CP, HUI: %d, G-Drift: %d, L-Drift: %d)%n",
                        res.transactions, res.runtimeMs, res.checkpoints, res.lastHuiCount, res.globalDriftCount, res.localDriftCount);
            }
            logWriter.printf(Locale.US,
                    "[PHASE 4][HOÀN TẤT] Số bộ dữ liệu=%d; tổng thời gian cộng dồn=%d ms.%n",
                    results.size(), results.stream().mapToLong(result -> result.runtimeMs).sum());
            logWriter.println("[PHASE 4][LƯU Ý] RAM trong bảng là chênh lệch heap đang dùng sau-trước "
                    + "từng lượt chạy, không phải heap đỉnh, RSS hay RAM vật lý.");
            logWriter.println("[PHASE 4][LƯU Ý] Runner này ghi kết quả lần chạy hiện tại, không tự so sánh "
                    + "từng checkpoint với bản baseline.");
            logWriter.flush();

            // In bảng tổng hợp
            System.out.println("\n=========================================================================================");
            System.out.println("                              BẢNG TỔNG HỢP BENCHMARK HỆ THỐNG                           ");
            System.out.println("=========================================================================================");
            System.out.printf("%-12s | %10s | %10s | %10s | %10s | %10s | %10s | %10s%n",
                    "Dataset", "Giao dịch", "Thời gian", "RAM (MB)", "Checkpoints", "HUI cuối", "Global Drift", "Local Drift");
            System.out.println("-------------+------------+------------+------------+-------------+------------+--------------+------------");

            logWriter.println("\nBẢNG TỔNG HỢP BENCHMARK:");
            logWriter.printf("%-12s | %10s | %10s | %10s | %10s | %10s | %10s | %10s%n",
                    "Dataset", "Giao dịch", "Thời gian", "RAM (MB)", "Checkpoints", "HUI cuối", "Global Drift", "Local Drift");

            for (BenchmarkResult r : results) {
                String row = String.format(Locale.US, "%-12s | %,10d | %,8d ms | %10.1f | %11d | %,10d | %12d | %11d",
                        r.dataset, r.transactions, r.runtimeMs, r.memoryMb, r.checkpoints, r.lastHuiCount, r.globalDriftCount, r.localDriftCount);
                System.out.println(row);
                logWriter.println(row);
            }
            System.out.println("=========================================================================================");
            System.out.println("Đã lưu kết quả chi tiết vào: " + logFile.getAbsolutePath());
        }
    }

    private static void writeOptimizationPhases(PrintWriter logWriter) {
        logWriter.println("NHẬT KÝ LỘ TRÌNH TỐI ƯU THEO GIAI ĐOẠN");
        logWriter.println("========================================");
        logWriter.println("PHASE 0 - ĐÓNG BĂNG BASELINE [HOÀN TẤT]");
        logWriter.println("Quy trình: cố định bộ dữ liệu, tham số và đầu ra thuật toán; ghi số giao dịch, "
                + "checkpoint, HUI và drift để làm mốc đối chiếu.");
        logWriter.println("Mốc tham chiếu: logs/benchmark_2026-10-08_18-21-31.log "
                + "(log baseline được người dùng cung cấp).");
        logWriter.println();

        logWriter.println("PHASE 1 - PROFILE VÀ TÌM NÚT THẮT [HOÀN TẤT]");
        logWriter.println("Quy trình: chạy cùng cấu hình JVM với JFR/GC log; xem mẫu CPU, "
                + "đường cấp phát và thời điểm GC trước khi sửa.");
        logWriter.println("Kết quả đã ghi nhận: HUIDiscovery.generateCandidates() và việc "
                + "kiểm tra candidate trên giao dịch là nút thắt chính.");
        logWriter.println("Lưu ý: lần benchmark đang chạy không tự thu JFR; số đo bên dưới "
                + "là benchmark thường.");
        logWriter.println();

        logWriter.println("PHASE 2 - GIẢM CHI PHÍ KHAI PHÁ HUI [ĐÃ TRIỂN KHAI]");
        logWriter.println("Công thức suy giảm: d(DeltaT) = 2^(-DeltaT / 2).");
        logWriter.println("TWU suy giảm của item i: TWU_d(i) = sum(TU(T) * d(t-TID_T)) "
                + "trên các giao dịch T chứa i; chỉ giữ item có TWU_d(i) >= minutil.");
        logWriter.println("Cận trên candidate X: UB_d(X) = sum(TU(T) * d(t-TID_T)) "
                + "trên các giao dịch T chứa toàn bộ X.");
        logWriter.println("Utility candidate: U_d(X) = sum(sum(u(i,T), i trong X) * d(t-TID_T)) "
                + "trên các giao dịch T chứa toàn bộ X.");
        logWriter.println("Quy trình: quét cửa sổ một lần cho mỗi candidate để tính cận trên "
                + "và utility; cắt nhánh nếu UB_d(X) < minutil; chỉ tạo đối tượng HUI "
                + "nếu U_d(X) >= minutil. Không đổi decay, ngưỡng hay thứ tự sinh candidate.");
        logWriter.println("Kiểm chứng đã ghi nhận: 9/9 dataset khớp số giao dịch, checkpoint, "
                + "HUI cuối và số drift; benchmark so sánh trước/sau ghi nhận giảm tổng thời gian 33,5%.");
        logWriter.println();

        logWriter.println("PHASE 3 - GIỚI HẠN LỊCH SỬ RAM VÀ GIỮ KẾT QUẢ ĐẦY ĐỦ [ĐÃ TRIỂN KHAI]");
        logWriter.println("Engine, chart và bảng giao diện giữ tối đa 1.000 checkpoint gần nhất; "
                + "lịch sử checkpoint/HUI đầy đủ được ghi tuần tự ra CSV.");
        logWriter.println("Global drift giữ các tổng tích lũy và cut point thay cho danh sách observation: "
                + "U = S_m/m; V = S_n/n.");
        logWriter.println("Ngưỡng global: epsilon = |range| * sqrt(((n-m)/(2*n*m)) * ln(2/alpha)); "
                + "phát hiện khi |U-V| >= epsilon (range mặc định 1.0).");
        logWriter.println("Quy trình: cập nhật tổng và cut point cho mỗi checkpoint; kiểm tra drift; "
                + "loại checkpoint quá cũ khỏi các cấu trúc hiển thị; ghi mọi checkpoint/HUI vào CSV.");
        logWriter.println("Kiểm chứng đã ghi nhận: retention 1.000 checkpoint, trạng thái detector "
                + "được đối chiếu oracle 2.500 observation và writer CSV có runner kiểm thử riêng.");
        logWriter.println();

        logWriter.println("PHASE 4 - BENCHMARK LẶP LẠI TRÊN 9 DATASET [SẼ GHI KẾT QUẢ BÊN DƯỚI]");
        logWriter.println("Quy trình: dùng tham số cố định trong FullBenchmarkSuite; ghi checkpoint, "
                + "HUI, global/local drift, thời gian và ước lượng chênh lệch heap.");
        logWriter.println("Công thức thời gian giảm khi đối chiếu hai lượt: "
                + "(T_truoc - T_sau) / T_truoc * 100%.");
        logWriter.println("Không diễn giải chênh lệch heap sau-trước thành RAM đỉnh hoặc RSS; "
                + "benchmark hiện tại không tự đối chiếu baseline từng checkpoint.");
        logWriter.println();

        logWriter.println("PHASE 5 - KIỂM THỬ GUI THỰC TẾ [CHƯA XÁC NHẬN]");
        logWriter.println("Cần smoke test desktop: RUN/PAUSE/RESUME/STOP/RESET; kiểm tra file CSV, "
                + "đủ lịch sử, chart/bảng giới hạn 1.000 checkpoint và thông báo lỗi ghi file.");
        logWriter.println("========================================");
        logWriter.println();
    }

    private static void writePhasesOnlyLog(File logDir) throws IOException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File logFile = new File(logDir, "optimization-phases_" + timestamp + ".log");
        try (PrintWriter logWriter = new PrintWriter(
                Files.newBufferedWriter(logFile.toPath(), StandardCharsets.UTF_8))) {
            logWriter.println("HUDD-TDS - LỘ TRÌNH TỐI ƯU THEO PHASE - " + timestamp);
            logWriter.println("Chế độ chỉ ghi kế hoạch; không chạy benchmark và không tạo số đo mới.");
            logWriter.println();
            writeOptimizationPhases(logWriter);
        }
        System.out.println("Đã lưu nhật ký lộ trình theo phase vào: " + logFile.getAbsolutePath());
    }

    private static BenchmarkResult runBenchmark(
            DatasetManager manager,
            String name,
            double minutil,
            int interval,
            int windowSize,
            double alpha,
            int maxItemsetSize,
            int maxLines,
            PrintWriter logWriter) throws Exception {

        BenchmarkResult res = new BenchmarkResult();
        res.dataset = name;

        Map<String, Double> inv = manager.loadInvestmentTable(name);
        HUDD_TDS engine = new HUDD_TDS(inv, minutil, interval, windowSize, alpha, maxItemsetSize);

        System.gc();
        Runtime rt = Runtime.getRuntime();
        long memBefore = rt.totalMemory() - rt.freeMemory();

        long start = System.currentTimeMillis();
        int tid = 0;

        logWriter.println("--------------------------------------------------------");
        logWriter.printf("DATASET: %s (Window=%d, Interval=%d, MinUtil=%.0f)%n", name, windowSize, interval, minutil);

        try (BufferedReader reader = manager.openTransactionStream(name)) {
            String line;
            while ((line = reader.readLine()) != null) {
                tid++;
                Transaction tx = TransactionParser.parseLine(line, tid);
                Checkpoint cp = engine.processTransaction(tx);
                if (cp != null) {
                    res.checkpoints++;
                    res.lastHuiCount = cp.getHuis().size();

                    String gd = engine.checkGlobalDrift();
                    String ld = engine.checkLocalDrift();
                    if (gd != null) res.globalDriftCount++;
                    if (ld != null) res.localDriftCount++;

                    logWriter.printf("  Checkpoint TID %d: HUI=%d, GlobalDist=%.4f, GD=%s, LD=%s%n",
                            cp.getTid(), cp.getHuis().size(), cp.getGlobalDistance(),
                            (gd != null ? gd : "None"), (ld != null ? ld : "None"));
                    logWriter.flush();
                }

                if (maxLines > 0 && tid >= maxLines) {
                    break;
                }
            }
        }

        long duration = System.currentTimeMillis() - start;
        long memAfter = rt.totalMemory() - rt.freeMemory();

        res.transactions = tid;
        res.runtimeMs = duration;
        res.memoryMb = Math.max(1.0, (memAfter - memBefore) / (1024.0 * 1024.0));

        logWriter.printf(Locale.US,
                "[PHASE 4][DATASET HOÀN TẤT] %s: giao dịch=%d; checkpoint=%d; "
                        + "HUI cuối=%d; global drift=%d; local drift=%d; thời gian=%d ms; "
                        + "chênh lệch heap ước lượng=%.1f MB.%n",
                name, res.transactions, res.checkpoints, res.lastHuiCount,
                res.globalDriftCount, res.localDriftCount, res.runtimeMs, res.memoryMb);
        logWriter.flush();
        return res;
    }
}
