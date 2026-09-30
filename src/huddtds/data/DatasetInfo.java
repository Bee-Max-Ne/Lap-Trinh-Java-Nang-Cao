package huddtds.data;

import java.io.File;

/**
 * Lưu trữ thông tin siêu dữ liệu (Metadata) của một bộ dữ liệu.
 */
public class DatasetInfo {
    private final String name;
    private final File transactionsFile;
    private final File investmentFile;
    private int transactionCount;
    private int itemCount;
    private long datasetSizeBytes;
    private boolean isValid;
    private String validationMessage;

    public DatasetInfo(String name, File transactionsFile, File investmentFile) {
        this.name = name;
        this.transactionsFile = transactionsFile;
        this.investmentFile = investmentFile;
        this.transactionCount = 0;
        this.itemCount = 0;
        this.datasetSizeBytes = (transactionsFile != null && transactionsFile.exists())
                ? transactionsFile.length() : 0;
        this.isValid = false;
        this.validationMessage = "Chưa kiểm tra (Unvalidated)";
    }

    public String getName() {
        return name;
    }

    public File getTransactionsFile() {
        return transactionsFile;
    }

    public File getInvestmentFile() {
        return investmentFile;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public long getDatasetSizeBytes() {
        return datasetSizeBytes;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    @Override
    public String toString() {
        return String.format("%s (%d giao dịch, %d items, %.2f MB)",
                name, transactionCount, itemCount, datasetSizeBytes / (1024.0 * 1024.0));
    }
}
