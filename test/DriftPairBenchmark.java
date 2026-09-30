package test;

import huddtds.algorithm.HUDD_TDS;
import huddtds.data.DatasetManager;
import huddtds.data.TransactionParser;
import huddtds.model.Checkpoint;
import huddtds.model.Transaction;

import java.io.BufferedReader;
import java.util.Map;

/**
 * Giai đoạn 17: Thực nghiệm đối sánh sự trôi dạt tiện ích trên các cặp Dataset:
 * - Chess vs NewChess
 * - Mushrooms vs NewMushroom
 *
 * Ghép nối luồng dữ liệu (Concatenated Stream) để kiểm thử khả năng phát hiện
 * trôi dạt toàn cục (Global Drift) và trôi dạt cục bộ (Local Drift) tại điểm chuyển pha.
 */
public class DriftPairBenchmark {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("   THỰC NGHIỆM ĐỐI SÁNH CẶP DATASET: CHESS & NEWCHESS");
        System.out.println("==================================================\n");

        runConcatenatedStreamTest(
                "Chess",
                "NewChess",
                1000,          // 1,000 tx đầu từ Chess
                1000,          // 1,000 tx tiếp theo từ NewChess
                300,           // windowSize
                100,           // interval
                2_500_000.0,   // minutil
                0.05,          // alpha
                3              // maxItemsetSize
        );

        System.out.println("\n==================================================");
        System.out.println(" THỰC NGHIỆM ĐỐI SÁNH CẶP DATASET: MUSHROOMS & NEWMUSHROOM");
        System.out.println("==================================================\n");

        runConcatenatedStreamTest(
                "Mushrooms",
                "NewMushroom",
                1000,          // 1,000 tx đầu từ Mushrooms
                1000,          // 1,000 tx tiếp theo từ NewMushroom
                300,
                100,
                2_500_000.0,
                0.05,
                3
        );
    }

    private static void runConcatenatedStreamTest(
            String phase1Dataset,
            String phase2Dataset,
            int phase1Count,
            int phase2Count,
            int windowSize,
            int interval,
            double minutil,
            double alpha,
            int maxItemsetSize) throws Exception {

        DatasetManager manager = new DatasetManager();
        Map<String, Double> invTable = manager.loadInvestmentTable(phase1Dataset);

        HUDD_TDS engine = new HUDD_TDS(invTable, minutil, interval, windowSize, alpha, maxItemsetSize);

        System.out.printf("Khởi tạo luồng ghép: Giai đoạn 1 [%s: %,d tx] -> Giai đoạn 2 [%s: %,d tx]%n",
                phase1Dataset, phase1Count, phase2Dataset, phase2Count);
        System.out.printf("Cửa sổ = %d, Chu kỳ = %d, MinUtil = %,.0f, Alpha = %.2f%n",
                windowSize, interval, minutil, alpha);
        System.out.printf("-> Điểm chuyển pha (Transition Point) dự kiến tại TID = %d%n%n", phase1Count);

        int currentTid = 0;
        int globalDriftCount = 0;
        int localDriftCount = 0;

        long start = System.currentTimeMillis();

        // 1. Giai đoạn 1: Stream từ dataset gốc
        try (BufferedReader reader1 = manager.openTransactionStream(phase1Dataset)) {
            String line;
            while (currentTid < phase1Count && (line = reader1.readLine()) != null) {
                currentTid++;
                Transaction tx = TransactionParser.parseLine(line, currentTid);
                Checkpoint cp = engine.processTransaction(tx);
                if (cp != null) {
                    processCheckpoint(engine, cp, currentTid, phase1Count);
                }
            }
        }

        // 2. Giai đoạn 2: Tiếp nối stream từ dataset biến thể New*
        try (BufferedReader reader2 = manager.openTransactionStream(phase2Dataset)) {
            String line;
            int phase2Read = 0;
            while (phase2Read < phase2Count && (line = reader2.readLine()) != null) {
                currentTid++;
                phase2Read++;
                Transaction tx = TransactionParser.parseLine(line, currentTid);
                Checkpoint cp = engine.processTransaction(tx);
                if (cp != null) {
                    processCheckpoint(engine, cp, currentTid, phase1Count);
                }
            }
        }

        long duration = System.currentTimeMillis() - start;
        System.out.printf("%nThời gian hoàn thành: %,d ms (Tốc độ: %,.0f giao dịch/giây)%n",
                duration, (currentTid / Math.max(1.0, duration / 1000.0)));
    }

    private static void processCheckpoint(HUDD_TDS engine, Checkpoint cp, int currentTid, int transitionTid) {
        String phaseMarker = (currentTid <= transitionTid)
                ? String.format("[PHA 1: TID %4d / %4d]", currentTid, transitionTid)
                : String.format("[PHA 2 (DRIFT): TID %4d]", currentTid);

        String globalDrift = engine.checkGlobalDrift();
        String localDrift = engine.checkLocalDrift();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s Checkpoint TID %4d | HUI=%4d | GlobalDist=%9.4f",
                phaseMarker, cp.getTid(), cp.getHuis().size(), cp.getGlobalDistance()));

        if (globalDrift != null) {
            sb.append(" | ").append(globalDrift);
        }
        if (localDrift != null) {
            sb.append(" | ").append(localDrift);
        }

        System.out.println(sb.toString());
    }
}
