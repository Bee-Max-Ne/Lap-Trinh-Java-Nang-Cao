package huddtds.algorithm.mining;

import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import java.util.List;
import java.util.function.Consumer;

/**
 * Strategy contract for mining high-utility itemsets from retained transactions.
 */
public interface HUIItemsetMiner {
    List<HighUtilityItemset> discover(List<Transaction> memory, int currentTid);

    void setTraceListener(Consumer<String> traceListener);
}
