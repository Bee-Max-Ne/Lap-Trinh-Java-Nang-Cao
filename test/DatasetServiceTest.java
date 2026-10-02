package test;

import huddtds.application.DatasetService;

import java.io.BufferedReader;
import java.util.List;
import java.util.Map;

public class DatasetServiceTest {
    public static void main(String[] args) throws Exception {
        DatasetService service = new DatasetService();
        List<String> datasets = service.getDatasetNames();
        String chess = findDataset(datasets, "Chess");
        require(chess != null, "Dataset service should expose Chess");

        Map<String, Double> investments = service.loadInvestmentTable(chess, null, false);
        require(investments.size() == 75, "Dataset service should load Chess investments");

        try (BufferedReader reader = service.openTransactionStream(chess, null)) {
            require(reader.readLine() != null, "Dataset service should open the Chess stream");
        }

        require(service.estimateTransactionCount(chess, null) > 0,
                "Dataset service should estimate a positive transaction count");

        DatasetService.ValidationSummary validation = service.validate(chess, null, 5);
        require(validation.passed && validation.validTransactions == 5,
                "Dataset service should preserve bounded dataset validation results");
    }

    private static String findDataset(List<String> datasetNames, String name) {
        for (String datasetName : datasetNames) {
            if (datasetName.equalsIgnoreCase(name)) {
                return datasetName;
            }
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
