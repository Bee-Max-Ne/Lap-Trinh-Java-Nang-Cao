package huddtds.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Quản lý kho bộ dữ liệu (DatasetManager).
 * Tự động quét và phát hiện các dataset trong:
 * 1. data/datasets/
 * 2. Dataset-metadata (CapNhat 14-02)/
 * 3. Thư mục gốc dự án (ví dụ: ../Chess-metadata)
 */
public class DatasetManager {
    private final List<File> searchDirectories;
    private final Map<String, DatasetInfo> datasets;

    public DatasetManager(File baseDirectory) {
        this.searchDirectories = new ArrayList<>();
        if (baseDirectory != null && baseDirectory.exists()) {
            this.searchDirectories.add(baseDirectory);
        }

        // Bổ sung các vị trí chứa dataset thực tế
        File capNhatDir = new File("Dataset-metadata (CapNhat 14-02)");
        if (capNhatDir.exists()) {
            this.searchDirectories.add(capNhatDir);
        }

        File currentDir = new File(".");
        File parentDir = new File("..");
        this.searchDirectories.add(currentDir);
        if (parentDir.exists()) {
            this.searchDirectories.add(parentDir);
        }

        this.datasets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        scanDatasets();
    }

    public DatasetManager() {
        this(new File("data/datasets"));
    }

    /**
     * Quét toàn bộ các thư mục tìm kiếm để phát hiện dataset hợp lệ.
     */
    public void scanDatasets() {
        datasets.clear();

        for (File baseDir : searchDirectories) {
            if (!baseDir.exists() || !baseDir.isDirectory()) {
                continue;
            }

            File[] subDirs = baseDir.listFiles(File::isDirectory);
            if (subDirs == null) {
                continue;
            }

            for (File dir : subDirs) {
                // Bỏ qua thư mục hệ thống / code
                String dName = dir.getName();
                if (dName.equals(".vscode") || dName.equals("src") || dName.equals("test")
                        || dName.equals("out") || dName.equals("bin") || dName.equals("logs")
                        || dName.equals(".git") || dName.equals("scratch")) {
                    continue;
                }

                File transFile = new File(dir, "transactions.txt");
                File invFile = new File(dir, "investment_table.txt");

                if (transFile.exists() && transFile.isFile()) {
                    DatasetInfo info = new DatasetInfo(dir.getName(), transFile, invFile.exists() ? invFile : null);
                    datasets.putIfAbsent(dir.getName(), info);
                }
            }
        }
    }

    public List<String> getDatasetNames() {
        return new ArrayList<>(datasets.keySet());
    }

    public DatasetInfo getDataset(String name) {
        return datasets.get(name);
    }

    public Map<String, DatasetInfo> getAllDatasets() {
        return Collections.unmodifiableMap(datasets);
    }

    /**
     * Mở một BufferedReader để đọc luồng transaction từ dataset.
     *
     * @param datasetName tên dataset (ví dụ: Chess, Chess-metadata)
     * @return BufferedReader
     * @throws IOException nếu lỗi
     */
    public BufferedReader openTransactionStream(String datasetName) throws IOException {
        DatasetInfo info = getDataset(datasetName);
        if (info == null || !info.getTransactionsFile().exists()) {
            throw new IOException("Không tìm thấy tệp giao dịch cho dataset: " + datasetName);
        }
        return new BufferedReader(new FileReader(info.getTransactionsFile()), 64 * 1024);
    }

    /**
     * Tải bảng đầu tư cho một dataset.
     *
     * @param datasetName tên dataset
     * @return Map<String, Double>
     * @throws IOException nếu lỗi
     */
    public Map<String, Double> loadInvestmentTable(String datasetName) throws IOException {
        DatasetInfo info = getDataset(datasetName);
        if (info != null && info.getInvestmentFile() != null && info.getInvestmentFile().exists()) {
            return InvestmentLoader.load(info.getInvestmentFile());
        }
        return Collections.emptyMap();
    }
}
