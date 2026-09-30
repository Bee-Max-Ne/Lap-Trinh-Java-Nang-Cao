package test;

import huddtds.algorithm.HUDD_TDS;
import huddtds.data.DatasetManager;
import huddtds.data.TransactionParser;
import huddtds.model.Checkpoint;
import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import java.io.BufferedReader;
import java.util.Locale;
import java.util.Map;

/**
 * Mốc nghiệm thu 2: Chạy kiểm thử End-to-End toàn bộ tập dữ liệu Chess (3,196 giao dịch).
 */
public class EndToEndChessRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("     BẮT ĐẦU CHẠY THỰC NGHIỆM END-TO-END: CHESS   ");
        System.out.println("==================================================");

        DatasetManager manager = new DatasetManager();
        Map<String, Double> investmentTable = manager.loadInvestmentTable("Chess");
        System.out.printf("Đã nạp bảng đầu tư Chess: %d items%n", investmentTable.size());

        // Thiết lập tham số luồng:
        // Window = 500 giao dịch, Interval = 200 giao dịch
        // minutil = 1,200,000,000 (1.2 tỷ, tương đương ~25% TU trung bình của window)
        // alpha = 0.05
        int windowSize = 500;
        int interval = 200;
        double minutil = 2_000_000.0;
        double alpha = 0.05;
        int maxItemsetSize = 3; // Khai phá các tập mục 1, 2, 3 items

        System.out.printf("Cấu hình: Window=%d, Interval=%d, MinUtil=%,.0f, Alpha=%.2f, MaxLength=%d%n%n",
                windowSize, interval, minutil, alpha, maxItemsetSize);

        HUDD_TDS engine = new HUDD_TDS(investmentTable, minutil, interval, windowSize, alpha, maxItemsetSize);

        long startTime = System.currentTimeMillis();
        int totalTransactions = 0;
        int checkpointCount = 0;

        try (BufferedReader reader = manager.openTransactionStream("Chess")) {
            String line;
            int tid = 0;

            while ((line = reader.readLine()) != null) {
                tid++;
                totalTransactions++;

                Transaction tx = TransactionParser.parseLine(line, tid);
                Checkpoint cp = engine.processTransaction(tx);

                if (cp != null) {
                    checkpointCount++;
                    System.out.printf(">>> CHECKPOINT #%d tại TID %d:%n", checkpointCount, cp.getTid());
                    System.out.printf("    - Số lượng HUI tìm thấy : %d itemsets%n", cp.getHuis().size());
                    System.out.printf("    - Khoảng cách toàn cục  : %.4f%n", cp.getGlobalDistance());

                    // In 3 HUI đầu tiên
                    int shown = 0;
                    for (HighUtilityItemset hui : cp.getHuis()) {
                        if (shown++ < 3) {
                            System.out.printf("      * %s => Utility=%,.2f, Dmo=%.4f%n",
                                    hui.getItems(), hui.getTotalUtility(), hui.getDistanceToRoot());
                        }
                    }
                    if (cp.getHuis().size() > 3) {
                        System.out.printf("      * ... (và %d itemsets khác)%n", cp.getHuis().size() - 3);
                    }

                    // Kiểm định Drift
                    String globalDrift = engine.checkGlobalDrift();
                    String localDrift = engine.checkLocalDrift();
                    if (globalDrift != null) {
                        System.out.printf("    [CẢNH BÁO TOÀN CỤC] %s%n", globalDrift);
                    }
                    if (localDrift != null) {
                        System.out.printf("    [CẢNH BÁO CỤC BỘ]   %s%n", localDrift);
                    }
                    System.out.println();
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("==================================================");
        System.out.println("           KẾT QUẢ THỰC THI END-TO-END            ");
        System.out.println("==================================================");
        System.out.printf("Tổng số giao dịch đã xử lý  : %,d giao dịch%n", totalTransactions);
        System.out.printf("Tổng số checkpoint đã tạo   : %d checkpoints%n", checkpointCount);
        System.out.printf("Thời gian thực thi toàn bộ  : %,d ms (%.2f giây)%n", duration, duration / 1000.0);
        System.out.printf("Tốc độ xử lý luồng stream   : %,.0f giao dịch/giây%n",
                (totalTransactions / Math.max(1.0, duration / 1000.0)));
        System.out.println("==================================================");
    }
}
