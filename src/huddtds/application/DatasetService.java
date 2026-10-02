package huddtds.application;

import huddtds.data.DatasetInfo;
import huddtds.data.DatasetManager;
import huddtds.data.DatasetValidator;
import huddtds.data.InvestmentLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Application boundary for dataset discovery, validation, and file access.
 */
public class DatasetService {
    private final DatasetManager datasetManager;

    public DatasetService() {
        this(new DatasetManager());
    }

    public DatasetService(DatasetManager datasetManager) {
        this.datasetManager = Objects.requireNonNull(datasetManager, "datasetManager");
    }

    public List<String> getDatasetNames() {
        return datasetManager.getDatasetNames();
    }

    public ValidationSummary validate(String datasetName, File customTransactionFile, int maxCheckLines) {
        Objects.requireNonNull(datasetName, "datasetName");
        File datasetDirectory;
        if (datasetName.startsWith("[Tùy chỉnh") && customTransactionFile != null) {
            datasetDirectory = customTransactionFile.getParentFile();
        } else {
            DatasetInfo info = datasetManager.getDataset(datasetName);
            datasetDirectory = info != null && info.getTransactionsFile() != null
                    ? info.getTransactionsFile().getParentFile()
                    : new File("data/datasets", datasetName);
        }

        DatasetValidator.ValidationReport report =
                DatasetValidator.validate(datasetDirectory, maxCheckLines);
        return new ValidationSummary(report);
    }

    public Map<String, Double> loadInvestmentTable(
            String datasetName,
            File customInvestmentFile,
            boolean runningExample) throws IOException {
        if (runningExample) {
            Map<String, Double> investments = new HashMap<>();
            investments.put("a", 5.0);
            investments.put("b", 2.0);
            investments.put("c", 1.0);
            investments.put("d", 2.0);
            investments.put("e", 3.0);
            investments.put("g", 1.0);
            return investments;
        }
        if (datasetName.startsWith("[Tùy chỉnh")) {
            return customInvestmentFile == null
                    ? Collections.emptyMap()
                    : InvestmentLoader.load(customInvestmentFile);
        }
        return datasetManager.loadInvestmentTable(datasetName);
    }

    public BufferedReader openTransactionStream(
            String datasetName,
            File customTransactionFile) throws IOException {
        if (datasetName.startsWith("[Tùy chỉnh") && customTransactionFile != null) {
            return new BufferedReader(new FileReader(customTransactionFile));
        }
        return datasetManager.openTransactionStream(datasetName);
    }

    public int estimateTransactionCount(String datasetName, File customTransactionFile) {
        if (datasetName.startsWith("[Tùy chỉnh") && customTransactionFile != null) {
            return Math.max(100, (int) (customTransactionFile.length() / 150));
        }
        DatasetInfo info = datasetManager.getDataset(datasetName);
        return info == null
                ? 1000
                : Math.max(100, (int) (info.getDatasetSizeBytes() / 150));
    }

    public static final class ValidationSummary {
        public final String datasetName;
        public final int totalTransactions;
        public final int validTransactions;
        public final int distinctItemsCount;
        public final int investmentItemsCount;
        public final double totalDatasetUtility;
        public final boolean passed;
        public final List<String> errors;

        private ValidationSummary(DatasetValidator.ValidationReport report) {
            this.datasetName = report.datasetName;
            this.totalTransactions = report.totalTransactions;
            this.validTransactions = report.validTransactions;
            this.distinctItemsCount = report.distinctItemsCount;
            this.investmentItemsCount = report.investmentItemsCount;
            this.totalDatasetUtility = report.totalDatasetUtility;
            this.passed = report.passed;
            this.errors = Collections.unmodifiableList(new ArrayList<>(report.errors));
        }
    }
}
