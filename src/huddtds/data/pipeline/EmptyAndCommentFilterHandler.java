package huddtds.data.pipeline;

import huddtds.model.Transaction;

/**
 * Bộ xử lý đầu tiên trong chuỗi: loại bỏ dòng trống và dòng bắt đầu bằng ký hiệu注释.
 * Loại bỏ:
 *   - Dòng trống (chỉ chứa khoảng trắng)
 *   - Dòng bắt đầu bằng '#' (comment SPMF)
 *   - Dòng bắt strani bằng '//' (comment kiểu Java)
 */
public class EmptyAndCommentFilterHandler implements TransactionLineHandler {
    private TransactionLineHandler next;

    @Override
    public void setNext(TransactionLineHandler next) {
        this.next = next;
    }

    @Override
    public Transaction handle(String line, int tid) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            // Bỏ qua dòng trống
            return null;
        }
        if (trimmed.startsWith("#") || trimmed.startsWith("//")) {
            // Bỏ qua dòng注释
            return null;
        }
        // Chuyển tiếp để bộ xử lý tiếp theo cố gắng xử lý
        if (next != null) {
            return next.handle(line, tid);
        }
        return null;
    }
}