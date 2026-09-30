package huddtds.data;

import huddtds.model.Element;
import huddtds.model.Transaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Bộ kiểm tra tính toàn vẹn và hợp lệ của Dataset (DatasetValidator).
 */
public class DatasetValidator {

    public static class ValidationReport {
        public String datasetName;
        public int totalTransactions = 0;
        public int validTransactions = 0;
        public int invalidTransactions = 0;
        public int distinctItemsCount = 0;
        public double totalDatasetUtility = 0.0;
        public int investmentItemsCount = 0;
        public int missingInvestmentCount = 0;
        public boolean passed = false;
        public List<String> errors = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        public void printSummary() {
            System.out.println("==================================================");
            System.out.println("            BÁO CÁO KIỂM ĐỊNH DATASET             ");
            System.out.println("==================================================");
            System.out.printf("Tên Dataset          : %s%n", datasetName);
            System.out.printf("Tổng số giao dịch    : %,d%n", totalTransactions);
            System.out.printf("Giao dịch hợp lệ     : %,d%n", validTransactions);
            System.out.printf("Giao dịch lỗi        : %,d%n", invalidTransactions);
            System.out.printf("Số item trong stream : %,d%n", distinctItemsCount);
            System.out.printf("Số item trong đầu tư : %,d%n", investmentItemsCount);
            System.out.printf("Item thiếu đầu tư    : %,d%n", missingInvestmentCount);
            System.out.printf("Tổng utility dataset : %,.2f%n", totalDatasetUtility);
            System.out.println("--------------------------------------------------");
            System.out.printf("KẾT LUẬN             : %s%n", passed ? "✓ HỢP LỆ (VALID)" : "✗ KHÔNG HỢP LỆ (INVALID)");
            if (!errors.isEmpty()) {
                System.out.println("Chi tiết lỗi (tối đa 5 lỗi đầu tiên):");
                for (int i = 0; i < Math.min(5, errors.size()); i++) {
                    System.out.println("  [LỖI] " + errors.get(i));
                }
            }
            if (!warnings.isEmpty()) {
                System.out.println("Cảnh báo (tối đa 3 cảnh báo):");
                for (int i = 0; i < Math.min(3, warnings.size()); i++) {
                    System.out.println("  [CẢNH BÁO] " + warnings.get(i));
                }
            }
            System.out.println("==================================================\n");
        }
    }

    /**
     * Kiểm định một dataset từ thư mục của nó.
     *
     * @param datasetDir thư mục chứa transactions.txt và investment_table.txt
     * @param maxCheckLines số dòng tối đa để kiểm tra sâu (0 = kiểm tra toàn bộ)
     * @return ValidationReport
     */
    public static ValidationReport validate(File datasetDir, int maxCheckLines) {
        ValidationReport report = new ValidationReport();
        report.datasetName = datasetDir.getName();

        File transFile = new File(datasetDir, "transactions.txt");
        File invFile = new File(datasetDir, "investment_table.txt");

        if (!transFile.exists() || !transFile.isFile()) {
            report.errors.add("Không tìm thấy tệp transactions.txt tại: " + transFile.getAbsolutePath());
            report.passed = false;
            return report;
        }

        if (!invFile.exists() || !invFile.isFile()) {
            report.warnings.add("Không tìm thấy tệp investment_table.txt tại: " + invFile.getAbsolutePath());
        }

        // 1. Kiểm tra investment_table.txt
        Map<String, Double> investments = new HashMap<>();
        if (invFile.exists()) {
            try {
                investments = InvestmentLoader.load(invFile);
                report.investmentItemsCount = investments.size();
            } catch (IOException e) {
                report.errors.add("Lỗi khi đọc investment_table.txt: " + e.getMessage());
            }
        }

        // 2. Kiểm tra transactions.txt
        Set<String> distinctItems = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(transFile))) {
            String line;
            int tid = 0;

            while ((line = reader.readLine()) != null) {
                tid++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                report.totalTransactions++;

                try {
                    Transaction tx = TransactionParser.parseLine(line, tid);
                    report.validTransactions++;
                    report.totalDatasetUtility += tx.getTransactionUtility();

                    double sumItemUtils = 0.0;
                    for (Element el : tx.getElements()) {
                        distinctItems.add(el.getItem());
                        sumItemUtils += el.getUtility();
                    }

                    // Kiểm tra TU == sum(u)
                    if (tx.getTransactionUtility() > 0.0) {
                        double diff = Math.abs(tx.getTransactionUtility() - sumItemUtils);
                        if (diff > 0.05) { // cho phép sai số làm tròn 0.05
                            if (report.warnings.size() < 10) {
                                report.warnings.add(String.format(
                                        "TID %d: TU trong file (%.2f) lệch với tổng utility item (%.2f)",
                                        tid, tx.getTransactionUtility(), sumItemUtils
                                ));
                            }
                        }
                    }

                } catch (Exception ex) {
                    report.invalidTransactions++;
                    if (report.errors.size() < 10) {
                        report.errors.add(String.format("TID %d: %s", tid, ex.getMessage()));
                    }
                }

                if (maxCheckLines > 0 && tid >= maxCheckLines) {
                    report.warnings.add("Đã dừng kiểm định nhanh sau " + maxCheckLines + " giao dịch.");
                    break;
                }
            }
        } catch (IOException e) {
            report.errors.add("Lỗi I/O khi đọc transactions.txt: " + e.getMessage());
        }

        report.distinctItemsCount = distinctItems.size();

        // Kiểm tra item xuất hiện trong stream nhưng thiếu trong investment_table
        if (!investments.isEmpty()) {
            for (String item : distinctItems) {
                if (!investments.containsKey(item)) {
                    report.missingInvestmentCount++;
                }
            }
        }

        report.passed = (report.invalidTransactions == 0) && (report.validTransactions > 0);
        return report;
    }

    public static ValidationReport validate(File datasetDir) {
        return validate(datasetDir, 0);
    }
}
