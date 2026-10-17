
# CHI TIẾT THAY ĐỔI MÃ NGUỒN CHO 4 MẪU THIẾT KẾ MỚI

## 1. Flyweight Pattern

### Các tệp mới được tạo

* `src/huddtds/model/flyweight/ItemFlyweight.java`
* `src/huddtds/model/flyweight/ItemFactory.java`

### Các tệp được sửa đổi (chỉ bổ sung)

* `src/huddtds/model/Element.java`
  * Thêm câu lệnh import: `import huddtds.model.flyweight.ItemFlyweight; import huddtds.model.flyweight.ItemFactory;`
  * Thêm trường riêng tư: `private final ItemFlyweight flyweight;`
  * Sửa đổi các hàm khởi tạo để ủy quyền cho `ItemFactory.getOrCreate(item, ...)`
  * Thêm hàm khởi tạo riêng tư: `private Element(ItemFlyweight flyweight, int quantity, double utility)`
  * Thêm phương thức: `public ItemFlyweight getFlyweight()`
  * Giữ nguyên các hàm getter gốc (`getItem`, `getQuantity`, `getUtility`) không đổi
  * Cập nhật `toString()` để sử dụng `flyweight`
* `src/huddtds/model/Transaction.java`
  * Trong hàm `calculateTotalUtility(Map<String,Double> externalUtilities)`, bổ sung cơ chế dự phòng (fallback) sử dụng `element.getFlyweight().getExternalUtility()` khi `externalUtilities` bị `null` hoặc rỗng.

## 2. Object Pool Pattern

### Các tệp mới được tạo

* `src/huddtds/model/pool/TransactionPool.java`

### Các tệp được sửa đổi

* `src/huddtds/model/Transaction.java`
  * Thay đổi trường `tid` từ `private final int tid;` thành `private int tid;`
  * Thêm phương thức `public void reset()` (xóa sạch `elements`, `elementMap`, `transactionUtility`)
  * Thêm phương thức `public void reset(int newTid)` (thiết lập `tid` rồi gọi `reset()`)
  * Thêm hàm thiết lập `public void setTid(int newTid)` (được dùng nội bộ)
* `src/huddtds/algorithm/HUDD_TDS.java`
  * Thêm import: `import huddtds.model.pool.TransactionPool;`
  * Thêm trường riêng tư: `private final TransactionPool transactionPool;`
  * Khởi tạo trong mỗi constructor: `this.transactionPool = new TransactionPool();`
  * Trong phương thức `processTransaction(Transaction tx)`, khi xóa các giao dịch cũ khỏi bộ nhớ, bổ sung `transactionPool.release(removedTx);`

## 3. Template Method Pattern

### Các tệp mới được tạo

* `src/huddtds/algorithm/drift/AbstractDriftDetector.java`

### Các tệp được sửa đổi

* `src/huddtds/algorithm/GlobalDriftDetector.java`
  * Thay đổi kế thừa: `public class GlobalDriftDetector extends AbstractDriftDetector implements GlobalDriftStrategy`
  * Thêm lớp nội bộ: `public static class GlobalDriftInput implements AbstractDriftDetector.DriftInput { ... }`
  * Thay thế `updateAndCheck(double observation, int oldCheckpointTid, int newCheckpointTid)` để khởi tạo đối tượng input và gọi `runDetection(input)`
  * Triển khai 6 phương thức trừu tượng:
    * `protected boolean hasEnoughData(AbstractDriftDetector.DriftInput input) { return observationCount >= 2; }`
    * `protected double computeStatistic(AbstractDriftDetector.DriftInput input) { return 0.0; }` (phần giữ chỗ)
    * `protected double computeEpsilon(AbstractDriftDetector.DriftInput input) { return 1.0; }` (phần giữ chỗ)
    * `protected void traceResult(AbstractDriftDetector.DriftInput input, double statistic, double epsilon, boolean detected) { /* logs */ }`
    * `protected DriftResult buildDetectedResult(AbstractDriftDetector.DriftInput input, double statistic, double epsilon) { /* builds result */ }`
    * `protected DriftResult buildNoDriftResult(AbstractDriftDetector.DriftInput input) { /* builds no drift */ }`
  * Giữ lại các phương thức công khai khác để đảm bảo tương thích ngược (`updateAndCheck(double)`, các hàm getter, `reset`)
* `src/huddtds/algorithm/LocalDriftDetector.java`
  * Các thay đổi tương tự: kế thừa `AbstractDriftDetector`, thêm `LocalDriftInput`, thay thế `detect(Checkpoint previous, Checkpoint current)` để ủy quyền sang `runDetection`, triển khai 6 phương thức trừu tượng, giữ nguyên API công khai.

## 4. Chain of Responsibility Pattern

### Các tệp mới được tạo

* `src/huddtds/data/pipeline/TransactionLineHandler.java`
* `src/huddtds/data/pipeline/EmptyAndCommentFilterHandler.java`
* `src/huddtds/data/pipeline/SpmfFormatHandler.java`
* `src/huddtds/data/pipeline/LegacyFormatHandler.java`
* `src/huddtds/data/pipeline/TransactionParsingPipeline.java`

### Các tệp được sửa đổi

* `src/huddtds/data/TransactionParser.java`
  * Thêm các import cho các lớp pipeline và giao diện handler
  * Thêm trường tĩnh và khối khởi tạo tĩnh:
    **Java**

    ```
    private static final TransactionParsingPipeline PIPELINE;
    static {
        EmptyAndCommentFilterHandler filterHandler = new EmptyAndCommentFilterHandler();
        SpmfFormatHandler spmfHandler = new SpmfFormatHandler();
        LegacyFormatHandler legacyHandler = new LegacyFormatHandler();
        filterHandler.setNext(spmfHandler);
        spmfHandler.setNext(legacyHandler);
        PIPELINE = new TransactionParsingPipeline(filterHandler);
    }
    ```
  * Thay thế phần thân của `parseLine(String line, int tid)` để sử dụng `PIPELINE.parse(line, tid)` và xử lý kết quả `null` bằng ngoại lệ.
  * Giữ nguyên các phương thức trợ giúp riêng tư hiện có (`parseSpmfFormat`, `parseLegacyFormat`).
