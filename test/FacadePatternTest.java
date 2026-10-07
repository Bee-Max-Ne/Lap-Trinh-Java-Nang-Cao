package test;

import huddtds.application.SimulationService;
import huddtds.application.facade.SimulationFacade;
import huddtds.application.DatasetService;
import huddtds.application.event.EventType;
import huddtds.application.event.SimulationEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        System.out.println("FacadePatternTest PASSED ✓");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
