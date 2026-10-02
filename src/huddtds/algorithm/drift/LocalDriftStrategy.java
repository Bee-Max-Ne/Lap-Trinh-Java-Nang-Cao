package huddtds.algorithm.drift;

import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;

import java.util.function.Consumer;

/**
 * Strategy for comparing the utility itemsets in two checkpoints.
 */
public interface LocalDriftStrategy {
    DriftResult detect(Checkpoint previous, Checkpoint current);

    void setTraceListener(Consumer<String> traceListener);
}
