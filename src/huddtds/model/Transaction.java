package huddtds.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lớp Transaction mô tả một giao dịch trong stream dữ liệu.
 * Hỗ trợ lưu trữ hiệu năng cao với Map tra cứu O(1) và tổng TU (Transaction Utility).
 */
public class Transaction {
    /** Mã định danh của giao dịch */
    private final int tid;

    /** Danh sách các phần tử thuộc giao dịch */
    private final List<Element> elements;

    /** Bảng băm tra cứu nhanh phần tử theo mã item O(1) */
    private final Map<String, Element> elementMap;

    /** Tổng tiện ích của giao dịch (TU = sum of item utilities) */
    private double transactionUtility;

    /**
     * Khởi tạo giao dịch mới với mã TID.
     * @param tid mã định danh của giao dịch
     */
    public Transaction(int tid) {
        this.tid = tid;
        this.elements = new ArrayList<>();
        this.elementMap = new HashMap<>();
        this.transactionUtility = 0.0;
    }

    /**
     * Khởi tạo giao dịch với mã TID và tổng TU định sẵn.
     * @param tid mã định danh của giao dịch
     * @param transactionUtility tổng utility của giao dịch
     */
    public Transaction(int tid, double transactionUtility) {
        this.tid = tid;
        this.elements = new ArrayList<>();
        this.elementMap = new HashMap<>();
        this.transactionUtility = transactionUtility;
    }

    /**
     * Trả về mã giao dịch.
     * @return tid của giao dịch
     */
    public int getTid() {
        return tid;
    }

    /**
     * Trả về toàn bộ danh sách các phần tử trong giao dịch.
     * @return danh sách Element
     */
    public List<Element> getElements() {
        return elements;
    }

    /**
     * Thêm một phần tử vào giao dịch với số lượng (quantity-based).
     * @param item tên/mã item
     * @param quantity số lượng của item đó
     */
    public void addElement(String item, int quantity) {
        addElement(new Element(item, quantity));
    }

    /**
     * Thêm một phần tử vào giao dịch với độ lợi trực tiếp (utility-based).
     * @param item tên/mã item
     * @param utility giá trị độ lợi thực tế của item
     */
    public void addElement(String item, double utility) {
        addElement(new Element(item, utility));
    }

    /**
     * Thêm một đối tượng Element vào giao dịch.
     * @param element phần tử cần thêm
     */
    public void addElement(Element element) {
        elements.add(element);
        elementMap.put(element.getItem(), element);
    }

    /**
     * Kiểm tra xem giao dịch có chứa item hay không (độ phức tạp O(1)).
     * @param item tên/mã item cần kiểm tra
     * @return true nếu giao dịch chứa item, ngược lại false
     */
    public boolean containsItem(String item) {
        return elementMap.containsKey(item);
    }

    /**
     * Lấy số lượng của một item trong giao dịch (O(1)).
     * @param item tên item cần lấy số lượng
     * @return số lượng của item nếu tồn tại, nếu không thì trả về 0
     */
    public int getQuantity(String item) {
        Element el = elementMap.get(item);
        return el != null ? el.getQuantity() : 0;
    }

    /**
     * Lấy độ lợi trực tiếp của một item trong giao dịch (O(1)).
     * @param item tên item cần lấy utility
     * @return utility của item nếu tồn tại, nếu không thì trả về 0.0
     */
    public double getUtility(String item) {
        Element el = elementMap.get(item);
        return el != null ? el.getUtility() : 0.0;
    }

    /**
     * Lấy tổng TU (Transaction Utility).
     * @return transactionUtility
     */
    public double getTransactionUtility() {
        return transactionUtility;
    }

    /**
     * Thiết lập tổng TU cho giao dịch.
     * @param transactionUtility tổng utility
     */
    public void setTransactionUtility(double transactionUtility) {
        this.transactionUtility = transactionUtility;
    }

    /**
     * Tính TU của giao dịch:
     * - Nếu transactionUtility > 0: trả về transactionUtility.
     * - Nếu các phần tử có utility > 0: cộng dồn utility của các phần tử.
     * - Ngược lại: tính theo công thức cổ điển (quantity * externalUtility).
     *
     * @param externalUtilities bảng utility ngoại vi
     * @return tổng utility của giao dịch
     */
    public double calculateTotalUtility(Map<String, Double> externalUtilities) {
        if (transactionUtility > 0.0) {
            return transactionUtility;
        }

        double totalUtility = 0.0;
        for (Element element : elements) {
            if (element.getUtility() > 0.0) {
                totalUtility += element.getUtility();
            } else {
                double ext = (externalUtilities != null)
                        ? externalUtilities.getOrDefault(element.getItem(), 1.0)
                        : 1.0;
                totalUtility += element.getQuantity() * ext;
            }
        }
        return totalUtility;
    }
}
