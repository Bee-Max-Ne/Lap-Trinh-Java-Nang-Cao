package huddtds.application.facade;

import huddtds.algorithm.HUDD_TDS;
import huddtds.application.DatasetService;
import huddtds.application.SimulationConfiguration;
import huddtds.application.SimulationService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Facade Pattern: Điểm truy cập "một cửa" đóng gói toàn bộ các dịch vụ dữ liệu,
 * khởi tạo thuật toán mô phỏng HUDD-TDS, đọc luồng giao dịch và xuất báo cáo.
 *
 * Giúp tách rời tầng giao diện (Swing GUI, CLI, Web) khỏi sự phức tạp của các dịch vụ bên dưới.
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
     * Lấy danh mục các dataset sẵn có được phát hiện trong hệ thống.
     */
    public List<String> getAvailableDatasets() {
        return datasetService.getDatasetNames();
    }

    /**
     * Thực hiện kiểm định cấu trúc và độ hợp lệ của dataset đã chọn.
     */
    public DatasetService.ValidationSummary validateDataset(
            String datasetName, File customTransactionFile, int maxCheckLines) {
        return datasetService.validate(datasetName, customTransactionFile, maxCheckLines);
    }

    /**
     * Khởi tạo đối tượng SimulationService hoàn chỉnh bằng cách sử dụng HUDD_TDS.Builder.
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

        return createSimulationService(SimulationConfiguration.builder(datasetName)
                .withCustomInvestmentFile(customInvestmentFile)
                .withMinutil(minutil)
                .withInterval(interval)
                .withWindowSize(windowSize)
                .withAlphaConfidence(alphaConfidence)
                .withMaxItemsetSize(maxItemsetSize)
                .build());
    }

    /**
     * Compatibility overload for callers that previously supplied a title.
     * The title was not written by the original CSV export implementation.
     */
    public void exportToCSV(
            File outputFile,
            String title,
            List<String[]> rows,
            String[] headers) throws IOException {
        exportToCSV(outputFile, rows, headers);
    }

    public SimulationService createSimulationService(
            SimulationConfiguration configuration) throws IOException {
        Objects.requireNonNull(configuration, "configuration");

        String datasetName = configuration.getDatasetName();
        boolean isRunningExample = datasetName.contains("Running Example");
        Map<String, Double> externalUtilities = datasetService.loadInvestmentTable(
                datasetName, configuration.getCustomInvestmentFile(), isRunningExample);

        HUDD_TDS.Builder engineBuilder = new HUDD_TDS.Builder()
                .withExternalUtilities(externalUtilities)
                .withMinutil(configuration.getMinutil())
                .withInterval(configuration.getInterval())
                .withWindowSize(configuration.getWindowSize())
                .withAlphaConfidence(configuration.getAlphaConfidence())
                .withMaxItemsetSize(configuration.getMaxItemsetSize());
        if (configuration.getHuiItemsetMiner() != null) {
            engineBuilder.withMiner(configuration.getHuiItemsetMiner());
        }
        if (configuration.getGlobalDriftStrategy() != null) {
            engineBuilder.withGlobalStrategy(configuration.getGlobalDriftStrategy());
        }
        if (configuration.getLocalDriftStrategy() != null) {
            engineBuilder.withLocalStrategy(configuration.getLocalDriftStrategy());
        }

        HUDD_TDS engine = engineBuilder.build();
        return new SimulationService(engine);
    }

    /**
     * Mở luồng đọc BufferedReader cho giao dịch (từ tệp đĩa hoặc chuỗi văn bản nhập tay).
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
     * Ước tính tổng số dòng giao dịch trong bộ dữ liệu để hiển thị thanh tiến trình.
     */
    public int estimateTransactionCount(String datasetName, File customTransactionFile) {
        return datasetService.estimateTransactionCount(datasetName, customTransactionFile);
    }

    /**
     * Exports table data (HUI Log, Drift Log, or Summary Report) to a CSV file.
     */
    public void exportToCSV(File outputFile, List<String[]> rows, String[] headers) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile");
        Objects.requireNonNull(rows, "rows");

        try (java.io.BufferedWriter writer = Files.newBufferedWriter(
                outputFile.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            if (headers != null && headers.length > 0) {
                writeCsvRow(writer, headers);
            }
            for (String[] row : rows) {
                writeCsvRow(writer, Objects.requireNonNull(row, "CSV row"));
            }
        }
    }

    private static void writeCsvRow(java.io.BufferedWriter writer, String[] fields) throws IOException {
        for (int index = 0; index < fields.length; index++) {
            if (index > 0) {
                writer.write(',');
            }
            String value = fields[index] == null ? "" : fields[index];
            writer.write('"');
            writer.write(value.replace("\"", "\"\""));
            writer.write('"');
        }
        writer.newLine();
    }
}
