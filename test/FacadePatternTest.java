package test;

import huddtds.application.SimulationService;
import huddtds.application.facade.SimulationFacade;
import huddtds.application.DatasetService;
import huddtds.application.SimulationConfiguration;
import huddtds.application.event.EventType;
import huddtds.application.event.SimulationEvent;
import huddtds.algorithm.drift.GlobalDriftStrategy;
import huddtds.algorithm.drift.LocalDriftStrategy;
import huddtds.algorithm.mining.HUIItemsetMiner;
import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;
import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Kiểm thử độc lập cho cách hiện thực mẫu Facade. */
public class FacadePatternTest {
    public static void main(String[] args) throws IOException {
        SimulationFacade facade = new SimulationFacade();

        // Kiểm tra tìm dataset qua Facade.
        List<String> datasets = facade.getAvailableDatasets();
        require(datasets != null, "Available datasets list should not be null");

        // Kiểm tra kiểm định dataset qua Facade.
        DatasetService.ValidationSummary summary = facade.validateDataset("Chess", null, 100);
        require(summary != null, "Validation summary should not be null");

        // Kiểm tra tạo SimulationService qua Facade.
        SimulationService simService = facade.createSimulationService(
                "Running Example (Mẫu)", null, null, 15.0, 1, 2, 0.10, 3);
        require(simService != null, "SimulationService created by Facade should not be null");
        List<SimulationEvent> events = new ArrayList<>();
        simService.addListener(events::add);

        // Kiểm tra luồng dữ liệu qua Facade, service và Observer.
        String sampleData = "a b:20:10 10\n";
        try (BufferedReader reader = facade.openTransactionStream("Running Example (Mẫu)", null, sampleData)) {
            require(reader != null, "Transaction stream reader should not be null");
            String line = reader.readLine();
            require(line != null && sampleData.trim().equals(line.trim()),
                    "Reader line should match input sample data");
            simService.processLine(line, 1);
        }
        require(events.stream().anyMatch(event ->
                        event.getType() == EventType.CHECKPOINT_CREATED
                                && event.getTid() == 1
                                && event.getCheckpoint() != null),
                "Facade stream should flow through the service and publish a checkpoint event");

        verifyFacadeCanComposeStrategies(facade);
        verifyFacadeCsvExport(facade);

        System.out.println("FacadePatternTest PASSED ✓");
    }

    private static void verifyFacadeCanComposeStrategies(SimulationFacade facade) throws IOException {
        CountingMiner miner = new CountingMiner();
        CountingGlobalStrategy global = new CountingGlobalStrategy();
        CountingLocalStrategy local = new CountingLocalStrategy();
        SimulationConfiguration configuration = SimulationConfiguration.builder("Running Example (Mẫu)")
                .withMinutil(15.0)
                .withInterval(1)
                .withWindowSize(2)
                .withAlphaConfidence(0.10)
                .withMaxItemsetSize(3)
                .withHuiItemsetMiner(miner)
                .withGlobalDriftStrategy(global)
                .withLocalDriftStrategy(local)
                .build();

        SimulationService service = facade.createSimulationService(configuration);
        List<SimulationEvent> events = new ArrayList<>();
        service.addListener(events::add);
        service.processLine("a:1", 1);
        service.processLine("a:2", 2);

        require(miner.calls == 2 && global.calls == 1 && local.calls == 1,
                "Facade configuration should inject all selected strategy implementations");
        require(events.stream().anyMatch(event -> event.getType() == EventType.GLOBAL_DRIFT)
                        && events.stream().anyMatch(event -> event.getType() == EventType.LOCAL_DRIFT),
                "Injected strategies should publish their results through the Observer contract");
    }

    private static void verifyFacadeCsvExport(SimulationFacade facade) throws IOException {
        Path csv = Files.createTempFile("huddtds-facade-export", ".csv");
        try {
            facade.exportToCSV(csv.toFile(),
                    Collections.singletonList(new String[]{"á,漢字", "value \"quoted\"", null}),
                    new String[]{"Name", "Description", "Empty"});
            List<String> lines = Files.readAllLines(csv, java.nio.charset.StandardCharsets.UTF_8);
            require(lines.size() == 2
                            && lines.get(1).equals("\"á,漢字\",\"value \"\"quoted\"\"\",\"\""),
                    "Facade CSV export should preserve UTF-8 and escape commas, quotes, and null values");
        } finally {
            Files.deleteIfExists(csv);
        }
    }

    private static final class CountingMiner implements HUIItemsetMiner {
        private int calls;

        @Override
        public List<HighUtilityItemset> discover(List<Transaction> memory, int currentTid) {
            calls++;
            return Collections.emptyList();
        }

        @Override
        public void setTraceListener(Consumer<String> traceListener) {
        }
    }

    private static final class CountingGlobalStrategy implements GlobalDriftStrategy {
        private int calls;

        @Override
        public DriftResult updateAndCheck(double observation, int oldTid, int newTid) {
            calls++;
            return DriftResult.globalDrift(oldTid, newTid, 1.0, 0.5, "TĂNG");
        }

        @Override
        public void setTraceListener(Consumer<String> traceListener) {
        }
    }

    private static final class CountingLocalStrategy implements LocalDriftStrategy {
        private int calls;

        @Override
        public DriftResult detect(Checkpoint previous, Checkpoint current) {
            calls++;
            return DriftResult.localDrift(previous.getTid(), current.getTid(), 1.0, 0.5, "[a]");
        }

        @Override
        public void setTraceListener(Consumer<String> traceListener) {
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
