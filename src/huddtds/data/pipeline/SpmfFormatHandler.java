package huddtds.data.pipeline;

import huddtds.model.Element;
import huddtds.model.Transaction;
import huddtds.model.flyweight.ItemFactory;

/**
 * Bộ xử lý định dạng SPMF / HUIM chuẩn:
 *    item1 item2 ... : TU : u1 u2 ...
 *    Ví dụ: 1 3 5 7:11699429.00:75465.00 118984.00 561780.00 32025.00
 * 
 * Этот обработчик предполагает, что Transaction будет получен из пула,
 * а элементы Item будут получены через ItemFlyweight фабрику.
 * Однако, для простоты интеграции с существующим кодом, мы создаем новые элементы
 * и полагаемся на то, что Transaction будет позже передан в пул.
 * 
 * В реальной интеграции, необходимо передать экземпляры TransactionPool и ItemFactory
 * в конструктор обработчика, но для простоты мы будем использовать статические методы
 * или создать экземпляры внутри. Учитывая требование о минимальном изменении существующего API,
 * мы оставим создание элементов как есть, но в будущем можно рефакторить.
 */
public class SpmfFormatHandler implements TransactionLineHandler {
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

        // Kiểm tra xem dòng có định dạng SPMF / HUIM (chứa dấu ':')
        if (!trimmed.contains(":")) {
            // Không phải định dạng SPMF, chuyển tiếp
            if (next != null) {
                return next.handle(line, tid);
            }
            return null;
        }

        String[] parts = trimmed.split(":");
        if (parts.length != 3) {
            // Định dạng không hợp lệ, chuyển tiếp (hoặc có thể ném ngoại lệ tùy vào thiết kế)
            if (next != null) {
                return next.handle(line, tid);
            }
            return null;
        }

        String itemsPart = parts[0].trim();
        String tuPart = parts[1].trim();
        String utilsPart = parts[2].trim();

        if (itemsPart.isEmpty() || tuPart.isEmpty() || utilsPart.isEmpty()) {
            // Định dạng không hợp lệ, chuyển tiếp
            if (next != null) {
                return next.handle(line, tid);
            }
            return null;
        }

        String[] itemTokens = itemsPart.split("\\s+");
        String[] utilTokens = utilsPart.split("\\s+");

        if (itemTokens.length != utilTokens.length) {
            // Lỗi không khớp số lượng, chuyển tiếp
            if (next != null) {
                return next.handle(line, tid);
            }
            return null;
        }

        double tu;
        try {
            tu = Double.parseDouble(tuPart);
        } catch (NumberFormatException e) {
            // Giá trị TU không hợp lệ, chuyển tiếp
            if (next != null) {
                return next.handle(line, tid);
            }
            return null;
        }

        Transaction tx = new Transaction(tid, tu);
        for (int i = 0; i < itemTokens.length; i++) {
            String item = itemTokens[i].trim();
            double utility;
            try {
                utility = Double.parseDouble(utilTokens[i].trim());
            } catch (NumberFormatException e) {
                // Giá trị utility không hợp lệ của item, chuyển tiếp
                if (next != null) {
                    return next.handle(line, tid);
                }
                return null;
            }
            tx.addElement(item, utility);
        }

        // Đã xử lý thành công, trả về Transaction
        return tx;
    }
}