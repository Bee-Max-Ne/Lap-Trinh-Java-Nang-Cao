package huddtds.algorithm.drift;

import huddtds.model.DriftResult;

/**
 * Lớp trừu raíces AbstractDriftDetector xác định bộ khung (template method) để
 * phát hiện trôi dạt theo 6 bước cố định. Các lớp cụ thể (GlobalDriftDetector,
 * LocalDriftDetector) sẽ triển khai các bước này để phù hợp với mô hình toán
 * của riêng chúng, đồng thời vẫn giữ nguyên việc triển khai các interface
 * Strategy (GlobalDriftStrategy, LocalDriftStrategy) để bảo toàn tính tương
 * thích với HUDD_TDS.
 */
public abstract class AbstractDriftDetector {

    /**
     * Định nghĩa đầu vào chung cho quá trình phát hiện trôi dạt.
     * Các lớp cụ thể sẽ mở rộng эту interface để cung cấp dữ liệu phù hợp.
     */
    public interface DriftInput {
        // Marker interface
    }

    /**
     * Phương thức cuối cùng xác định bộ khung 6 bước của quá trình kiểm định
     * trôi dạt. Không thể được ghi đè để đảm bảo tính nhất quán của thuật toán.
     *
     * @param input dữ liệu đầu vào cụ thể cho từng loại détecteur
     * @return kết quả phát hiện trôi dạt
     */
    public final DriftResult runDetection(DriftInput input) {
        // Bước 1: Kiểm tra đủ dữ liệu
        if (!hasEnoughData(input)) {
            return buildNoDriftResult(input);
        }

        // Bước 2: Tính toán giá trị thống kê
        double statistic = computeStatistic(input);

        // Bước 3: Tính toán ngưỡng kiểm định
        double epsilon = computeEpsilon(input);

        // Bước 4: Phán quyết
        boolean detected = Math.abs(statistic) >= epsilon;

        // Bước 5: Ghi log chẩn đoán
        traceResult(input, statistic, epsilon, detected);

        // Bước 6: Xây dựng kết quả
        if (detected) {
            return buildDetectedResult(input, statistic, epsilon);
        } else {
            return buildNoDriftResult(input);
        }
    }

    /**
     * Kiểm tra xem có đủ dữ liệu để thực hiện kiểm định không.
     * @param input dữ liệu đầu vào
     * @return true nếu đủ dữ liệu, false nếu không
     */
    protected abstract boolean hasEnoughData(DriftInput input);

    /**
     * Tính toán giá trị thống kê độ lệch dựa trên dữ liệu đầu vào.
     * @param input dữ liệu đầu vào
     * @return giá trị thống kê
     */
    protected abstract double computeStatistic(DriftInput input);

    /**
     * Tính toán ngưỡng kiểm định dựa trên mô hình toán và dữ liệu đầu vào.
     * @param input dữ liệu đầu vào
     * @return ngưỡng epsilon
     */
    protected abstract double computeEpsilon(DriftInput input);

    /**
     * Ghi log chẩn đoán sau khi tính toán thống kê và ngưỡng.
     * @param input dữ liệu đầu vào
     * @param statistic giá trị thống kê đã tính
     * @param epsilon   ngưỡng kiểm định đã tính
     * @param detected  kết quả phán quyết (true nếu phát hiện trôi dạt)
     */
    protected abstract void traceResult(DriftInput input, double statistic, double epsilon, boolean detected);

    /**
     * Xây dựng đối tượng DriftResult khi phát hiện trôi dạt.
     * @param input   dữ liệu đầu vào
     * @param statistic giá trị thống kê
     * @param epsilon   ngưỡng kiểm định
     * @return đối tượng DriftResult biểu diễn trôi dạt đã phát hiện
     */
    protected abstract DriftResult buildDetectedResult(DriftInput input, double statistic, double epsilon);

    /**
     * Xây dựng đối tượng DriftResult khi không phát hiện trôi dạt.
     * @param input dữ liệu đầu vào
     * @return đối tượng DriftResult biểu diễn không có trôi dạt
     */
    protected abstract DriftResult buildNoDriftResult(DriftInput input);
}