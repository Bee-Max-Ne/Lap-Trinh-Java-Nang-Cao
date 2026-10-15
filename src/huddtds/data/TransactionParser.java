package huddtds.data;

import huddtds.data.pipeline.EmptyAndCommentFilterHandler;
import huddtds.data.pipeline.LegacyFormatHandler;
import huddtds.data.pipeline.SpmfFormatHandler;
import huddtds.data.pipeline.TransactionLineHandler;
import huddtds.data.pipeline.TransactionParsingPipeline;
import huddtds.model.Transaction;

/**
 * Bộ phân tích cú pháp Transaction (TransactionParser).
 * Hỗ trợ hai định dạng:
 * 1. Định dạng SPMF / HUIM chuẩn:
 *    item1 item2 ... : TU : u1 u2 ...
 *    Ví dụ: 1 3 5 7:11699429.00:75465.00 118984.00 561780.00 32025.00
 *
 * 2. Định dạng Running Example cổ điển:
 *    item1:quantity1 item2:quantity2 ...
 *    Ví dụ: a:2 c:6 e:2 g:5
 * 
 * Đã được tích hợp Chain of Responsibility pattern thông qua TransactionParsingPipeline
 * đồng thời giữ nguyên 100% các phương thức parse cổ điển nội bộ để bảo toàn cấu trúc mã nguồn.
 */
public class TransactionParser {

    /**
     * Pipeline xử lý chuỗi trách nhiệm để phân tích dòng giao dịch.
     * Các bộ xử lý được sắp xếp theo thứ tự:
     *   1. EmptyAndCommentFilterHandler - loại bỏ dòng trống và dòng chú thích
     *   2. SpmfFormatHandler - xử lý định dạng SPMF/HUIM
     *   3. LegacyFormatHandler - xử lý định dạng cũ (item:quantity)
     */
    private static final TransactionParsingPipeline PIPELINE;

    static {
        EmptyAndCommentFilterHandler filterHandler = new EmptyAndCommentFilterHandler();
        SpmfFormatHandler spmfHandler = new SpmfFormatHandler();
        LegacyFormatHandler legacyHandler = new LegacyFormatHandler();
        
        // Xây dựng chuỗi: filter -> spmf -> legacy
        filterHandler.setNext(spmfHandler);
        spmfHandler.setNext(legacyHandler);
        
        PIPELINE = new TransactionParsingPipeline(filterHandler);
    }

    /**
     * Phân tích một dòng văn bản thành đối tượng Transaction.
     * Ưu tiên xử lý qua Chain of Responsibility Pipeline, nếu không khớp sẽ chuyển sang phân tích fallback cổ điển.
     *
     * @param line chuỗi dòng dữ liệu
     * @param tid mã định danh giao dịch
     * @return Transaction đã được phân tích
     * @throws IllegalArgumentException nếu dòng dữ liệu không hợp lệ
     */
    public static Transaction parseLine(String line, int tid) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("Dòng giao dịch rỗng tại TID=" + tid);
        }

        Transaction tx = PIPELINE.parse(line, tid);
        if (tx != null) {
            return tx;
        }

        String trimmed = line.trim();

        // Kiểm tra xem dòng có định dạng SPMF / HUIM (chứa dấu ':')
        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":");
            if (parts.length == 3) {
                return parseSpmfFormat(parts, tid);
            }
        }

        // Định dạng cổ điển: a:1 c:6 e:2
        return parseLegacyFormat(trimmed, tid);
    }

    /**
     * Phương thức phân tích định dạng SPMF cổ điển được giữ nguyên vẹn.
     */
    public static Transaction parseSpmfFormat(String[] parts, int tid) {
        String itemsPart = parts[0].trim();
        String tuPart = parts[1].trim();
        String utilsPart = parts[2].trim();

        if (itemsPart.isEmpty() || tuPart.isEmpty() || utilsPart.isEmpty()) {
            throw new IllegalArgumentException("Lỗi định dạng SPMF (thiếu thành phần) tại TID=" + tid);
        }

        String[] itemTokens = itemsPart.split("\\s+");
        String[] utilTokens = utilsPart.split("\\s+");

        if (itemTokens.length != utilTokens.length) {
            throw new IllegalArgumentException(String.format(
                    "Lỗi không khớp số lượng tại TID=%d: số item (%d) != số utility (%d)",
                    tid, itemTokens.length, utilTokens.length
            ));
        }

        double tu;
        try {
            tu = Double.parseDouble(tuPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá trị TU không hợp lệ tại TID=" + tid + ": " + tuPart, e);
        }

        Transaction tx = new Transaction(tid, tu);
        for (int i = 0; i < itemTokens.length; i++) {
            String item = itemTokens[i].trim();
            double utility;
            try {
                utility = Double.parseDouble(utilTokens[i].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format(
                        "Giá trị utility không hợp lệ của item '%s' tại TID=%d: %s",
                        item, tid, utilTokens[i]
                ), e);
            }
            tx.addElement(item, utility);
        }

        return tx;
    }

    /**
     * Phương thức phân tích định dạng Running Example cổ điển được giữ nguyên vẹn.
     */
    public static Transaction parseLegacyFormat(String line, int tid) {
        Transaction tx = new Transaction(tid);
        String[] tokens = line.split("\\s+");

        for (String token : tokens) {
            if (token.isEmpty()) continue;
            String[] parts = token.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Token không hợp lệ tại TID=" + tid + ": " + token);
            }
            String item = parts[0].trim();
            try {
                int quantity = Integer.parseInt(parts[1].trim());
                tx.addElement(item, quantity);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Số lượng không hợp lệ tại TID=" + tid + ": " + parts[1], e);
            }
        }

        if (tx.getElements().isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy item hợp lệ tại TID=" + tid);
        }

        return tx;
    }
}