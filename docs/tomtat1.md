# CHI TIẾT THAY ĐỔI CODE CHO 4 MẪU THIẾT KẾ MỚI

## 1. Flyweight Pattern
### Files newly created
- src/huddtds/model/flyweight/ItemFlyweight.java
- src/huddtds/model/flyweight/ItemFactory.java
### Files modified (additions only)
- src/huddtds/model/Element.java
  - Added import: import huddtds.model.flyweight.ItemFlyweight; import huddtds.model.flyweight.ItemFactory;
  - Added private field: private final ItemFlyweight flyweight;
  - Modified constructors to delegate to ItemFactory.getOrCreate(item, ...)
  - Added private constructor: private Element(ItemFlyweight flyweight, int quantity, double utility)
  - Added method: public ItemFlyweight getFlyweight()
  - Retained original getters (getItem, getQuantity, getUtility) unchanged
  - Updated 	oString() to use flyweight
- src/huddtds/model/Transaction.java
  - In calculateTotalUtility(Map<String,Double> externalUtilities), added fallback to use element.getFlyweight().getExternalUtility() when externalUtilities is null or empty

## 2. Object Pool Pattern
### Files newly created
- src/huddtds/model/pool/TransactionPool.java
### Files modified
- src/huddtds/model/Transaction.java
  - Changed field 	id from private final int tid; to private int tid;
  - Added method public void reset() (clears elements, elementMap, transactionUtility)
  - Added method public void reset(int newTid) (sets tid then calls reset())
  - Added setter public void setTid(int newTid) (used internally)
- src/huddtds/algorithm/HUDD_TDS.java
  - Added import: import huddtds.model.pool.TransactionPool;
  - Added private field: private final TransactionPool transactionPool;
  - Initialized in each constructor: 	his.transactionPool = new TransactionPool();
  - In processTransaction(Transaction tx), when removing old transactions from memory, added 	ransactionPool.release(removedTx);

## 3. Template Method Pattern
### Files newly created
- src/huddtds/algorithm/drift/AbstractDriftDetector.java
### Files modified
- src/huddtds/algorithm/GlobalDriftDetector.java
  - Changed inheritance: public class GlobalDriftDetector extends AbstractDriftDetector implements GlobalDriftStrategy
  - Added inner class public static class GlobalDriftInput implements AbstractDriftDetector.DriftInput { ... }
  - Replaced updateAndCheck(double observation, int oldCheckpointTid, int newCheckpointTid) to create input and call unDetection(input)
  - Implemented the six abstract methods:
    - protected boolean hasEnoughData(AbstractDriftDetector.DriftInput input) { return observationCount >= 2; }
    - protected double computeStatistic(AbstractDriftDetector.DriftInput input) { return 0.0; } (placeholder)
    - protected double computeEpsilon(AbstractDriftDetector.DriftInput input) { return 1.0; } (placeholder)
    - protected void traceResult(AbstractDriftDetector.DriftInput input, double statistic, double epsilon, boolean detected) { /* logs */ }
    - protected DriftResult buildDetectedResult(AbstractDriftDetector.DriftInput input, double statistic, double epsilon) { /* builds result */ }
    - protected DriftResult buildNoDriftResult(AbstractDriftDetector.DriftInput input) { /* builds no drift */ }
  - Retained other public methods for backward compatibility (updateAndCheck(double), getters, reset)
- src/huddtds/algorithm/LocalDriftDetector.java
  - Similar changes: extends AbstractDriftDetector, adds LocalDriftInput, replaces detect(Checkpoint previous, Checkpoint current) to delegate to runDetection, implements six abstract methods, retains public API.

## 4. Chain of Responsibility Pattern
### Files newly created
- src/huddtds/data/pipeline/TransactionLineHandler.java
- src/huddtds/data/pipeline/EmptyAndCommentFilterHandler.java
- src/huddtds/data/pipeline/SpmfFormatHandler.java
- src/huddtds/data/pipeline/LegacyFormatHandler.java
- src/huddtds/data/pipeline/TransactionParsingPipeline.java
### Files modified
- src/huddtds/data/TransactionParser.java
  - Added imports for the pipeline classes and handler interface
  - Added static field and initializer:
    `java
    private static final TransactionParsingPipeline PIPELINE;
    static {
        EmptyAndCommentFilterHandler filterHandler = new EmptyAndCommentFilterHandler();
        SpmfFormatHandler spmfHandler = new SpmfFormatHandler();
        LegacyFormatHandler legacyHandler = new LegacyFormatHandler();
        filterHandler.setNext(spmfHandler);
        spmfHandler.setNext(legacyHandler);
        PIPELINE = new TransactionParsingPipeline(filterHandler);
    }
    `
  - Replaced body of parseLine(String line, int tid) to use PIPELINE.parse(line, tid) and handle null result with exception.
  - Kept existing private helper methods (parseSpmfFormat, parseLegacyFormat) unchanged.
