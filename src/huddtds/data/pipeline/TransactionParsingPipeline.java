package huddtds.data.pipeline;

import huddtds.model.Transaction;

/**
 * Lớp TransactionParsingPipeline triển khai mẫu Chain of Responsibility
 * để phân tích dòng dữ liệu giao dịch từ nhiều định dạng khác nhau.
 * 
 * Pipeline cho phép liên kết các handler lại với nhau và cung cấp phương thức
 * parse(String line, int tid) để xử lý dòng dữ liệu.
 */
public class TransactionParsingPipeline {
    /** Đầu tiên trong chuỗi xử lý */
    private TransactionLineHandler firstHandler;

    /**
     * Khởi tạo pipeline với một danh sách các handler sẽ được liên kết thành chuỗi
     * theo thứ tự cung cấp.
     * 
     * @param handlers các handler theo thứ tự xử lý
     */
    @SafeVarargs
    public TransactionParsingPipeline(TransactionLineHandler... handlers) {
        if (handlers == null || handlers.length == 0) {
            this.firstHandler = null;
            return;
        }
        // Liên kết các handler thành chuỗi
        for (int i = 0; i < handlers.length - 1; i++) {
            handlers[i].setNext(handlers[i + 1]);
        }
        // handler cuối cùng tự động chỉ向 null (mặc định)
        this.firstHandler = handlers[0];
    }

    /**
     * Phân tích một dòng dữ liệu giao dịch bằng cách truyền qua chuỗi xử lý.
     *
     * @param line  dòng dữ liệu cần phân tích
     * @param tid   mã định danh giao dịch
     * @return Transaction nếu thành công, null nếu không thể phân tích
     */
    public Transaction parse(String line, int tid) {
        if (firstHandler == null) {
            return null;
        }
        return firstHandler.handle(line, tid);
    }
}