package huddtds.benchmark;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cấu hình thực nghiệm chuẩn cho bộ 10 Dataset đo lường hiệu năng HUDD-TDS.
 */
public class BenchmarkConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String datasetName;
    private final String safeFileName;
    private final double minutil;
    private final int interval;
    private final int windowSize;
    private final double alpha;
    private final int maxPattern;
    private final int maxTxLimit;
    private final String manualInput;

    public BenchmarkConfig(String datasetName, String safeFileName, double minutil,
                           int interval, int windowSize, double alpha, int maxPattern,
                           int maxTxLimit, String manualInput) {
        this.datasetName = datasetName;
        this.safeFileName = safeFileName;
        this.minutil = minutil;
        this.interval = interval;
        this.windowSize = windowSize;
        this.alpha = alpha;
        this.maxPattern = maxPattern;
        this.maxTxLimit = maxTxLimit;
        this.manualInput = manualInput;
    }

    public String getDatasetName() { return datasetName; }
    public String getSafeFileName() { return safeFileName; }
    public double getMinutil() { return minutil; }
    public int getInterval() { return interval; }
    public int getWindowSize() { return windowSize; }
    public double getAlpha() { return alpha; }
    public int getMaxPattern() { return maxPattern; }
    public int getMaxTxLimit() { return maxTxLimit; }
    public String getManualInput() { return manualInput; }

    public static List<BenchmarkConfig> getStandardSuite() {
        List<BenchmarkConfig> suite = new ArrayList<>();

        // 1. Running Example (Mẫu giao dịch giáo trình)
        String sampleTx = "a:1 c:1 d:1\n"
                + "a:2 c:6 e:2 g:5\n"
                + "b:4 c:3 d:3 e:1\n"
                + "b:2 c:3 e:2 g:2";
        suite.add(new BenchmarkConfig("Running Example (Mẫu)", "01_Running_Example",
                15.0, 1, 2, 0.10, 4, 0, sampleTx));

        // 2. Chess-metadata
        suite.add(new BenchmarkConfig("Chess-metadata", "02_Chess",
                2000000.0, 300, 500, 0.05, 3, 0, null));

        // 3. NewChess-Metadata
        suite.add(new BenchmarkConfig("NewChess-Metadata", "03_NewChess",
                2000000.0, 300, 500, 0.05, 3, 0, null));

        // 4. mushrooms-metadata
        suite.add(new BenchmarkConfig("mushrooms-metadata", "04_Mushrooms",
                2500000.0, 500, 1000, 0.05, 3, 0, null));

        // 5. NewMushroom-Metadata
        suite.add(new BenchmarkConfig("NewMushroom-Metadata", "05_NewMushroom",
                2500000.0, 500, 1000, 0.05, 3, 0, null));

        // 6. connects-metadata
        suite.add(new BenchmarkConfig("connects-metadata", "06_Connects",
                3000000.0, 1000, 2000, 0.05, 3, 0, null));

        // 7. NewConnects-Metadata
        suite.add(new BenchmarkConfig("NewConnects-Metadata", "07_NewConnects",
                3000000.0, 1000, 2000, 0.05, 3, 0, null));

        // 8. Retail-metadata
        suite.add(new BenchmarkConfig("Retail-metadata", "08_Retail",
                100000.0, 1000, 2000, 0.05, 3, 0, null));

        // 9. Accidents-metadata
        suite.add(new BenchmarkConfig("Accidents-metadata", "09_Accidents",
                5000000.0, 2000, 5000, 0.05, 3, 0, null));

        // 10. Chainstore-metadata
        suite.add(new BenchmarkConfig("Chainstore-metadata", "10_Chainstore",
                500000.0, 5000, 10000, 0.05, 3, 0, null));

        return Collections.unmodifiableList(suite);
    }
}
