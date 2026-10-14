package huddtds.data.pipeline;

import huddtds.model.Element;
import huddtds.model.Transaction;
import huddtds.model.flyweight.ItemFactory;

/**
 * Bộ xử lý định dạng Running Example cổ điển:
 *    item1:quantity1 item2:quantity2 ...
 *    Ví dụ: a:2 c:6 e:2 g:5
 */
public class LegacyFormatHandler implements TransactionLineHandler {
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

        // Định dạng cổ điển không chứa dấu ':' ở định dạng tổng thể? 
        // Actually, он содержит двоеточия в парах item:quantity, но не имеет тройного разделения如SPMF.
        // Мы проверим, что строка не соответствует формату SPMF (то есть не содержит тройного разделения по ':').
        // Однако, проще предположить, что если мы дошли до этого обработчика, то строка не является комментарием или пустой,
        // и не является форматом SPMF (поскольку предыдущие обработчики не смогли обработать).
        // Мы можем просто попытаться разобрать как формат legacy.

        String[] tokens = trimmed.split("\\s+");
        Transaction tx = new Transaction(tid);

        boolean hasValidToken = false;
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            String[] parts = token.split(":", 2);
            if (parts.length != 2) {
                // Token không hợp lệ, bỏ qua token này nhưng tiếp tục xử lý các token khác?
                // Trong định dạng legacy, mỗi token phải có dạng item:quantity
                // Nếu gặp token không hợp lệ, chúng ta có thể bỏ qua toàn bộ dòng hoặc chỉ bỏ qua token này.
                // Theo định dạng, việc gặp token không hợp lệ betekк hela dòng không hợp lệ.
                // Tuy nhiên, để đơn giản, chúng ta sẽ bỏ qua token này và tiếp tục.
                continue;
            }
            String item = parts[0].trim();
            try {
                int quantity = Integer.parseInt(parts[1].trim());
                tx.addElement(item, quantity);
                hasValidToken = true;
            } catch (NumberFormatException e) {
                // Số lượng không hợp lệ, bỏ qua token này
                continue;
            }
        }

        if (!hasValidToken) {
            // Không có token hợp lệ nào, trả về null để chỉ ra rằng dòng không được xử lý
            return null;
        }

        // Đã xử lý thành công, trả về Transaction
        return tx;
    }
}