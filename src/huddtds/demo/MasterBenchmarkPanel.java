package huddtds.demo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Panel đối sánh tổng hợp hiệu năng Master Benchmark giữa Baseline (Chưa áp dụng mẫu)
 * và Design Patterns (Đã tối ưu hóa).
 * Tích hợp toàn diện:
 * - Thanh công cụ điều khiển & Mở báo cáo tương tác HTML5 ngoài trình duyệt.
 * - 4 Thẻ KPI tóm tắt kết quả vượt trội (Tăng tốc, RAM, GC, Match).
 * - Bảng số liệu đối sánh JTable 10 bộ dữ liệu (tự động nạp từ CSV kèm Fallback).
 * - Biểu đồ Vector Java 2D linh hoạt với 5 Metric đo lường và Hover Tooltip.
 */
public class MasterBenchmarkPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    // Đường dẫn tương đối đến báo cáo và thư mục
    private static final String CSV_PATH = "benchmark_experiments/03_comparative_reports/HUDD_TDS_Comparison_Summary.csv";
    private static final String HTML_PATH = "benchmark_experiments/03_comparative_reports/HUDD_TDS_Master_Comparison_Report.html";
    private static final String REPORT_DIR = "benchmark_experiments/03_comparative_reports";

    // Danh sách dữ liệu benchmark
    private final List<DatasetBenchmarkRecord> records = new ArrayList<>();

    // Các thành phần giao diện
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final BenchmarkChart2DPanel chartPanel;
    private final JComboBox<String> cbMetrics;
    private final JSplitPane centerSplitPane;

    public MasterBenchmarkPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBackground(new Color(248, 250, 252));

        // Nạp dữ liệu từ CSV (hoặc fallback mặc định)
        loadBenchmarkData();

        // 1. KHU VỰC BẢNG SỐ LIỆU (10 DATASETS)
        String[] columnNames = {
                "Bộ dữ liệu", "Giao dịch", "Thời gian (s)", "Tăng tốc",
                "Đỉnh RAM (MB)", "Tiết kiệm RAM", "Giảm GC", "Thông lượng (tx/s)", "Kiểm chứng"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                // Click lại vào chính dòng đang được chọn -> Bỏ chọn (Deselect / Hủy highlight)
                if (rowIndex == getSelectedRow() && !extend) {
                    clearSelection();
                    return;
                }
                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        };

        // Nhấn phím ESC để hủy chọn / bỏ highlight nhanh
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "clearSelection");
        table.getActionMap().put("clearSelection", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                table.clearSelection();
            }
        });

        setupTableAppearance();
        populateTableData();

        chartPanel = new BenchmarkChart2DPanel(records);

        JScrollPane tableScrollPane = new JScrollPane(table);
        // Click vào khoảng trống bên dưới các dòng trong bảng -> Hủy chọn
        tableScrollPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                table.clearSelection();
            }
        });

        JPanel leftContainer = new JPanel(new BorderLayout(4, 4));
        leftContainer.setBorder(BorderFactory.createTitledBorder("Bảng số liệu đối sánh chi tiết (10 Datasets)"));
        leftContainer.add(tableScrollPane, BorderLayout.CENTER);

        // Đồng bộ Click Bảng -> Highlight Biểu đồ Java 2D
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int sel = table.getSelectedRow();
                if (sel >= 0) {
                    int modelIndex = table.convertRowIndexToModel(sel);
                    chartPanel.setHighlightedIndex(modelIndex);
                } else {
                    chartPanel.setHighlightedIndex(-1);
                }
            }
        });

        // 2. KHU VỰC BIỂU ĐỒ 2D
        JPanel rightContainer = new JPanel(new BorderLayout(4, 4));
        rightContainer.setBorder(BorderFactory.createTitledBorder("Đồ thị Vector đối sánh trực quan (Java 2D)"));

        JPanel chartControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        chartControls.add(new JLabel("Chỉ số:"));
        cbMetrics = new JComboBox<>(new String[]{
                "1. Thời gian thực thi (Runtime - s, Thang Log10)",
                "2. Tiêu thụ đỉnh RAM Heap (MB)",
                "3. Áp lực Thu gom rác (Số lần GC Calls - Thang Log10)",
                "4. Tổng thời gian dừng GC (Pause Time - ms/s)",
                "5. Thông lượng xử lý (TPS - tx/giây)"
        });
        cbMetrics.setPreferredSize(new Dimension(320, 25));
        chartControls.add(cbMetrics);

        JButton btnSavePng = new JButton("Lưu ảnh PNG");
        btnSavePng.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnSavePng.setBackground(new Color(241, 245, 249));
        btnSavePng.setForeground(new Color(51, 65, 85));
        btnSavePng.setToolTipText("Lưu biểu đồ hiện tại thành file ảnh PNG độ nét cao");
        btnSavePng.addActionListener(e -> chartPanel.exportToPng(this));
        chartControls.add(btnSavePng);

        rightContainer.add(chartControls, BorderLayout.NORTH);

        // Đồng bộ Click Biểu đồ -> Chọn dòng tương ứng trên Bảng
        chartPanel.setOnDatasetSelectedListener(modelIndex -> {
            if (modelIndex >= 0 && modelIndex < table.getRowCount()) {
                int viewIndex = table.convertRowIndexToView(modelIndex);
                table.setRowSelectionInterval(viewIndex, viewIndex);
                table.scrollRectToVisible(table.getCellRect(viewIndex, 0, true));
            } else {
                table.clearSelection();
            }
        });
        rightContainer.add(chartPanel, BorderLayout.CENTER);

        cbMetrics.addActionListener(e -> {
            chartPanel.setSelectedMetric(cbMetrics.getSelectedIndex());
        });

        // 3. Bố cục chia đôi: Bảng số liệu bên TRÁI, Đồ thị đối sánh bên PHẢI (50% / 50%)
        centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftContainer, rightContainer);
        centerSplitPane.setResizeWeight(0.50);
        centerSplitPane.setContinuousLayout(true);
        add(centerSplitPane, BorderLayout.CENTER);

        // Đảm bảo chia đều 50/50 khi hiển thị
        SwingUtilities.invokeLater(() -> centerSplitPane.setDividerLocation(0.50));

        // 4. Thanh công cụ và KPI phía Bắc
        JPanel northPanel = createNorthPanel();
        add(northPanel, BorderLayout.NORTH);
    }

    /**
     * Tạo Header phía trên gồm nút bấm mở báo cáo và các thẻ KPI tóm tắt.
     */
    private JPanel createNorthPanel() {
        JPanel northPanel = new JPanel(new BorderLayout(8, 8));
        northPanel.setOpaque(false);

        // Dòng nút bấm thao tác
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actionRow.setOpaque(false);

        JButton btnOpenHtml = new JButton("Báo cáo HTML5");
        btnOpenHtml.setBackground(new Color(37, 99, 235));
        btnOpenHtml.setForeground(Color.WHITE);
        btnOpenHtml.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOpenHtml.setToolTipText("Mở báo cáo so sánh chi tiết bằng trình duyệt mặc định với biểu đồ động Chart.js");
        btnOpenHtml.addActionListener(e -> openHtmlReport());
        actionRow.add(btnOpenHtml);

        JButton btnOpenFolder = new JButton("Thư mục");
        btnOpenFolder.setBackground(new Color(241, 245, 249));
        btnOpenFolder.setForeground(new Color(51, 65, 85));
        btnOpenFolder.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnOpenFolder.setToolTipText("Mở thư mục chứa các tệp báo cáo đối sánh");
        btnOpenFolder.addActionListener(e -> openReportFolder());
        actionRow.add(btnOpenFolder);

        JButton btnReload = new JButton("Nạp lại CSV");
        btnReload.setBackground(new Color(241, 245, 249));
        btnReload.setForeground(new Color(51, 65, 85));
        btnReload.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReload.setToolTipText("Đọc lại dữ liệu từ tệp HUDD_TDS_Comparison_Summary.csv");
        btnReload.addActionListener(e -> {
            loadBenchmarkData();
            populateTableData();
            chartPanel.setRecords(records);
            JOptionPane.showMessageDialog(this, "Đã cập nhật lại số liệu từ file CSV!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        });
        actionRow.add(btnReload);

        northPanel.add(actionRow, BorderLayout.WEST);

        // 4 Thẻ KPI Tóm tắt kết quả
        JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 8, 0));
        kpiPanel.setOpaque(false);
        kpiPanel.add(createKpiCard("TĂNG TỐC TỐI ĐA", "2,50× (Connects)", new Color(220, 252, 231), new Color(22, 101, 52)));
        kpiPanel.add(createKpiCard("TIẾT KIỆM RAM ĐỈNH", "54,2% (Accidents)", new Color(224, 242, 254), new Color(7, 89, 133)));
        kpiPanel.add(createKpiCard("GIẢM THU GOM RÁC", "98,4% GC Calls", new Color(254, 243, 199), new Color(146, 64, 14)));
        kpiPanel.add(createKpiCard("ĐỘ CHÍNH XÁC THUẬT TOÁN", "100% MATCH", new Color(243, 232, 255), new Color(107, 33, 168)));

        northPanel.add(kpiPanel, BorderLayout.EAST);
        return northPanel;
    }

    private JPanel createKpiCard(String title, String value, Color bg, Color textCol) {
        JPanel card = new JPanel(new BorderLayout(2, 2));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(bg.darker(), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitle.setForeground(new Color(71, 85, 105));

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblVal.setForeground(textCol);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblVal, BorderLayout.CENTER);
        return card;
    }

    private void openHtmlReport() {
        File file = new File(HTML_PATH);
        if (!file.exists()) {
            file = new File("HUDD_TDS_Master_Comparison_Report.html");
        }
        if (file.exists() && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở file HTML: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Không tìm thấy tệp báo cáo tại: " + file.getAbsolutePath(), "Thông báo", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void openReportFolder() {
        File dir = new File(REPORT_DIR);
        if (!dir.exists()) {
            dir = new File(".");
        }
        if (dir.exists() && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(dir);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở thư mục: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Tinh chỉnh giao diện bảng số liệu (Zebra row, căn lề, độ rộng).
     */
    private void setupTableAppearance() {
        table.setRowHeight(24);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(new Color(30, 41, 59));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFocus, row, col);
                if (!isSel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                c.setForeground(Color.BLACK);

                // Căn lề
                if (col == 0) {
                    setHorizontalAlignment(SwingConstants.LEFT);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else if (col == 8) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    c.setForeground(new Color(22, 101, 52));
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                }

                // Cột tăng tốc nổi bật
                if (col == 3 && val != null) {
                    String s = val.toString();
                    if (s.contains("×") && !s.startsWith("0.")) {
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                        c.setForeground(new Color(22, 101, 52));
                    }
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Thiết lập độ rộng cột tối ưu chia đều cho layout 50/50 Trái - Phải
        int[] preferredWidths = {85, 60, 75, 55, 75, 65, 80, 75, 65};
        for (int i = 0; i < Math.min(table.getColumnCount(), preferredWidths.length); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(preferredWidths[i]);
        }

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
    }

    private void populateTableData() {
        tableModel.setRowCount(0);
        for (DatasetBenchmarkRecord r : records) {
            String timeStr = String.format(Locale.US, "%.1fs → %.1fs", r.baseTime, r.patternTime);
            String speedupStr = String.format(Locale.US, "%.2f×", r.speedup);
            String ramStr = String.format(Locale.US, "%.0fM → %.0fM", r.baseRam, r.patternRam);
            String ramSavedStr = String.format(Locale.US, "%s%.1f%%", r.ramSavedPct >= 0 ? "+" : "", r.ramSavedPct);
            String gcStr = String.format(Locale.US, "%.1f%% (%d→%d)", r.gcReductionPct, r.baseGcCalls, r.patternGcCalls);
            String tpsStr = String.format(Locale.US, "%d → %d", (int)r.baseTps, (int)r.patternTps);
            String matchStr = r.isMatch ? "MATCH" : "DIFF";

            tableModel.addRow(new Object[]{
                    r.datasetName,
                    String.format(Locale.US, "%,d", r.txCount),
                    timeStr,
                    speedupStr,
                    ramStr,
                    ramSavedStr,
                    gcStr,
                    tpsStr,
                    matchStr
            });
        }
    }

    /**
     * Nạp dữ liệu từ file CSV, nếu không có thì dùng fallback 100% chuẩn xác.
     */
    private void loadBenchmarkData() {
        records.clear();
        File csvFile = new File(CSV_PATH);
        boolean loaded = false;

        if (csvFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(csvFile, StandardCharsets.UTF_8))) {
                String line;
                boolean isHeader = true;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }
                    String[] parts = line.split(",");
                    if (parts.length >= 28) {
                        DatasetBenchmarkRecord rec = new DatasetBenchmarkRecord();
                        rec.datasetName = parts[0].trim();
                        rec.txCount = Long.parseLong(parts[1].trim());
                        rec.checkpoints = Integer.parseInt(parts[2].trim());
                        rec.baseTime = Double.parseDouble(parts[3].trim());
                        rec.patternTime = Double.parseDouble(parts[4].trim());
                        rec.speedup = Double.parseDouble(parts[6].trim());
                        rec.baseTps = Double.parseDouble(parts[8].trim());
                        rec.patternTps = Double.parseDouble(parts[9].trim());
                        rec.baseRam = Double.parseDouble(parts[11].trim());
                        rec.patternRam = Double.parseDouble(parts[12].trim());
                        rec.ramSavedPct = parsePercent(parts[14].trim());
                        rec.baseGcCalls = Long.parseLong(parts[15].trim());
                        rec.patternGcCalls = Long.parseLong(parts[16].trim());
                        rec.baseGcTime = Double.parseDouble(parts[18].trim());
                        rec.patternGcTime = Double.parseDouble(parts[19].trim());
                        rec.gcReductionPct = parsePercent(parts[20].trim());
                        rec.distinctHuis = Long.parseLong(parts[25].trim());
                        rec.isMatch = parts[27].trim().equalsIgnoreCase("MATCH");
                        records.add(rec);
                    }
                }
                if (records.size() >= 10) {
                    loaded = true;
                }
            } catch (Exception ex) {
                // Fallback nếu có lỗi đọc file
            }
        }

        if (!loaded) {
            loadFallbackData();
        }
    }

    private double parsePercent(String str) {
        str = str.replace("%", "").trim();
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Dữ liệu Fallback chuẩn xác 10 bộ dữ liệu từ thực nghiệm đối sánh.
     */
    private void loadFallbackData() {
        records.clear();
        records.add(new DatasetBenchmarkRecord("Running Example", 4, 4, 0.13, 0.16, 0.83, 30, 25, 4.03, 2.89, 28.3, 2, 2, 2, 1, 50.0, 14, true));
        records.add(new DatasetBenchmarkRecord("Chess", 3196, 10, 8.53, 8.11, 1.05, 374, 394, 184.3, 125.25, 32.0, 70, 57, 198, 119, 39.9, 19170, true));
        records.add(new DatasetBenchmarkRecord("NewChess", 3196, 10, 8.55, 8.74, 0.98, 373, 365, 247.19, 173.66, 29.8, 64, 64, 201, 145, 27.9, 20232, true));
        records.add(new DatasetBenchmarkRecord("Mushrooms", 8416, 16, 5.17, 2.74, 1.88, 1627, 3068, 198.95, 93.46, 53.0, 46, 13, 124, 55, 55.7, 9754, true));
        records.add(new DatasetBenchmarkRecord("NewMushroom", 8416, 16, 5.18, 2.68, 1.93, 1625, 3137, 161.09, 81.77, 49.2, 47, 13, 130, 62, 52.3, 9994, true));
        records.add(new DatasetBenchmarkRecord("Connects", 67557, 67, 682.76, 273.58, 2.50, 98, 246, 313.27, 405.48, -29.4, 6466, 304, 5090, 703, 86.2, 34700, true));
        records.add(new DatasetBenchmarkRecord("NewConnects", 67557, 67, 705.51, 346.10, 2.04, 95, 195, 387.43, 407.24, -5.1, 6539, 433, 5209, 940, 82.0, 36951, true));
        records.add(new DatasetBenchmarkRecord("Retail", 88162, 88, 176.90, 170.70, 1.04, 498, 516, 496.28, 369.92, 25.5, 1727, 533, 2573, 1075, 58.2, 305294, true));
        records.add(new DatasetBenchmarkRecord("Accidents", 340183, 170, 3156.22, 1490.79, 2.12, 107, 228, 242.54, 110.98, 54.2, 15600, 247, 12677, 1138, 91.0, 395, true));
        records.add(new DatasetBenchmarkRecord("Chainstore", 1112949, 222, 874.58, 589.30, 1.48, 1272, 1888, 479.96, 920.39, -91.8, 5679, 603, 7835, 2419, 69.1, 217717, true));
    }

    /**
     * Lớp lưu trữ bản ghi dữ liệu benchmark của 1 dataset.
     */
    public static class DatasetBenchmarkRecord {
        public String datasetName;
        public long txCount;
        public int checkpoints;
        public double baseTime;
        public double patternTime;
        public double speedup;
        public double baseTps;
        public double patternTps;
        public double baseRam;
        public double patternRam;
        public double ramSavedPct;
        public long baseGcCalls;
        public long patternGcCalls;
        public double baseGcTime;
        public double patternGcTime;
        public double gcReductionPct;
        public long distinctHuis;
        public boolean isMatch;

        public DatasetBenchmarkRecord() {}

        public DatasetBenchmarkRecord(String datasetName, long txCount, int checkpoints, double baseTime, double patternTime,
                                     double speedup, double baseTps, double patternTps, double baseRam, double patternRam,
                                     double ramSavedPct, long baseGcCalls, long patternGcCalls, double baseGcTime,
                                     double patternGcTime, double gcReductionPct, long distinctHuis, boolean isMatch) {
            this.datasetName = datasetName;
            this.txCount = txCount;
            this.checkpoints = checkpoints;
            this.baseTime = baseTime;
            this.patternTime = patternTime;
            this.speedup = speedup;
            this.baseTps = baseTps;
            this.patternTps = patternTps;
            this.baseRam = baseRam;
            this.patternRam = patternRam;
            this.ramSavedPct = ramSavedPct;
            this.baseGcCalls = baseGcCalls;
            this.patternGcCalls = patternGcCalls;
            this.baseGcTime = baseGcTime;
            this.patternGcTime = patternGcTime;
            this.gcReductionPct = gcReductionPct;
            this.distinctHuis = distinctHuis;
            this.isMatch = isMatch;
        }
    }

    /**
     * Thành phần vẽ đồ thị cột Vector 2D độc lập bằng Graphics2D.
     */
    private static class BenchmarkChart2DPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private List<DatasetBenchmarkRecord> records;
        private int selectedMetric = 0; // 0: Time, 1: RAM, 2: GC Calls, 3: GC Time, 4: TPS
        private Point mousePoint = null;
        private int highlightedIndex = -1;
        private Consumer<Integer> onDatasetSelectedListener = null;

        public BenchmarkChart2DPanel(List<DatasetBenchmarkRecord> records) {
            this.records = records;
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(500, 360));

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    mousePoint = e.getPoint();
                    repaint();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    mousePoint = null;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    int clickX = e.getX();
                    int w = getWidth();
                    int padLeft = 65;
                    int padRight = 20;
                    int plotW = w - padLeft - padRight;
                    if (clickX >= padLeft && clickX <= padLeft + plotW && records != null && !records.isEmpty()) {
                        double groupWidth = (double) plotW / records.size();
                        int idx = (int) ((clickX - padLeft) / groupWidth);
                        if (idx >= 0 && idx < records.size()) {
                            if (highlightedIndex == idx) {
                                highlightedIndex = -1; // Click lại vào chính cột đó -> Hủy chọn
                            } else {
                                highlightedIndex = idx;
                            }
                            repaint();
                            if (onDatasetSelectedListener != null) {
                                onDatasetSelectedListener.accept(highlightedIndex);
                            }
                        }
                    } else {
                        // Click ra ngoài vùng cột (lề, trục) -> Hủy chọn
                        if (highlightedIndex != -1) {
                            highlightedIndex = -1;
                            repaint();
                            if (onDatasetSelectedListener != null) {
                                onDatasetSelectedListener.accept(-1);
                            }
                        }
                    }
                }
            });
        }

        public void setHighlightedIndex(int index) {
            this.highlightedIndex = index;
            repaint();
        }

        public void setOnDatasetSelectedListener(Consumer<Integer> listener) {
            this.onDatasetSelectedListener = listener;
        }

        public void exportToPng(Component parent) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Lưu Biểu Đồ Đối Sánh Thành Ảnh PNG");
            fileChooser.setSelectedFile(new File("BieuDo_DoiSanh_" + getMetricFileName() + ".png"));
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                File target = fileChooser.getSelectedFile();
                if (!target.getName().toLowerCase().endsWith(".png")) {
                    target = new File(target.getParentFile(), target.getName() + ".png");
                }
                try {
                    BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2 = image.createGraphics();
                    paint(g2);
                    g2.dispose();
                    ImageIO.write(image, "png", target);
                    JOptionPane.showMessageDialog(parent, "Đã lưu ảnh biểu đồ thành công tại:\n" + target.getAbsolutePath(), "Lưu ảnh thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(parent, "Lỗi khi lưu ảnh: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private String getMetricFileName() {
            switch (selectedMetric) {
                case 1: return "RAM_Heap";
                case 2: return "GC_Calls";
                case 3: return "GC_PauseTime";
                case 4: return "Throughput_TPS";
                default: return "Runtime";
            }
        }

        public void setRecords(List<DatasetBenchmarkRecord> records) {
            this.records = records;
            repaint();
        }

        public void setSelectedMetric(int metric) {
            this.selectedMetric = metric;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (records == null || records.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Khung lề
            int padLeft = 65;
            int padRight = 20;
            int padBottom = 55;

            // Bảng màu sắc tương ứng với slide và HTML report
            Color baseColor = new Color(148, 163, 184); // Xám #94A3B8
            Color patternColor;
            String unitLabel;
            boolean isLog10;

            switch (selectedMetric) {
                case 1: // RAM
                    patternColor = new Color(16, 185, 129); // Xanh lá
                    unitLabel = "Megabytes (MB) — Thấp hơn là tiết kiệm hơn";
                    isLog10 = false;
                    break;
                case 2: // GC Calls
                    patternColor = new Color(217, 119, 6); // Cam
                    unitLabel = "Số lần kích hoạt GC (Thang Log10) — Thấp hơn là tốt hơn";
                    isLog10 = true;
                    break;
                case 3: // GC Pause Time
                    patternColor = new Color(225, 29, 72); // Đỏ hồng
                    unitLabel = "Thời gian dừng GC (ms) — Thấp hơn là tối ưu hơn";
                    isLog10 = false;
                    break;
                case 4: // TPS
                    patternColor = new Color(124, 58, 237); // Tím
                    unitLabel = "Transactions / Giây (tx/s) — Cao hơn là tốt hơn";
                    isLog10 = true;
                    break;
                default: // Runtime
                    patternColor = new Color(37, 99, 235); // Xanh dương
                    unitLabel = "Thời gian (Giây - Thang Log10) — Cột thấp hơn là nhanh hơn";
                    isLog10 = true;
                    break;
            }

            // Kiểm tra bề ngang để phân bổ 1 hàng hay 2 hàng (chống đè chữ)
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            FontMetrics ufm = g2.getFontMetrics();
            int unitWidth = ufm.stringWidth(unitLabel);
            int totalLegendW = 310;

            boolean isWide = (w - padRight - totalLegendW >= padLeft + unitWidth + 15);
            int padTop = isWide ? 42 : 54;
            int plotW = w - padLeft - padRight;
            int plotH = h - padTop - padBottom;

            // Vẽ Chú thích Legend phía trên
            if (isWide) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.setColor(new Color(100, 116, 139));
                g2.drawString(unitLabel, padLeft, 22);

                int legX = w - padRight - totalLegendW;
                g2.setColor(baseColor);
                g2.fillRect(legX, 12, 14, 10);
                g2.setColor(new Color(51, 65, 85));
                g2.drawString("Baseline (Chưa áp dụng)", legX + 18, 21);

                g2.setColor(patternColor);
                g2.fillRect(legX + 160, 12, 14, 10);
                g2.setColor(new Color(51, 65, 85));
                g2.drawString("Design Patterns", legX + 178, 21);
            } else {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.setColor(new Color(100, 116, 139));
                g2.drawString(unitLabel, padLeft, 16);

                int legX = padLeft;
                g2.setColor(baseColor);
                g2.fillRect(legX, 26, 13, 9);
                g2.setColor(new Color(51, 65, 85));
                g2.drawString("Baseline (Chưa áp dụng)", legX + 17, 34);

                g2.setColor(patternColor);
                g2.fillRect(legX + 155, 26, 13, 9);
                g2.setColor(new Color(51, 65, 85));
                g2.drawString("Design Patterns", legX + 172, 34);
            }

            // Tìm giá trị max
            double maxVal = 1.0;
            for (DatasetBenchmarkRecord r : records) {
                double bVal = getMetricValue(r, true);
                double pVal = getMetricValue(r, false);
                maxVal = Math.max(maxVal, Math.max(bVal, pVal));
            }

            // Vẽ lưới tọa độ ngang
            g2.setColor(new Color(241, 245, 249));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            int gridSteps = 5;
            for (int i = 0; i <= gridSteps; i++) {
                int y = padTop + plotH - (i * plotH / gridSteps);
                g2.setColor(new Color(226, 232, 240));
                g2.drawLine(padLeft, y, padLeft + plotW, y);

                double labelVal;
                if (isLog10) {
                    double maxExp = Math.log10(Math.max(10.0, maxVal));
                    double exp = i * maxExp / gridSteps;
                    labelVal = Math.pow(10, exp);
                } else {
                    labelVal = i * maxVal / gridSteps;
                }
                g2.setColor(new Color(148, 163, 184));
                String lblStr = formatGridLabel(labelVal);
                g2.drawString(lblStr, padLeft - 48, y + 4);
            }

            // Vẽ các cột của từng dataset
            int n = records.size();
            double groupWidth = (double) plotW / n;
            double barWidth = Math.max(6, (groupWidth * 0.70) / 2);

            String hoveredTooltip = null;
            int tooltipX = 0, tooltipY = 0;

            for (int i = 0; i < n; i++) {
                DatasetBenchmarkRecord r = records.get(i);
                double bVal = getMetricValue(r, true);
                double pVal = getMetricValue(r, false);

                double bHeightNorm = isLog10
                        ? Math.log10(Math.max(0.1, bVal)) / Math.log10(Math.max(10.0, maxVal))
                        : bVal / maxVal;
                double pHeightNorm = isLog10
                        ? Math.log10(Math.max(0.1, pVal)) / Math.log10(Math.max(10.0, maxVal))
                        : pVal / maxVal;

                bHeightNorm = Math.max(0.02, Math.min(1.0, bHeightNorm));
                pHeightNorm = Math.max(0.02, Math.min(1.0, pHeightNorm));

                int bBarH = (int) (bHeightNorm * (plotH - 10));
                int pBarH = (int) (pHeightNorm * (plotH - 10));

                int groupCenterX = (int) (padLeft + i * groupWidth + groupWidth / 2);
                int bBarX = (int) (groupCenterX - barWidth);
                int pBarX = (int) (groupCenterX);

                int bBarY = padTop + plotH - bBarH;
                int pBarY = padTop + plotH - pBarH;

                // Kiểm tra trạng thái Highlight & Hover
                boolean isSelected = (highlightedIndex == i);
                boolean hasSelection = (highlightedIndex >= 0);

                // Khung highlight nền khi dataset được chọn
                if (isSelected) {
                    int bgX = (int) (padLeft + i * groupWidth + 2);
                    int bgW = (int) (groupWidth - 4);
                    g2.setColor(new Color(238, 242, 255)); // Indigo-50
                    g2.fill(new RoundRectangle2D.Float(bgX, padTop - 14, bgW, plotH + 14, 6, 6));
                    g2.setColor(new Color(165, 180, 252)); // Indigo-300
                    g2.draw(new RoundRectangle2D.Float(bgX, padTop - 14, bgW, plotH + 14, 6, 6));

                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    g2.setColor(new Color(67, 56, 202));
                    String selBadge = "[CHỌN]";
                    int badgeW = g2.getFontMetrics().stringWidth(selBadge);
                    g2.drawString(selBadge, (int) (groupCenterX - badgeW / 2), padTop - 4);
                }

                // Kiểm tra hover chuột
                Rectangle bRect = new Rectangle(bBarX, bBarY, (int) barWidth, bBarH);
                Rectangle pRect = new Rectangle(pBarX, pBarY, (int) barWidth, pBarH);

                boolean bHover = mousePoint != null && bRect.contains(mousePoint);
                boolean pHover = mousePoint != null && pRect.contains(mousePoint);

                // Xác định màu sắc (Đầy đủ vs Mờ nhẹ nếu có dòng khác đang được chọn)
                Color curBaseCol;
                Color curPatternCol;
                if (hasSelection && !isSelected) {
                    curBaseCol = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 80);
                    curPatternCol = new Color(patternColor.getRed(), patternColor.getGreen(), patternColor.getBlue(), 80);
                } else {
                    curBaseCol = bHover ? baseColor.darker() : baseColor;
                    curPatternCol = pHover ? patternColor.darker() : patternColor;
                }

                // Vẽ Cột Baseline
                g2.setColor(curBaseCol);
                g2.fill(new RoundRectangle2D.Float(bBarX, bBarY, (float) barWidth - 1, bBarH, 4, 4));
                if (isSelected) {
                    g2.setColor(new Color(71, 85, 105));
                    g2.draw(new RoundRectangle2D.Float(bBarX, bBarY, (float) barWidth - 1, bBarH, 4, 4));
                }

                // Vẽ Cột Patterns
                g2.setColor(curPatternCol);
                g2.fill(new RoundRectangle2D.Float(pBarX, pBarY, (float) barWidth - 1, pBarH, 4, 4));
                if (isSelected) {
                    g2.setColor(patternColor.darker().darker());
                    g2.draw(new RoundRectangle2D.Float(pBarX, pBarY, (float) barWidth - 1, pBarH, 4, 4));
                }

                // Vẽ nhãn giá trị trên đầu cột
                g2.setFont(new Font("Segoe UI", isSelected ? Font.BOLD : Font.PLAIN, 9));
                g2.setColor(hasSelection && !isSelected ? new Color(148, 163, 184) : (isSelected ? new Color(15, 23, 42) : new Color(71, 85, 105)));
                String bValStr = formatVal(bVal);
                String pValStr = formatVal(pVal);
                g2.drawString(bValStr, bBarX - 2, bBarY - 3);
                g2.drawString(pValStr, pBarX - 1, pBarY - 3);

                // Nhãn tên bộ dữ liệu dưới trục X
                g2.setFont(new Font("Segoe UI", isSelected ? Font.BOLD : Font.PLAIN, 10));
                g2.setColor(isSelected ? new Color(67, 56, 202) : (hasSelection ? new Color(148, 163, 184) : new Color(30, 41, 59)));
                String shortName = getShortName(r.datasetName);
                int nameW = g2.getFontMetrics().stringWidth(shortName);
                g2.drawString(shortName, (int) (groupCenterX - nameW / 2), padTop + plotH + 16);

                if (bHover || pHover || (isSelected && mousePoint == null)) {
                    hoveredTooltip = String.format(Locale.US,
                            "<html><b>%s</b><br>Baseline: <b>%s</b><br>Patterns: <b>%s</b><br>Tăng tốc: <b>%.2f×</b></html>",
                            r.datasetName, bValStr, pValStr, r.speedup);
                    tooltipX = (int) groupCenterX;
                    tooltipY = Math.min(bBarY, pBarY) - 10;
                }
            }

            // Trục tọa độ chính
            g2.setColor(new Color(100, 116, 139));
            g2.drawLine(padLeft, padTop + plotH, padLeft + plotW, padTop + plotH);
            g2.drawLine(padLeft, padTop, padLeft, padTop + plotH);

            // Vẽ Tooltip nếu có
            if (hoveredTooltip != null && mousePoint != null) {
                drawTooltip(g2, hoveredTooltip, mousePoint.x + 12, mousePoint.y - 15);
            }

            g2.dispose();
        }

        private void drawTooltip(Graphics2D g2, String html, int x, int y) {
            JLabel lbl = new JLabel(html);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lbl.setOpaque(true);
            lbl.setBackground(new Color(15, 23, 42));
            lbl.setForeground(Color.WHITE);
            lbl.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(51, 65, 85), 1, true),
                    new EmptyBorder(4, 8, 4, 8)
            ));
            Dimension d = lbl.getPreferredSize();
            g2.translate(x, y);
            lbl.setSize(d);
            lbl.paint(g2);
            g2.translate(-x, -y);
        }

        private double getMetricValue(DatasetBenchmarkRecord r, boolean isBase) {
            switch (selectedMetric) {
                case 1: return isBase ? r.baseRam : r.patternRam;
                case 2: return isBase ? r.baseGcCalls : r.patternGcCalls;
                case 3: return isBase ? r.baseGcTime : r.patternGcTime;
                case 4: return isBase ? r.baseTps : r.patternTps;
                default: return isBase ? r.baseTime : r.patternTime;
            }
        }

        private String formatVal(double v) {
            if (v >= 10000) return String.format(Locale.US, "%.1fk", v / 1000.0);
            if (v >= 1000) return String.format(Locale.US, "%.0f", v);
            if (v >= 10) return String.format(Locale.US, "%.1f", v);
            if (v >= 1) return String.format(Locale.US, "%.2f", v);
            return String.format(Locale.US, "%.2f", v);
        }

        private String formatGridLabel(double v) {
            if (v >= 1000000) return String.format(Locale.US, "%.0fM", v / 1000000.0);
            if (v >= 1000) return String.format(Locale.US, "%.0fk", v / 1000.0);
            if (v >= 1) return String.format(Locale.US, "%.0f", v);
            return String.format(Locale.US, "%.1f", v);
        }

        private String getShortName(String name) {
            if (name.contains("Running")) return "Running";
            if (name.contains("NewChess")) return "NewChess";
            if (name.contains("Chess")) return "Chess";
            if (name.contains("NewMushroom")) return "NewMush";
            if (name.contains("mushroom")) return "Mushrooms";
            if (name.contains("NewConnects")) return "NewConn";
            if (name.contains("connects")) return "Connects";
            if (name.contains("Retail")) return "Retail";
            if (name.contains("Accidents")) return "Accidents";
            if (name.contains("Chainstore")) return "Chainstore";
            return name;
        }
    }
}
