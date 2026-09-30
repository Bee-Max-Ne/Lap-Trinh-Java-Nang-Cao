package huddtds.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Checkpoint là điểm kiểm tra thời gian trong quá trình xử lý stream.
 * Mỗi checkpoint lưu trạng thái HUI hiện tại và khoảng cách toàn cục.
 */
public class Checkpoint {
    /** Mã giao dịch/tid tại thời điểm checkpoint */
    private final int tid;

    /** Danh sách HUI được tìm thấy tại checkpoint này */
    private final List<HighUtilityItemset> huis;

    /** Tổng khoảng cách globalDistance của tất cả HUI tại checkpoint */
    private double globalDistance;

    /**
     * Khởi tạo một checkpoint mới tại thời điểm TID cụ thể.
     * @param tid mã giao dịch thời điểm checkpoint
     */
    public Checkpoint(int tid) {
        this.tid = tid;
        this.huis = new ArrayList<>();
        this.globalDistance = 0.0;
    }

    /**
     * Trả về TID của checkpoint.
     * @return tid
     */
    public int getTid() {
        return tid;
    }

    /**
     * Trả về danh sách HUI của checkpoint.
     * @return list HUI
     */
    public List<HighUtilityItemset> getHuis() {
        return huis;
    }

    /**
     * Trả về khoảng cách tổng globalDistance.
     * @return khoảng cách toàn cục
     */
    public double getGlobalDistance() {
        return globalDistance;
    }

    /**
     * Gán khoảng cách toàn cục cho checkpoint.
     * @param globalDistance khoảng cách mới
     */
    public void setGlobalDistance(double globalDistance) {
        this.globalDistance = globalDistance;
    }
}
