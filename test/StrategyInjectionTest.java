package test;

import huddtds.algorithm.HUDD_TDS;
import huddtds.algorithm.drift.GlobalDriftStrategy;
import huddtds.algorithm.drift.LocalDriftStrategy;
import huddtds.algorithm.mining.HUIItemsetMiner;
import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;
import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * Focused smoke test for injected algorithm strategies and legacy string APIs.
 */
public class StrategyInjectionTest {
    public static void main(String[] args) {
        CountingMiner miner = new CountingMiner();
        FixedGlobalStrategy global = new FixedGlobalStrategy();
        FixedLocalStrategy local = new FixedLocalStrategy();
        HUDD_TDS engine = new HUDD_TDS(
                Collections.<String, Double>emptyMap(), 1.0, 1, 2, 0.05, miner, global, local);

        engine.processTransaction(new Transaction(1));
        Checkpoint second = engine.processTransaction(new Transaction(2));

        require(second != null, "Expected a checkpoint at TID 2");
        require(miner.calls == 2, "Injected HUI miner should receive both checkpoints");
        require(engine.checkGlobalDrift().equals("GLOBAL DRIFT (TĂNG) [Δ=1.0000]"),
                "Legacy global drift text should be preserved");
        require(engine.checkLocalDrift().equals("LOCAL DRIFT (itemset)"),
                "Legacy local drift text should be preserved");
        require(global.calls == 1, "Injected global strategy should be invoked");
        require(local.calls == 1, "Injected local strategy should be invoked");

        CountingMiner typedMiner = new CountingMiner();
        FixedGlobalStrategy typedGlobal = new FixedGlobalStrategy();
        FixedLocalStrategy typedLocal = new FixedLocalStrategy();
        HUDD_TDS typedEngine = new HUDD_TDS(
                Collections.<String, Double>emptyMap(), 1.0, 1, 2, 0.05,
                typedMiner, typedGlobal, typedLocal);
        typedEngine.processTransaction(new Transaction(1));
        typedEngine.processTransaction(new Transaction(2));

        DriftResult globalResult = typedEngine.checkGlobalDriftResult();
        DriftResult localResult = typedEngine.checkLocalDriftResult();
        require(globalResult.getType() == DriftResult.DriftType.GLOBAL_DRIFT,
                "Typed global API should preserve the drift type");
        require(globalResult.getStatistic() == 0.5 && globalResult.getThreshold() == 0.25,
                "Typed global API should preserve statistic and threshold");
        require(localResult.getType() == DriftResult.DriftType.LOCAL_DRIFT
                        && localResult.getAffectedItemsets().contains("itemset"),
                "Typed local API should expose the affected itemset");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static class CountingMiner implements HUIItemsetMiner {
        private int calls;

        @Override
        public List<HighUtilityItemset> discover(List<Transaction> memory, int currentTid) {
            calls++;
            HighUtilityItemset hui = new HighUtilityItemset(
                    new LinkedHashSet<>(Collections.singletonList("item")));
            hui.setTotalUtility(10.0);
            hui.setDistanceToRoot(currentTid);
            return Collections.singletonList(hui);
        }

        @Override
        public void setTraceListener(Consumer<String> traceListener) {
        }
    }

    private static class FixedGlobalStrategy implements GlobalDriftStrategy {
        private int calls;

        @Override
        public DriftResult updateAndCheck(double observation, int oldCheckpointTid, int newCheckpointTid) {
            calls++;
            return DriftResult.globalDrift(oldCheckpointTid, newCheckpointTid, 0.5, 0.25, "TĂNG");
        }

        @Override
        public void setTraceListener(Consumer<String> traceListener) {
        }
    }

    private static class FixedLocalStrategy implements LocalDriftStrategy {
        private int calls;

        @Override
        public DriftResult detect(Checkpoint previous, Checkpoint current) {
            calls++;
            return DriftResult.localDrift(previous.getTid(), current.getTid(), 2.0, 1.0, "itemset");
        }

        @Override
        public void setTraceListener(Consumer<String> traceListener) {
        }
    }
}
