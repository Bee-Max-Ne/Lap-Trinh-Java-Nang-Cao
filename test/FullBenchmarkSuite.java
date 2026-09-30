package test;

import huddtds.algorithm.HUDD_TDS;
import huddtds.data.DatasetManager;
import huddtds.data.TransactionParser;
import huddtds.model.Checkpoint;
import huddtds.model.Transaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Giai đoạn 21 & 22: Bộ Benchmark toàn diện 9 Dataset và tự động ghi log vào logs/.
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
            logDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File logFile = new File(logDir, "benchmark_" + timestamp + ".log");

        DatasetManager manager = new DatasetManager();
        List<String> datasets = manager.getDatasetNames();

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

        try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile))) {
            logWriter.println("HUDD-TDS BENCHMARK LOG - " + timestamp);
            logWriter.println("========================================================\n");

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

        return res;
    }
}
