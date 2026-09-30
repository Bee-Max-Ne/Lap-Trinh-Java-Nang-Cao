package huddtds.math;

import huddtds.model.HighUtilityItemset;
import huddtds.model.ItemsetVector;
import java.util.List;

/**
 * UtilityMetrics chứa các hàm toán học dùng trong thuật toán HUDD-TDS.
 *
 * Các công thức chính:
 * - Decay function
 * - Hoeffding Bound
 * - Bonferroni correction
 * - Global drift epsilon - Equation (8)
 * - Local drift epsilon - Equation (9)
 * - D_mo distance
 */
public final class UtilityMetrics {

    private UtilityMetrics() {
    }

    /**
     * Hàm suy giảm theo thời gian.
     *
     * d_lambda(DeltaT) = 2^(-DeltaT / 2)
     */
    public static double decayFunction(int deltaTime) {
        return Math.pow(2.0, -((double) deltaTime / 2.0));
    }

    /**
     * Hoeffding Bound tổng quát.
     *
     * Hàm này vẫn được giữ lại để tương thích với các phần khác
     * của chương trình.
     */
    public static double hoeffdingBound(
            double range,
            double alpha,
            int n) {

        if (n <= 0 || alpha <= 0.0 || alpha >= 1.0) {
            return 0.0;
        }

        return Math.sqrt(
                (Math.pow(range, 2) * Math.log(1.0 / alpha))
                        / (2.0 * n)
        );
    }

    /**
     * Bonferroni correction.
     *
     * alpha' = alpha / count
     */
    public static double bonferroniAdjustedAlpha(
            double alpha,
            int count) {

        if (count <= 0) {
            return alpha;
        }

        return alpha / count;
    }

    /**
     * Global Drift - Equation (8) của HUDD-TDS.
     *
     * epsilon =
     * sqrt(
     *      (n - m) / (2*n*m)
     *      * ln(2/alpha)
     * )
     *
     * Trong đó:
     * n = tổng số observation
     * m = kích thước cửa sổ bên trái
     * alpha = mức ý nghĩa
     *
     * Các biến trong bài báo được chuẩn hóa trong [0,1].
     */
    public static double globalDriftEpsilon(
            int n,
            int m,
            double alpha,
            double range) {

        if (n <= 0 || m <= 0 || m >= n) {
            return Double.POSITIVE_INFINITY;
        }

        if (alpha <= 0.0 || alpha >= 1.0) {
            return Double.POSITIVE_INFINITY;
        }

        double epsilon = Math.sqrt(
                ((double) (n - m) / (2.0 * n * m))
                        * Math.log(2.0 / alpha)
        );

        /*
         * Trong HUDD-TDS, các observation được xem là
         * nằm trong [0,1]. range được giữ trong chữ ký
         * để tương thích với cấu trúc chương trình hiện tại.
         */
        return epsilon * Math.abs(range);
    }

    /**
     * Local Drift - Equation (9) của HUDD-TDS.
     *
     * epsilon_alpha =
     *
     * sqrt(
     *   2*m*sigma^2*ln(2ln(n)/alpha)
     * )
     *
     * +
     *
     * 2*m/3*ln(2ln(n)/alpha)
     *
     * với:
     *
     * n = n1 + n2
     * m = 1/n1 + 1/n2
     * sigma^2 = variance của utility distribution
     *
     * Đây là ngưỡng được sử dụng sau Bonferroni correction.
     */
    public static double localDriftEpsilon(
            int n1,
            int n2,
            double variance,
            double alpha) {

        if (n1 <= 0 || n2 <= 0) {
            return Double.POSITIVE_INFINITY;
        }

        if (alpha <= 0.0 || alpha >= 1.0) {
            return Double.POSITIVE_INFINITY;
        }

        int n = n1 + n2;

        if (n <= 1) {
            return Double.POSITIVE_INFINITY;
        }

        double m =
                (1.0 / n1)
                + (1.0 / n2);

        double logTerm =
                Math.log(
                        (2.0 * Math.log(n)) / alpha
                );

        if (logTerm < 0.0) {
            return Double.POSITIVE_INFINITY;
        }

        double firstTerm =
                Math.sqrt(
                        2.0
                        * m
                        * variance
                        * logTerm
                );

        double secondTerm =
                (2.0 * m / 3.0)
                * logTerm;

        return firstTerm + secondTerm;
    }

    /**
     * Tính phương sai population:
     *
     * sigma^2 =
     * sum((x_i - mean)^2) / n
     *
     * Đây là cách định nghĩa variance được mô tả
     * trong phần Local Utility Change Detection của bài báo.
     */
    public static double variance(double[] values) {

        if (values == null || values.length == 0) {
            return 0.0;
        }

        double sum = 0.0;

        for (double value : values) {
            sum += value;
        }

        double mean = sum / values.length;

        double squaredDistance = 0.0;

        for (double value : values) {
            double diff = value - mean;
            squaredDistance += diff * diff;
        }

        return squaredDistance / values.length;
    }

    /**
     * Tính khoảng cách D_mo từ itemset tới Root.
     */
    public static double calculateDmoToRoot(
            HighUtilityItemset itemset) {

        if (itemset == null || itemset.getItems().isEmpty()) {
            return 0.0;
        }

        double sumX2 = 0.0;
        double sumX = 0.0;

        for (String item : itemset.getItems()) {

            double value =
                    itemset.getItemUtilities()
                            .getOrDefault(item, 0.0);

            sumX2 += value * value;
            sumX += value;
        }

        double normX = Math.sqrt(sumX2);

        int k = itemset.getItems().size();

        double normRoot = Math.sqrt(k);

        if (normX == 0.0 || normRoot == 0.0) {
            return 0.0;
        }

        double sCos =
                sumX / (normX * normRoot);

        double magnitudeDiff =
                Math.abs(normX - normRoot);

        double maxMagnitude =
                Math.max(normX, normRoot);

        double sMo =
                sCos
                * (1.0 - (magnitudeDiff / maxMagnitude));

        return Math.max(
                0.0,
                1.0 - sMo
        );
    }

    /**
     * Tạo vector utility của HUI.
     */
    public static ItemsetVector buildItemsetVector(
            HighUtilityItemset itemset,
            List<String> dimensions) {

        ItemsetVector vector =
                new ItemsetVector(dimensions);

        for (String item : itemset.getItems()) {

            vector.setValue(
                    item,
                    itemset.getItemUtilities()
                            .getOrDefault(item, 0.0)
            );
        }

        return vector;
    }

    /**
     * Tạo vector Root.
     *
     * Mỗi chiều có giá trị 1.
     */
    public static ItemsetVector buildRootVector(
            List<String> dimensions) {

        ItemsetVector vector =
                new ItemsetVector(dimensions);

        for (String dimension : vector.getDimensions()) {
            vector.setValue(dimension, 1.0);
        }

        return vector;
    }
}
