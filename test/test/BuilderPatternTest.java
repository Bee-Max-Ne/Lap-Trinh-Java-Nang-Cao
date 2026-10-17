package test;

import huddtds.algorithm.HUDD_TDS;
import huddtds.application.SimulationConfiguration;
import huddtds.application.event.EventType;
import huddtds.application.event.SimulationEvent;
import huddtds.model.Checkpoint;
import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Bài kiểm thử tự động (Unit Test) kiểm tra tính đúng đắn của việc triển khai Builder Pattern
 * cho lớp HUDD_TDS và lớp SimulationEvent.
 */
public class BuilderPatternTest {
    public static void main(String[] args) {
        // 1. Kiểm thử khởi tạo HUDD_TDS thông qua chuỗi Fluent Interface Chaining của Builder
        testHuddTdsBuilder();
        
        // 2. Kiểm thử tính năng bắt lỗi hợp lệ (Validation) khi truyền tham số sai
        testHuddTdsBuilderValidation();

        testSimulationConfigurationBuilderValidation();

        // 3. Kiểm thử khởi tạo đối tượng sự kiện SimulationEvent qua Builder
        testSimulationEventBuilder();

        System.out.println("BuilderPatternTest PASSED ✓ (Tất cả kiểm thử Builder Pattern đã THÀNH CÔNG)");
    }

    private static void testHuddTdsBuilder() {
        Map<String, Double> utilities = Collections.singletonMap("a", 5.0);
        HUDD_TDS engine = new HUDD_TDS.Builder()
                .withExternalUtilities(utilities)
                .withMinutil(15.0)
                .withInterval(10)
                .withWindowSize(20)
                .withAlphaConfidence(0.05)
                .withMaxItemsetSize(3)
                .build();

        require(engine != null, "Đối tượng HUDD_TDS được tạo qua Builder không được null");
        require(engine.getMinutil() == 15.0, "Ngưỡng minutil phải khớp với giá trị truyền vào Builder");
        require(engine.getInterval() == 10, "Chu kỳ interval phải khớp với giá trị truyền vào Builder");
        require(engine.getWindowSize() == 20, "Kích thước windowSize phải khớp với giá trị truyền vào Builder");
        require(engine.getAlphaConfidence() == 0.05, "Ngưỡng alphaConfidence phải khớp với giá trị truyền vào Builder");

        Map<String, Double> mutableUtilities = new HashMap<>();
        mutableUtilities.put("a", 5.0);
        HUDD_TDS snapshotEngine = new HUDD_TDS.Builder()
                .withExternalUtilities(mutableUtilities)
                .withMinutil(1.0)
                .withInterval(1)
                .withWindowSize(1)
                .withAlphaConfidence(0.05)
                .withMaxItemsetSize(1)
                .build();
        mutableUtilities.put("a", 50.0);
        Transaction transaction = new Transaction(1);
        transaction.addElement("a", 1);
        Checkpoint checkpoint = snapshotEngine.processTransaction(transaction);
        HighUtilityItemset hui = checkpoint.getHuis().get(0);
        require(hui.getTotalUtility() == 5.0,
                "Engine Builder should snapshot external utilities rather than retain a mutable caller map");
    }

    private static void testHuddTdsBuilderValidation() {
        // Kiểm thử Validation 1: Ngưỡng minutil âm phải ném ra IllegalArgumentException
        try {
            new HUDD_TDS.Builder().withMinutil(-5.0).build();
            throw new AssertionError("Phải báo lỗi khi minutil âm");
        } catch (IllegalArgumentException expected) {
            // Ném lỗi hợp lệ như mong đợi
        }

        expectIllegalArgument(() -> new HUDD_TDS.Builder().withMinutil(Double.NaN),
                "Builder phải từ chối minutil không hữu hạn");
        expectIllegalArgument(() -> new HUDD_TDS.Builder().withAlphaConfidence(1.0),
                "Builder phải từ chối alpha=1 vì công thức thống kê yêu cầu alpha<1");
        expectIllegalArgument(() -> new HUDD_TDS.Builder().withMaxItemsetSize(-1),
                "Builder phải từ chối độ dài itemset âm");
    }

    private static void testSimulationConfigurationBuilderValidation() {
        SimulationConfiguration configuration = SimulationConfiguration.builder("Running Example")
                .withMinutil(0.0)
                .withInterval(2)
                .withWindowSize(4)
                .withAlphaConfidence(0.05)
                .withMaxItemsetSize(3)
                .build();
        require(configuration.getInterval() == 2 && configuration.getWindowSize() == 4,
                "Simulation configuration should preserve its validated parameters");

        expectIllegalArgument(() -> SimulationConfiguration.builder(" "),
                "Configuration phải yêu cầu tên dataset");
        expectIllegalArgument(() -> SimulationConfiguration.builder("Chess").withAlphaConfidence(1.0),
                "Configuration phải từ chối alpha=1");
        expectIllegalArgument(() -> SimulationConfiguration.builder("Chess")
                        .withMinutil(Double.POSITIVE_INFINITY),
                "Configuration phải từ chối minutil không hữu hạn");

        // Kiểm thử Validation 2: Ngưỡng alphaConfidence vượt quá 1 phải ném ra IllegalArgumentException
        try {
            new HUDD_TDS.Builder().withAlphaConfidence(1.5).build();
            throw new AssertionError("Phải báo lỗi khi alphaConfidence > 1");
        } catch (IllegalArgumentException expected) {
            // Ném lỗi hợp lệ như mong đợi
        }
    }

    private static void testSimulationEventBuilder() {
        Transaction tx = new Transaction(100);
        SimulationEvent event = new SimulationEvent.Builder()
                .withType(EventType.TRANSACTION_PROCESSED)
                .withTid(100)
                .withTransaction(tx)
                .withTotalEstimate(5000)
                .withSpeedTxPerSec(1200)
                .withMessage("Cập nhật tiến trình mô phỏng")
                .build();

        require(event.getType() == EventType.TRANSACTION_PROCESSED, "Loại sự kiện phải khớp");
        require(event.getTid() == 100, "Mã TID phải khớp");
        require(event.getTotalEstimate() == 5000, "Ước tính tổng số dòng phải khớp");
        require(event.getSpeedTxPerSec() == 1200, "Tốc độ thông lượng phải khớp");
        require("Cập nhật tiến trình mô phỏng".equals(event.getMessage()), "Thông điệp mô tả phải khớp");
        expectIllegalState(() -> new SimulationEvent.Builder().build(),
                "Event builder phải yêu cầu EventType");
        expectIllegalState(() -> new SimulationEvent.Builder()
                        .withType(EventType.CHECKPOINT_CREATED)
                        .withTid(1)
                        .build(),
                "Checkpoint event phải có checkpoint payload");
        expectIllegalState(() -> new SimulationEvent.Builder()
                        .withType(EventType.TRANSACTION_PROCESSED)
                        .withTid(1)
                        .withSpeedTxPerSec(-1)
                        .build(),
                "Progress event không được có metadata âm");
    }

    private static void expectIllegalArgument(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }

    private static void expectIllegalState(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalStateException expected) {
            // Expected validation failure.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("LỖI KIỂM THỬ: " + message);
        }
    }
}
