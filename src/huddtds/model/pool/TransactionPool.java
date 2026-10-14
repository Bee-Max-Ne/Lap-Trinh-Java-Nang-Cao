package huddtds.model.pool;

import huddtds.model.Transaction;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Lớp TransactionPool quản lý một hồ chứa các đối tượng Transaction để tái sử dụng,
 * giúp giảmภาระ Garbage Collection và tối ưu hiệu năng.
 * <p>
 * Sử dụng {@link ArrayBlockingQueue} với kích thước cố định để lưu trữ các Transaction đã được reset.
 * Khi Pool bị exhausted (hết đối tượng), sẽ tạo mới một Transaction thay vì chờ đợi.
 */
public class TransactionPool {
    /** Kích thước mặc định của pool */
    private static final int DEFAULT_POOL_SIZE = 10000;

    /** Hàng đợi lưu trữ các Transaction có sẵn để tái sử dụng */
    private final BlockingQueue<Transaction> pool;

    /**
     * Khởi tạo TransactionPool với kích thước mặc định.
     */
    public TransactionPool() {
        this(DEFAULT_POOL_SIZE);
    }

    /**
     * Khởi tạo TransactionPool với kích thước cụ thể.
     * @param poolSize kích thước tối đa của pool
     */
    public TransactionPool(int poolSize) {
        this.pool = new ArrayBlockingQueue<>(poolSize);
    }

    /**
     * Lấy một Transaction từ pool. Nếu pool rỗng, tạo mới một Transaction.
     * <p>
     * Lưu ý: Transaction trả về có thể đã được sử dụng trước đó và đã được reset.
     * Người gọi nên đặt lại TID bằng cách gọi {@link Transaction#reset(int)} nếu cần.
     * Tuy nhiên, phương thức này đã tự động đặt TID theo tham số {@code tid}.
     *
     * @param tid mã định danh giao dịch mới
     * @return đối tượng Transaction
     */
    public Transaction acquire(int tid) {
        Transaction tx = pool.poll();
        if (tx == null) {
            // Pool rỗng, tạo mới
            tx = new Transaction(tid);
        } else {
            // Reset transaction và đặt TID mới
            tx.reset(tid);
        }
        return tx;
    }

    /**
     * Giải phóng Transaction trả về pool sau khi reset.
     * <p>
     * Transaction sẽ được gọi {@link Transaction#reset()} để làm sạch nội dung trước khi đưa vào pool.
     * Nếu pool đã đầy, Transaction sẽ bị hủy (để được GC lần sau).
     *
     * @param tx đối tượng Transaction cần giải phóng
     */
    public void release(Transaction tx) {
        if (tx == null) {
            return;
        }
        // Reset transaction để przygotować để tái sử dụng
        tx.reset();
        // Nếu pool chưa đầy, thêm vào pool; nếu đầy, để đối tượng bị hủy
        if (!pool.offer(tx)) {
            // Pool đã đầy, bỏ qua (để được GC)
        }
    }
}