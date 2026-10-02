package huddtds.algorithm.drift;

import huddtds.model.DriftResult;

import java.util.function.Consumer;

/**
 * Stateful strategy for evaluating global-distance observations.
 */
public interface GlobalDriftStrategy {
    DriftResult updateAndCheck(double observation, int oldCheckpointTid, int newCheckpointTid);

    void setTraceListener(Consumer<String> traceListener);
}
