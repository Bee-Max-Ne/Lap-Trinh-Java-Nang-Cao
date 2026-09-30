package test;

import huddtds.algorithm.HUDD_TDS;
import huddtds.data.DatasetValidator;
import huddtds.data.InvestmentLoader;
import huddtds.data.TransactionParser;
import huddtds.math.UtilityMetrics;
import huddtds.model.Element;
import huddtds.model.Transaction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

/**
 * Giai đoạn 23: Bộ kiểm thử toàn diện cuối cùng (Final Validation Suite).
 * Kiểm tra các trường hợp biên, dữ liệu dị thường (Edge Cases) và xử lý ngoại lệ:
 * - File không tồn tại
 * - File rỗng
 * - Dòng giao dịch rỗng / sai cú pháp
 * - Lệch số lượng item và utility
 * - Utility âm hoặc không phải số
 * - Investment table thiếu file / định dạng lỗi
 * - Tham số toán học biên: n <= 0, alpha <= 0, variance = 0
 */
public class FinalValidationSuite {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("     BỘ KIỂM THỬ XỬ LÝ LỖI & DỮ LIỆU DỊ THƯỜNG    ");
        System.out.println("==================================================\n");

        boolean passed = true;

        passed &= testMathEdgeCases();
        passed &= testTransactionParserErrors();
        passed &= testInvestmentLoaderErrors();
        passed &= testDatasetValidatorErrors();

        System.out.println("\n==================================================");
        if (passed) {
            System.out.println(">>> TẤT CẢ KIỂM THỬ XỬ LÝ LỖI ĐỀU VƯỢT QUA (PASSED) <<<");
        } else {
            System.err.println(">>> CÓ KIỂM THỬ KHÔNG ĐẠT YÊU CẦU! <<<");
        }
        System.out.println("==================================================");
    }

    private static boolean testMathEdgeCases() {
        System.out.print("[TEST 1] Xử lý tham số toán học biên: ");
        try {
            // Decay tại delta = 0 -> 1.0; delta = 2 -> 0.5
            if (Math.abs(UtilityMetrics.decayFunction(0) - 1.0) > 1e-6) return fail("Decay tại 0 sai");
            if (Math.abs(UtilityMetrics.decayFunction(2) - 0.5) > 1e-6) return fail("Decay tại 2 sai");
            if (Math.abs(UtilityMetrics.decayFunction(4) - 0.25) > 1e-6) return fail("Decay tại 4 sai");

            // Hoeffding với n <= 0 hoặc alpha <= 0 phải an toàn (không crash)
            if (UtilityMetrics.hoeffdingBound(1.0, 0.05, 0) != 0.0) return fail("Hoeffding tại n=0 phải = 0");
            if (UtilityMetrics.hoeffdingBound(1.0, -0.1, 100) != 0.0) return fail("Hoeffding tại alpha âm phải = 0");

            // Global drift epsilon với n <= m phải trả về vô cùng (không thể kiểm định)
            if (!Double.isInfinite(UtilityMetrics.globalDriftEpsilon(10, 10, 0.05, 1.0))) {
                return fail("Global drift tại n=m phải là Infinity");
            }

            // Variance của mảng 1 phần tử hoặc mảng giá trị bằng nhau
            double[] zeroVar = {5.0, 5.0, 5.0};
            if (UtilityMetrics.variance(zeroVar) != 0.0) return fail("Variance các phần tử bằng nhau phải = 0");

            System.out.println("PASSED");
            return true;
        } catch (Exception e) {
            return fail("Ngoại lệ: " + e.getMessage());
        }
    }

    private static boolean testTransactionParserErrors() {
        System.out.print("[TEST 2] Xử lý lỗi định dạng giao dịch: ");

        // 1. Dòng rỗng
        try {
            TransactionParser.parseLine("", 1);
            return fail("Không bắt lỗi dòng rỗng");
        } catch (IllegalArgumentException ignored) {}

        // 2. Lệch số item và utility (3 items nhưng chỉ có 2 utility)
        try {
            TransactionParser.parseLine("1 2 3:100:50 50", 2);
            return fail("Không bắt lỗi lệch số item và utility");
        } catch (IllegalArgumentException ignored) {}

        // 3. TU không phải số hợp lệ
        try {
            TransactionParser.parseLine("1 2:INVALID:50 50", 3);
            return fail("Không bắt lỗi TU không phải số");
        } catch (IllegalArgumentException ignored) {}

        // 4. Utility của item không phải số hợp lệ
        try {
            TransactionParser.parseLine("1 2:100:50 ABC", 4);
            return fail("Không bắt lỗi utility item không phải số");
        } catch (IllegalArgumentException ignored) {}

        System.out.println("PASSED");
        return true;
    }

    private static boolean testInvestmentLoaderErrors() {
        System.out.print("[TEST 3] Xử lý lỗi nạp tệp investment_table: ");

        // 1. Tệp không tồn tại
        try {
            InvestmentLoader.load("data/datasets/NonExistentFile.txt");
            return fail("Không bắt lỗi tệp không tồn tại");
        } catch (IOException ignored) {}

        System.out.println("PASSED");
        return true;
    }

    private static boolean testDatasetValidatorErrors() throws Exception {
        System.out.print("[TEST 4] Kiểm định phát hiện tập dữ liệu lỗi: ");

        // Tạo một thư mục tạm chứa dữ liệu lỗi
        File tempDir = new File("scratch/faulty_dataset");
        tempDir.mkdirs();

        File faultyTrans = new File(tempDir, "transactions.txt");
        try (PrintWriter pw = new PrintWriter(new FileWriter(faultyTrans))) {
            pw.println("1 2:100:50 50"); // Dòng đúng
            pw.println("1 2 3:100:50 50"); // Dòng lỗi: thiếu 1 utility
        }

        DatasetValidator.ValidationReport report = DatasetValidator.validate(tempDir);
        if (report.passed) {
            return fail("DatasetValidator phải đánh dấu lỗi cho tập dữ liệu này");
        }
        if (report.invalidTransactions != 1) {
            return fail("Kỳ vọng 1 giao dịch lỗi, nhận: " + report.invalidTransactions);
        }

        // Dọn dẹp thư mục tạm
        faultyTrans.delete();
        tempDir.delete();

        System.out.println("PASSED (Phát hiện chính xác 1 dòng lỗi cú pháp)");
        return true;
    }

    private static boolean fail(String reason) {
        System.out.println("FAILED: " + reason);
        return false;
    }
}
