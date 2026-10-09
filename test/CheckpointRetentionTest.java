package test;

import huddtds.algorithm.HUDD_TDS;
import huddtds.algorithm.GlobalDriftDetector;
import huddtds.algorithm.HUIDiscovery;
import huddtds.algorithm.LocalDriftDetector;
import huddtds.model.Checkpoint;
import huddtds.model.Transaction;

import java.util.Collections;

public class CheckpointRetentionTest {
    public static void main(String[] args) {
        HUDD_TDS engine = new HUDD_TDS(
                Collections.<String, Double>emptyMap(),
                1.0,
                1,
                2,
                0.05,
                new HUIDiscovery(Collections.<String, Double>emptyMap(), 1.0, 2, 1),
                new GlobalDriftDetector(0.05, 1.0),
                new LocalDriftDetector(0.05, 100.0, 2));

        for (int tid = 1; tid <= HUDD_TDS.MAX_RETAINED_CHECKPOINTS + 5; tid++) {
            Checkpoint checkpoint = engine.processTransaction(new Transaction(tid));
            require(checkpoint != null, "Expected checkpoint at TID " + tid);
        }

        require(engine.getCheckpoints().size() == HUDD_TDS.MAX_RETAINED_CHECKPOINTS,
                "Engine should retain only the configured recent checkpoint count");
        require(engine.getCheckpoints().get(0).getTid() == 6
                        && engine.getCheckpoints()
                        .get(HUDD_TDS.MAX_RETAINED_CHECKPOINTS - 1).getTid() == 1005,
                "Engine should retain the latest 1,000 checkpoints in order");

        System.out.println("CheckpointRetentionTest PASSED");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
