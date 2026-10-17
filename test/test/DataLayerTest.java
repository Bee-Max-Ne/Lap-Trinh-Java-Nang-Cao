package test;

import huddtds.data.DatasetManager;
import huddtds.data.DatasetValidator;
import huddtds.data.InvestmentLoader;
import huddtds.data.TransactionParser;
import huddtds.model.Transaction;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Kiểm thử tầng dữ liệu (Data Layer Unit Test).
 * Kiểm tra:
 * - TransactionParser (SPMF + Legacy)
 * - InvestmentLoader
 * - DatasetManager
 * - DatasetValidator trên các bộ dữ liệu thực tế
 */
public class DataLayerTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        KHỞI CHẠY KIỂM THỬ TẦNG DỮ LIỆU           ");
        System.out.println("==================================================\n");

        boolean allPassed = true;

        allPassed &= testTransactionParserSPMF();
        allPassed &= testTransactionParserLegacy();
        allPassed &= testInvestmentLoader();
        allPassed &= testDatasetManager();
        allPassed &= testValidationChess();
        allPassed &= testValidationMushrooms();
        allPassed &= testValidationAllDatasetsOverview();

        System.out.println("==================================================");
        if (allPassed) {
            System.out.println(">>> MỐC 1 (DATA LAYER) ĐẠT TIÊU CHÍ NGHIỆM THU! <<<");
        } else {
            System.err.println(">>> CÓ LỖI XẢY RA TRONG TẦNG DỮ LIỆU! <<<");
        }
        System.out.println("==================================================");
    }

    private static boolean testTransactionParserSPMF() {
        System.out.print("[TEST 1] TransactionParser SPMF Format: ");
        String line = "1 3 5 7:100.50:20.25 30.25 10.00 40.00";
        try {
            Transaction tx = TransactionParser.parseLine(line, 1);
            if (tx.getTid() != 1) return fail("TID không khớp");
            if (Math.abs(tx.getTransactionUtility() - 100.50) > 0.001) return fail("TU không khớp");
            if (tx.getElements().size() != 4) return fail("Số phần tử không bằng 4");
            if (!tx.containsItem("3")) return fail("Không chứa item '3'");
            if (Math.abs(tx.getUtility("3") - 30.25) > 0.001) return fail("Utility của item '3' không khớp");

            // Kiểm tra bắt lỗi không khớp số lượng item và utility
            try {
                TransactionParser.parseLine("1 2 3:100:50 50", 2);
                return fail("Không phát hiện lỗi lệch số lượng item và utility");
            } catch (IllegalArgumentException expected) {
                // Mong muốn có ngoại lệ
            }

            System.out.println("PASSED");
            return true;
        } catch (IllegalArgumentException e) {
            return fail("Ngoại lệ: " + e.getMessage());
        }
    }

    private static boolean testTransactionParserLegacy() {
        System.out.print("[TEST 2] TransactionParser Legacy Format: ");
        String line = "a:2 c:6 e:2 g:5";
        try {
            Transaction tx = TransactionParser.parseLine(line, 10);
            if (tx.getTid() != 10) return fail("TID không khớp");
            if (tx.getElements().size() != 4) return fail("Số phần tử không bằng 4");
            if (tx.getQuantity("c") != 6) return fail("Số lượng item 'c' không bằng 6");

            System.out.println("PASSED");
            return true;
        } catch (IllegalArgumentException e) {
            return fail("Ngoại lệ: " + e.getMessage());
        }
    }

    private static boolean testInvestmentLoader() {
        System.out.print("[TEST 3] InvestmentLoader on Chess: ");
        File invFile = new File("data/datasets/Chess/investment_table.txt");
        try {
            Map<String, Double> table = InvestmentLoader.load(invFile);
            if (table.size() != 75) return fail("Số lượng item trong Chess phải là 75, nhận được: " + table.size());
            if (!table.containsKey("1")) return fail("Không chứa item '1'");
            if (Math.abs(table.get("1") - 187318730.87) > 0.01) return fail("Giá trị investment item '1' sai lệch");

            System.out.println("PASSED (75 items loaded)");
            return true;
        } catch (IOException | IllegalArgumentException e) {
            return fail("Ngoại lệ: " + e.getMessage());
        }
    }

    private static boolean testDatasetManager() {
        System.out.print("[TEST 4] DatasetManager auto-scan: ");
        DatasetManager manager = new DatasetManager(new File("data/datasets"));
        List<String> names = manager.getDatasetNames();
        if (names.size() < 9) {
            return fail("Kỳ vọng ít nhất 9 dataset, tìm thấy: " + names.size());
        }
        if (!names.contains("Chess") || !names.contains("Chess-metadata")) {
            return fail("Thiếu dataset Chess hoặc Chess-metadata: " + names);
        }
        System.out.println("PASSED (Đã nhận diện " + names.size() + " datasets, bao gồm cả Chess và Chess-metadata)");
        return true;
    }

    private static boolean testValidationChess() {
        System.out.println("\n[TEST 5] Kiểm định toàn diện Dataset: Chess");
        File chessDir = new File("data/datasets/Chess");
        DatasetValidator.ValidationReport report = DatasetValidator.validate(chessDir);
        report.printSummary();

        if (!report.passed) return fail("Chess kiểm định không đạt");
        if (report.validTransactions != 3196) return fail("Chess phải có 3,196 giao dịch, nhận: " + report.validTransactions);
        if (report.distinctItemsCount != 75) return fail("Chess phải có 75 items, nhận: " + report.distinctItemsCount);
        if (report.invalidTransactions != 0) return fail("Chess có giao dịch lỗi: " + report.invalidTransactions);

        return true;
    }

    private static boolean testValidationMushrooms() {
        System.out.println("[TEST 6] Kiểm định toàn diện Dataset: Mushrooms");
        File mushDir = new File("data/datasets/Mushrooms");
        DatasetValidator.ValidationReport report = DatasetValidator.validate(mushDir);
        report.printSummary();

        if (!report.passed) return fail("Mushrooms kiểm định không đạt");
        if (report.validTransactions != 8416) return fail("Mushrooms phải có 8,416 giao dịch, nhận: " + report.validTransactions);
        if (report.distinctItemsCount != 119) return fail("Mushrooms phải có 119 items, nhận: " + report.distinctItemsCount);

        return true;
    }

    private static boolean testValidationAllDatasetsOverview() {
        System.out.println("[TEST 7] Kiểm định sơ bộ 7 dataset còn lại (mẫu kiểm tra 1,000 giao dịch đầu):");
        DatasetManager manager = new DatasetManager(new File("data/datasets"));
        boolean allValid = true;

        for (String name : manager.getDatasetNames()) {
            if (name.equals("Chess") || name.equals("Mushrooms")) continue;

            File dir = new File("data/datasets", name);
            DatasetValidator.ValidationReport r = DatasetValidator.validate(dir, 1000);
            System.out.printf("  - %-15s: %s (Hợp lệ: %,d/%,d, Items: %d)%n",
                    name, r.passed ? "PASS" : "FAIL", r.validTransactions, r.totalTransactions, r.distinctItemsCount);
            if (!r.passed) {
                allValid = false;
            }
        }
        System.out.println();
        return allValid;
    }

    private static boolean fail(String reason) {
        System.out.println("FAILED: " + reason);
        return false;
    }
}
