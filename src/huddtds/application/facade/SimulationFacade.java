package huddtds.application.facade;

import huddtds.algorithm.HUDD_TDS;
import huddtds.application.DatasetService;
import huddtds.application.SimulationService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Facade Pattern: Unified, simplified entry point for managing dataset operations,
 * simulation lifecycle, transaction stream parsing, and report exports.
 *
 * Decouples presentation layers (Swing UI, CLI) from complex underlying subsystems.
 */
public class SimulationFacade {
    private final DatasetService datasetService;

    public SimulationFacade() {
        this(new DatasetService());
    }

    public SimulationFacade(DatasetService datasetService) {
        this.datasetService = Objects.requireNonNull(datasetService, "datasetService");
    }

    /**
     * Gets available dataset options discovered in the repository.
     */
    public List<String> getAvailableDatasets() {
        return datasetService.getDatasetNames();
    }

    /**
     * Validates a selected dataset structure and integrity.
     */
    public DatasetService.ValidationSummary validateDataset(
            String datasetName, File customTransactionFile, int maxCheckLines) {
        return datasetService.validate(datasetName, customTransactionFile, maxCheckLines);
    }

    /**
     * Creates and initializes a complete HUDD-TDS simulation service.
     */
    public SimulationService createSimulationService(
            String datasetName,
            File customTransactionFile,
            File customInvestmentFile,
            double minutil,
            int interval,
            int windowSize,
            double alphaConfidence,
            int maxItemsetSize) throws IOException {

        boolean isRunningExample = datasetName != null && datasetName.contains("Running Example");
        Map<String, Double> externalUtilities = datasetService.loadInvestmentTable(
                datasetName, customInvestmentFile, isRunningExample);

        HUDD_TDS engine = new HUDD_TDS.Builder()
                .withExternalUtilities(externalUtilities)
                .withMinutil(minutil)
                .withInterval(interval)
                .withWindowSize(windowSize)
                .withAlphaConfidence(alphaConfidence)
                .withMaxItemsetSize(maxItemsetSize)
                .build();

        return new SimulationService(engine);
    }

    /**
     * Opens a transaction reader stream from dataset name, file, or manual text input.
     */
    public BufferedReader openTransactionStream(
            String datasetName,
            File customTransactionFile,
            String manualInputText) throws IOException {

        if (datasetName != null && datasetName.contains("Running Example")) {
            return new BufferedReader(new StringReader(manualInputText != null ? manualInputText : ""));
        }
        return datasetService.openTransactionStream(datasetName, customTransactionFile);
    }

    /**
     * Estimates total transactions in the dataset for progress tracking.
     */
    public int estimateTransactionCount(String datasetName, File customTransactionFile) {
        return datasetService.estimateTransactionCount(datasetName, customTransactionFile);
    }

    /**
     * Exports table data (HUI Log, Drift Log, or Summary Report) to a CSV file.
     */
    public void exportToCSV(File outputFile, String title, List<String[]> rows, String[] headers) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile");
        Objects.requireNonNull(rows, "rows");

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
            if (headers != null && headers.length > 0) {
                writer.println(String.join(",", headers));
            }
            for (String[] row : rows) {
                writer.println(String.join(",", row));
            }
        }
    }
}
