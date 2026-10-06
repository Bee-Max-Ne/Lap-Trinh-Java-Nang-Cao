package test;

import huddtds.application.SimulationService;
import huddtds.application.facade.SimulationFacade;
import huddtds.application.DatasetService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Unit test verifying Facade Pattern implementation.
 */
public class FacadePatternTest {
    public static void main(String[] args) throws IOException {
        SimulationFacade facade = new SimulationFacade();

        // Test 1: Discover datasets via Facade
        List<String> datasets = facade.getAvailableDatasets();
        require(datasets != null, "Available datasets list should not be null");

        // Test 2: Validate dataset via Facade
        DatasetService.ValidationSummary summary = facade.validateDataset("Chess", null, 100);
        require(summary != null, "Validation summary should not be null");

        // Test 3: Create SimulationService via Facade
        SimulationService simService = facade.createSimulationService(
                "Running Example (Mẫu)", null, null, 15.0, 1, 2, 0.10, 3);
        require(simService != null, "SimulationService created by Facade should not be null");

        // Test 4: Open transaction stream via Facade
        String sampleData = "a(5) b(2):10:a(1) b(1)\n";
        try (BufferedReader reader = facade.openTransactionStream("Running Example (Mẫu)", null, sampleData)) {
            require(reader != null, "Transaction stream reader should not be null");
            String line = reader.readLine();
            require(sampleData.trim().equals(line.trim()), "Reader line should match input sample data");
        }

        System.out.println("FacadePatternTest PASSED ✓");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
