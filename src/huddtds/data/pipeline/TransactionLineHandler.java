package huddtds.data.pipeline;

import huddtds.model.Transaction;

/**
 * Định nghĩa contrato cho một bộ xử lý trong chuỗi trách nhiệm phân tích dòng giao dịch.
 * Mỗi bộ xử lý có thể xử lý dòng và chuyển phát petición tiếp theo trong chuỗi nếu không thể xử lý.
 */
public interface TransactionLineHandler {
    /**
     * Thiết lập bộ xử lý tiếp theo trong chuỗi.
     * @param next bộ xử lý tiếp theo
     */
    void setNext(TransactionLineHandler next);

    /**
     * Xử lý một dòng dữ liệu giao dịch.
     *
     * @param line  dòng dữ liệu cần phân tích
     * @param tid   mã định danh giao dịch
     * @return đối tượng Transaction nếu xử lý thành công, hoặc null nếu bộ xử lý không thể xử lý và chuyển tiếp
     */
    Transaction handle(String line, int tid);
}