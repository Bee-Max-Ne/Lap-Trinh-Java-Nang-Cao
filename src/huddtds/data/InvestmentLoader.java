package huddtds.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Bộ nạp bảng đầu tư ngoại vi (InvestmentLoader).
 * Đọc tệp investment_table.txt và lưu trữ dưới dạng Map<String, Double>.
 */
public class InvestmentLoader {

    /**
     * Tải dữ liệu từ tệp investment_table.txt.
     *
     * @param file đối tượng tệp tin
     * @return Map lưu trữ ItemID -> Total Investment
     * @throws IOException nếu xảy ra lỗi đọc tệp
     */
    public static Map<String, Double> load(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Tệp investment_table không tồn tại: " + file.getAbsolutePath());
        }

        Map<String, Double> table = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();

                // Bỏ qua dòng trống, tiêu đề bảng hoặc đường kẻ phân cách
                if (trimmed.isEmpty()
                        || trimmed.startsWith("ItemID")
                        || trimmed.startsWith("Item")
                        || trimmed.startsWith("=")
                        || trimmed.startsWith("#")) {
                    continue;
                }

                // Dòng dữ liệu: ItemID [tab/space] Investment
                String[] parts = trimmed.split("\\s+");
                if (parts.length < 2) {
                    continue;
                }

                String itemId = parts[0].trim();
                try {
                    double investment = Double.parseDouble(parts[1].trim());
                    if (investment < 0.0) {
                        System.err.println(String.format(
                                "Cảnh báo dòng %d: Investment âm (%f) cho item %s",
                                lineNumber, investment, itemId
                        ));
                    }
                    table.put(itemId, investment);
                } catch (NumberFormatException e) {
                    System.err.println(String.format(
                            "Bỏ qua dòng %d (giá trị không hợp lệ): %s",
                            lineNumber, trimmed
                    ));
                }
            }
        }

        return table;
    }

    /**
     * Tải dữ liệu từ đường dẫn tệp.
     *
     * @param filePath đường dẫn tệp tin
     * @return Map lưu trữ ItemID -> Total Investment
     * @throws IOException nếu lỗi
     */
    public static Map<String, Double> load(String filePath) throws IOException {
        return load(new File(filePath));
    }
}
