package huddtds.demo;

import huddtds.application.SimulationService;
import huddtds.application.DatasetService;
import huddtds.application.event.EventType;
import huddtds.application.event.SimulationEvent;
import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;
import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * Giao diện chính HUDD-TDS GUI (Java Swing).
 * Cung cấp tính linh hoạt tối đa trong khi giữ nguyên 100% bản chất thuật toán và mô hình toán học:
 * - Chọn từ danh mục 18 dataset chuẩn hoặc duyệt tệp tùy chỉnh (Custom File Picker).
 * - Giới hạn số lượng giao dịch thử nghiệm nhanh (Max Transactions limit).
 * - Điều khiển luồng linh hoạt: Phát luồng (RUN), Tạm dừng/Tiếp tục (PAUSE/RESUME), Dừng (STOP), Đặt lại (RESET).
 * - Điều chỉnh tốc độ phát luồng thời gian thực qua thanh trượt độ trễ (Stream Speed/Delay Slider).
 * - Tìm kiếm, lọc và sắp xếp tương tác trên Bảng HUI và Bảng Drift.
 * - Đồ thị trực quan với khả năng chuyển đổi metric (DISHS / HUI Count), đánh dấu điểm Drift và Hover Tooltip.
 * - Xuất báo cáo linh hoạt ra tệp CSV (HUI Log, Drift Log, hoặc Toàn bộ bản tóm tắt).
 */
public class HUDD_TDS_GUI extends JFrame {
    private static final long serialVersionUID = 1L;

    // Quản lý Dữ liệu
    private final DatasetService datasetService;
    private File customTransactionFile = null;
    private File customInvestmentFile = null;

    // Các thành phần cấu hình đầu vào
    private final JComboBox<String> cbDatasets;
    private final JButton btnBrowseCustom;
    private final JTextField txtMinUtil;
    private final JTextField txtInterval;
    private final JTextField txtWindowSize;
    private final JTextField txtAlpha;
    private final JTextField txtMaxPattern;
    private final JTextField txtMaxTransactions;

    // Các thành phần điều khiển phát luồng
    private final JSlider sliderDelay;
    private final JLabel lblDelayValue;
    private final JButton btnValidate;
    private final JButton btnRun;
    private final JButton btnPause;
    private final JButton btnStop;
    private final JButton btnReset;
    private final JButton btnExport;
    private final JCheckBox chkTraceEachTransaction;

    // Thanh tiến trình & Thẻ thống kê
    private final JProgressBar progressBar;
    private final JLabel lblCurrentTID;
    private final JLabel lblHuiCount;
    private final JLabel lblGlobalDistance;
    private final JLabel lblStatus;
    private final JLabel lblLocalDrift;
    private final JLabel lblSpeed;

    // Bảng và Lọc
    private final DefaultTableModel huiTableModel;
    private final JTable huiTable;
    private final TableRowSorter<DefaultTableModel> huiSorter;
    private final JTextField txtSearchHui;
    private final JCheckBox chkAutoScrollHui;

    private final DefaultTableModel driftTableModel;
    private final JTable driftTable;
    private final TableRowSorter<DefaultTableModel> driftSorter;
    private final JComboBox<String> cbFilterDrift;

    // Dữ liệu nhập tay & Thống kê tổng quan
    private final JTextArea manualInputArea;
    private final JTextArea summaryStatsArea;

    // Đồ thị trực quan & Chi tiết tính toán
    private final ChartPanel chartPanel;
    private final JComboBox<String> cbChartMetric;
    private final JTextArea calculationDetails;
    private final JTextArea processLogArea;

    // Luồng ngầm
    private SimulationWorker currentWorker;
    private File currentProcessLogFile;
    private volatile int currentDelayMs = 0;

    // Thống kê tổng hợp
    private int totalCheckpointsCount = 0;
    private int totalGlobalDriftsCount = 0;
    private int totalLocalDriftsCount = 0;

    public HUDD_TDS_GUI() {
        super("HUDD-TDS - Hệ thống giám sát trôi dạt độ lợi (Utility Drift Detection)");
        this.datasetService = new DatasetService();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(6, 6));

        // =========================================================================
        // 1. THANH ĐIỀU KHIỂN & CẤU HÌNH PHÍA BẮC (NORTH PANEL)
        // =========================================================================
        JPanel northPanel = new JPanel(new BorderLayout(4, 4));
        northPanel.setBorder(new EmptyBorder(6, 8, 2, 8));

        // Khung trên: Tham số & Bộ dữ liệu
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Cấu hình Tham số & Chọn Bộ dữ liệu",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Dòng 1: Dataset, Duyệt tệp, MinUtil, Interval, Window, Alpha, MaxLen, Giới hạn Tx
        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Bộ dữ liệu:"), gbc);

        gbc.gridx = 1;
        List<String> datasetOptions = new ArrayList<>(datasetService.getDatasetNames());
        datasetOptions.add(0, "Running Example (Mẫu)");
        datasetOptions.add("[+ Duyệt tệp ngoài...]");
        cbDatasets = new JComboBox<>(datasetOptions.toArray(String[]::new));
        cbDatasets.setPreferredSize(new Dimension(175, 25));
        configPanel.add(cbDatasets, gbc);

        gbc.gridx = 2;
        btnBrowseCustom = new JButton("📁 Duyệt...");
        btnBrowseCustom.setToolTipText("Chọn tệp transactions.txt và investment_table.txt từ ổ đĩa máy tính");
        configPanel.add(btnBrowseCustom, gbc);

        gbc.gridx = 3;
        configPanel.add(new JLabel("MinUtil:"), gbc);
        gbc.gridx = 4;
        txtMinUtil = new JTextField("15.0", 7);
        configPanel.add(txtMinUtil, gbc);

        gbc.gridx = 5;
        configPanel.add(new JLabel("Interval:"), gbc);
        gbc.gridx = 6;
        txtInterval = new JTextField("1", 4);
        configPanel.add(txtInterval, gbc);

        gbc.gridx = 7;
        configPanel.add(new JLabel("Window:"), gbc);
        gbc.gridx = 8;
        txtWindowSize = new JTextField("2", 4);
        configPanel.add(txtWindowSize, gbc);

        gbc.gridx = 9;
        configPanel.add(new JLabel("Alpha:"), gbc);
        gbc.gridx = 10;
        txtAlpha = new JTextField("0.10", 4);
        configPanel.add(txtAlpha, gbc);

        gbc.gridx = 11;
        configPanel.add(new JLabel("Max Len:"), gbc);
        gbc.gridx = 12;
        txtMaxPattern = new JTextField("3", 3);
        configPanel.add(txtMaxPattern, gbc);

        gbc.gridx = 13;
        configPanel.add(new JLabel("Max Tx:"), gbc);
        gbc.gridx = 14;
        txtMaxTransactions = new JTextField("0", 5);
        txtMaxTransactions.setToolTipText("0 = Toàn bộ giao dịch trong tệp; hoặc nhập số cụ thể để chạy thử nghiệm nhanh (vd: 500, 1000)");
        configPanel.add(txtMaxTransactions, gbc);

        // Dòng 2: Điều khiển phát luồng, Tốc độ (Slider) và Nút thao tác
        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        controlRow.add(new JLabel("Tốc độ trễ:"));
        sliderDelay = new JSlider(0, 200, 0);
        sliderDelay.setPreferredSize(new Dimension(130, 24));
        sliderDelay.setToolTipText("0 ms: Chạy nhanh tối đa (Batch). > 0 ms: Làm chậm để quan sát từng giao dịch/checkpoint");
        controlRow.add(sliderDelay);

        lblDelayValue = new JLabel("0 ms (Tối đa)");
        lblDelayValue.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDelayValue.setPreferredSize(new Dimension(80, 20));
        controlRow.add(lblDelayValue);

        chkTraceEachTransaction = new JCheckBox("Log từng giao dịch");
        chkTraceEachTransaction.setToolTipText("Ghi chi tiết từng giao dịch vào nhật ký; có thể tạo tệp lớn với dataset lớn");

        btnValidate = new JButton("Kiểm tra Data");
        btnValidate.setBackground(new Color(23, 162, 184));
        btnValidate.setForeground(Color.WHITE);
        controlRow.add(btnValidate);

        btnRun = new JButton("▶ Phát luồng (RUN)");
        btnRun.setBackground(new Color(40, 167, 69));
        btnRun.setForeground(Color.WHITE);
        btnRun.setFont(new Font("Segoe UI", Font.BOLD, 12));
        controlRow.add(btnRun);

        btnPause = new JButton("⏸ Tạm dừng");
        btnPause.setBackground(new Color(243, 156, 18));
        btnPause.setForeground(Color.WHITE);
        btnPause.setEnabled(false);
        controlRow.add(btnPause);

        btnStop = new JButton("⏹ Dừng (STOP)");
        btnStop.setBackground(new Color(220, 53, 69));
        btnStop.setForeground(Color.WHITE);
        btnStop.setEnabled(false);
        controlRow.add(btnStop);

        btnReset = new JButton("🔄 Đặt lại");
        controlRow.add(btnReset);

        btnExport = new JButton("💾 Xuất báo cáo ▼");
        btnExport.setToolTipText("Xuất Nhật ký HUI, Nhật ký Drift hoặc Báo cáo thống kê");
        controlRow.add(btnExport);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 15;
        configPanel.add(controlRow, gbc);

        northPanel.add(configPanel, BorderLayout.NORTH);

        // THANH TIẾN TRÌNH & THẺ THỐNG KÊ (METRICS)
        JPanel statusContainer = new JPanel(new BorderLayout(4, 4));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Sẵn sàng phát luồng");
        progressBar.setPreferredSize(new Dimension(800, 20));
        statusContainer.add(progressBar, BorderLayout.NORTH);

        JPanel metricsPanel = new JPanel(new GridLayout(1, 6, 6, 6));
        metricsPanel.setBorder(new EmptyBorder(3, 0, 3, 0));

        lblCurrentTID = createMetricCard(metricsPanel, "TID HIỆN TẠI", "T_0", new Color(225, 238, 254));
        lblHuiCount = createMetricCard(metricsPanel, "SỐ LƯỢNG HUI", "0", new Color(220, 248, 225));
        lblGlobalDistance = createMetricCard(metricsPanel, "TOTAL DISHS", "0.0000", new Color(254, 249, 215));
        lblStatus = createMetricCard(metricsPanel, "GLOBAL DRIFT", "ỔN ĐỊNH", new Color(212, 237, 218));
        lblLocalDrift = createMetricCard(metricsPanel, "LOCAL DRIFT", "ỔN ĐỊNH", new Color(255, 238, 210));
        lblSpeed = createMetricCard(metricsPanel, "TỐC ĐỘ (TX/S)", "0", new Color(237, 233, 254));

        statusContainer.add(metricsPanel, BorderLayout.CENTER);
        northPanel.add(statusContainer, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);

        // =========================================================================
        // 2. KHU VỰC TRUNG TÂM (CENTER): TABS BẢNG & BIỂU ĐỒ (SPLIT PANE)
        // =========================================================================
        JTabbedPane tabbedPane = new JTabbedPane();

        // TAB 1: BẢNG NHẬT KÝ HUI (Có tìm kiếm & bộ lọc)
        JPanel huiPanel = new JPanel(new BorderLayout(4, 4));
        JPanel huiFilterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        huiFilterBar.add(new JLabel("🔍 Lọc Itemset / Mục:"));
        txtSearchHui = new JTextField(14);
        huiFilterBar.add(txtSearchHui);

        chkAutoScrollHui = new JCheckBox("Tự động cuộn theo dòng mới", true);
        huiFilterBar.add(chkAutoScrollHui);

        JButton btnClearFilterHui = new JButton("Xóa lọc");
        btnClearFilterHui.addActionListener(e -> txtSearchHui.setText(""));
        huiFilterBar.add(btnClearFilterHui);

        huiPanel.add(huiFilterBar, BorderLayout.NORTH);

        String[] huiColumns = {"Checkpoint TID", "Tập mục (Itemset)", "TU giao dịch",
                "Độ lợi suy giảm (Faded Utility)", "Vector itemset", "Khoảng cách D_mo"};
        huiTableModel = new DefaultTableModel(huiColumns, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        huiTable = new JTable(huiTableModel);
        huiTable.setRowHeight(22);
        huiSorter = new TableRowSorter<>(huiTableModel);
        huiTable.setRowSorter(huiSorter);
        huiPanel.add(new JScrollPane(huiTable), BorderLayout.CENTER);
        tabbedPane.addTab("Nhật ký HUI", huiPanel);

        // TAB 2: NHẬT KÝ TRÔI DẠT (DRIFT LOG) (Có bộ lọc)
        JPanel driftPanel = new JPanel(new BorderLayout(4, 4));
        JPanel driftFilterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        driftFilterBar.add(new JLabel("Lọc loại Drift:"));
        cbFilterDrift = new JComboBox<>(new String[]{"Tất cả loại Drift", "Chỉ Global Drift", "Chỉ Local Drift"});
        driftFilterBar.add(cbFilterDrift);
        driftPanel.add(driftFilterBar, BorderLayout.NORTH);

        String[] driftColumns = {"Checkpoint TID", "Loại Drift", "Giá trị Thống kê", "Ngưỡng Epsilon", "Chi tiết / Itemsets bị ảnh hưởng"};
        driftTableModel = new DefaultTableModel(driftColumns, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        driftTable = new JTable(driftTableModel);
        driftTable.setRowHeight(22);
        driftSorter = new TableRowSorter<>(driftTableModel);
        driftTable.setRowSorter(driftSorter);
        driftPanel.add(new JScrollPane(driftTable), BorderLayout.CENTER);
        tabbedPane.addTab("Nhật ký Drift", driftPanel);

        // TAB 3: NHẬP TAY (MANUAL DATA)
        JPanel manualPanel = new JPanel(new BorderLayout(4, 4));
        manualInputArea = new JTextArea("""
            a:1 c:1 d:1
            a:2 c:6 e:2 g:5
            b:4 c:3 d:3 e:1
            b:2 c:3 e:2 g:2""");
        manualInputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        manualPanel.add(new JScrollPane(manualInputArea), BorderLayout.CENTER);
        tabbedPane.addTab("Dữ liệu nhập tay", manualPanel);

        // TAB 4: TỔNG QUAN & THỐNG KÊ (SUMMARY STATS)
        summaryStatsArea = new JTextArea();
        summaryStatsArea.setEditable(false);
        summaryStatsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        summaryStatsArea.setText("Chưa có thống kê thực nghiệm. Nhấn 'Phát luồng (RUN)' để tiến hành.");
        tabbedPane.addTab("Tổng quan Thống kê", new JScrollPane(summaryStatsArea));

        processLogArea = new JTextArea();
        processLogArea.setEditable(false);
        processLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        processLogArea.setLineWrap(true);
        processLogArea.setWrapStyleWord(true);
        JPanel processLogPanel = new JPanel(new BorderLayout(4, 4));
        JPanel processLogControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        processLogControls.add(chkTraceEachTransaction);
        processLogPanel.add(processLogControls, BorderLayout.NORTH);
        processLogPanel.add(new JScrollPane(processLogArea), BorderLayout.CENTER);
        tabbedPane.addTab("Nhật ký tiến trình", processLogPanel);

        // BIỂU ĐỒ NÂNG CAO (CHART PANEL)
        JPanel chartContainer = new JPanel(new BorderLayout(3, 3));
        chartContainer.setBorder(BorderFactory.createTitledBorder("Đồ thị biến thiên trực quan"));

        JPanel chartControlBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        chartControlBar.add(new JLabel("Chỉ số:"));
        cbChartMetric = new JComboBox<>(new String[]{"Khoảng cách toàn cục (DISHS)", "Số lượng HUI"});
        cbChartMetric.setPreferredSize(new Dimension(190, 24));
        chartControlBar.add(cbChartMetric);

        chartContainer.add(chartControlBar, BorderLayout.NORTH);

        chartPanel = new ChartPanel();
        chartContainer.add(chartPanel, BorderLayout.CENTER);
        chartContainer.setPreferredSize(new Dimension(460, 300));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedPane, chartContainer);
        splitPane.setResizeWeight(0.60);
        add(splitPane, BorderLayout.CENTER);

        // =========================================================================
        // 3. CHI TIẾT TOÁN HỌC & KIỂM ĐỊNH PHÍA NAM (SOUTH PANEL)
        // =========================================================================
        calculationDetails = new JTextArea(3, 80);
        calculationDetails.setEditable(false);
        calculationDetails.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        calculationDetails.setText("Hệ thống sẵn sàng. Chọn Dataset, thiết lập tham số và nhấn 'Phát luồng (RUN)'.");
        JScrollPane detailsScrollPane = new JScrollPane(calculationDetails);
        detailsScrollPane.setBorder(BorderFactory.createTitledBorder("Chi tiết tính toán & Kiểm định thống kê (U_drift, V, Epsilon, Delta D)"));
        detailsScrollPane.setPreferredSize(new Dimension(1000, 85));
        add(detailsScrollPane, BorderLayout.SOUTH);

        // =========================================================================
        // 4. LẮNG NGHE SỰ KIỆN GIAO DIỆN
        // =========================================================================
        cbDatasets.addActionListener(e -> onDatasetChanged());
        btnBrowseCustom.addActionListener(e -> onBrowseCustomFiles());
        btnValidate.addActionListener(e -> onValidateDataset());
        btnRun.addActionListener(e -> onRunSimulation());
        btnPause.addActionListener(e -> onTogglePause());
        btnStop.addActionListener(e -> onStopSimulation());
        btnReset.addActionListener(e -> onReset());
        btnExport.addActionListener(e -> onShowExportMenu());

        // Thanh trượt tốc độ
        sliderDelay.addChangeListener(e -> {
            currentDelayMs = sliderDelay.getValue();
            if (currentDelayMs == 0) {
                lblDelayValue.setText("0 ms (Tối đa)");
            } else {
                lblDelayValue.setText(currentDelayMs + " ms");
            }
        });

        // Đổi chế độ vẽ trên ChartPanel
        cbChartMetric.addActionListener(e -> {
            int idx = cbChartMetric.getSelectedIndex();
            if (idx == 0) {
                chartPanel.setMetricMode(ChartPanel.MetricMode.GLOBAL_DISTANCE);
            } else {
                chartPanel.setMetricMode(ChartPanel.MetricMode.HUI_COUNT);
            }
        });

        // Lọc bảng HUI theo từ khóa
        txtSearchHui.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyHuiFilter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyHuiFilter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyHuiFilter(); }
        });

        // Lọc bảng Drift theo loại
        cbFilterDrift.addActionListener(e -> applyDriftFilter());

        // Nạp tham số mặc định theo dataset ban đầu
        onDatasetChanged();
    }

    private JLabel createMetricCard(JPanel parent, String title, String initialValue, Color bgColor) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 220), 1),
                new EmptyBorder(3, 4, 3, 4)
        ));
        card.setBackground(bgColor);

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitle.setForeground(new Color(70, 80, 90));

        JLabel lblValue = new JLabel(initialValue, SwingConstants.CENTER);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 13));

        card.add(lblTitle);
        card.add(lblValue);
        parent.add(card);
        return lblValue;
    }

    private void applyHuiFilter() {
        String text = txtSearchHui.getText().trim();
        if (text.isEmpty()) {
            huiSorter.setRowFilter(null);
        } else {
            huiSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 1));
        }
    }

    private void applyDriftFilter() {
        switch (cbFilterDrift.getSelectedIndex()) {
            case 1 -> driftSorter.setRowFilter(RowFilter.regexFilter("GLOBAL DRIFT", 1));
            case 2 -> driftSorter.setRowFilter(RowFilter.regexFilter("LOCAL DRIFT", 1));
            default -> driftSorter.setRowFilter(null);
        }
    }

    private void onBrowseCustomFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn tệp giao dịch (transactions.txt)");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            customTransactionFile = chooser.getSelectedFile();

            // Tự động tìm investment_table.txt trong cùng thư mục
            File dir = customTransactionFile.getParentFile();
            File potentialInv = new File(dir, "investment_table.txt");
            if (potentialInv.exists()) {
                customInvestmentFile = potentialInv;
            } else {
                int res = JOptionPane.showConfirmDialog(this,
                        "Bạn có muốn chọn tệp bảng đầu tư (investment_table.txt) tương ứng không?",
                        "Chọn bảng đầu tư", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    JFileChooser invChooser = new JFileChooser(dir);
                    invChooser.setDialogTitle("Chọn tệp bảng đầu tư (investment_table.txt)");
                    if (invChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                        customInvestmentFile = invChooser.getSelectedFile();
                    }
                }
            }

            String customLabel = "[Tùy chỉnh: " + customTransactionFile.getName() + "]";
            DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) cbDatasets.getModel();
            boolean exists = false;
            for (int i = 0; i < model.getSize(); i++) {
                if (model.getElementAt(i).equals(customLabel)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                model.addElement(customLabel);
            }
            cbDatasets.setSelectedItem(customLabel);

            JOptionPane.showMessageDialog(this,
                    "Đã nạp tệp giao dịch tùy chỉnh:\n" + customTransactionFile.getAbsolutePath() + "\n"
                            + (customInvestmentFile != null ? "Bảng đầu tư: " + customInvestmentFile.getAbsolutePath() : "Không sử dụng bảng đầu tư ngoại vi."),
                    "Đã nạp tệp thành công", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onDatasetChanged() {
        String selected = (String) cbDatasets.getSelectedItem();
        if (selected == null) return;

        if (selected.equals("[+ Duyệt tệp ngoài...]")) {
            onBrowseCustomFiles();
            return;
        }

        String lower = selected.toLowerCase();
        if (lower.contains("running example")) {
            txtMinUtil.setText("15.0");
            txtInterval.setText("1");
            txtWindowSize.setText("2");
            txtAlpha.setText("0.10");
            txtMaxPattern.setText("4");
            txtMaxTransactions.setText("0");
        } else if (lower.contains("chess")) {
            txtMinUtil.setText("2000000");
            txtInterval.setText("300");
            txtWindowSize.setText("500");
            txtAlpha.setText("0.05");
            txtMaxPattern.setText("3");
            txtMaxTransactions.setText("0");
        } else if (lower.contains("mushroom")) {
            txtMinUtil.setText("2500000");
            txtInterval.setText("500");
            txtWindowSize.setText("1000");
            txtAlpha.setText("0.05");
            txtMaxPattern.setText("3");
            txtMaxTransactions.setText("0");
        } else if (lower.contains("connect")) {
            txtMinUtil.setText("3000000");
            txtInterval.setText("1000");
            txtWindowSize.setText("2000");
            txtAlpha.setText("0.05");
            txtMaxPattern.setText("3");
            txtMaxTransactions.setText("0");
        } else if (lower.contains("retail")) {
            txtMinUtil.setText("100000");
            txtInterval.setText("1000");
            txtWindowSize.setText("2000");
            txtAlpha.setText("0.05");
            txtMaxPattern.setText("3");
            txtMaxTransactions.setText("0");
        } else if (lower.contains("accident")) {
            txtMinUtil.setText("5000000");
            txtInterval.setText("2000");
            txtWindowSize.setText("5000");
            txtAlpha.setText("0.05");
            txtMaxPattern.setText("3");
            txtMaxTransactions.setText("0");
        } else if (lower.contains("chainstore")) {
            txtMinUtil.setText("500000");
            txtInterval.setText("5000");
            txtWindowSize.setText("10000");
            txtAlpha.setText("0.05");
            txtMaxPattern.setText("3");
            txtMaxTransactions.setText("0");
        }
    }

    private void onValidateDataset() {
        String selected = (String) cbDatasets.getSelectedItem();
        if (selected == null || selected.contains("Running Example")) {
            JOptionPane.showMessageDialog(this,
                    "Dữ liệu mẫu Running Example không cần kiểm định tệp tin.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DatasetService.ValidationSummary report =
                datasetService.validate(selected, customTransactionFile, 5000);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("KẾT QUẢ KIỂM ĐỊNH DATASET: %s%n%n", report.datasetName));
        sb.append(String.format("Trạng thái         : %s%n", report.passed ? "HỢP LỆ (VALID) ✓" : "KHÔNG HỢP LỆ ✗"));
        sb.append(String.format("Giao dịch hợp lệ   : %,d / %,d%n", report.validTransactions, report.totalTransactions));
        sb.append(String.format("Số mục (Items)     : %,d%n", report.distinctItemsCount));
        sb.append(String.format("Số mục trong Đầu tư: %,d%n", report.investmentItemsCount));
        sb.append(String.format("Tổng utility mẫu   : %,.2f%n", report.totalDatasetUtility));

        if (!report.errors.isEmpty()) {
            sb.append("\nLỖI PHÁT HIỆN:\n");
            for (String err : report.errors) {
                sb.append(" - ").append(err).append("\n");
            }
        }

        JOptionPane.showMessageDialog(this, sb.toString(),
                "Báo cáo kiểm định: " + selected,
                report.passed ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    private void onRunSimulation() {
        String selected = (String) cbDatasets.getSelectedItem();
        if (selected == null) return;

        double minutil;
        int interval;
        int windowSize;
        double alpha;
        int maxPattern;
        int maxTxLimit;

        try {
            minutil = Double.parseDouble(txtMinUtil.getText().trim());
            interval = Integer.parseInt(txtInterval.getText().trim());
            windowSize = Integer.parseInt(txtWindowSize.getText().trim());
            alpha = Double.parseDouble(txtAlpha.getText().trim());
            maxPattern = Integer.parseInt(txtMaxPattern.getText().trim());
            maxTxLimit = Integer.parseInt(txtMaxTransactions.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng kiểm tra lại các tham số số học: " + e.getMessage(),
                    "Lỗi tham số", JOptionPane.ERROR_MESSAGE);
            return;
        }

        onReset();
        btnRun.setEnabled(false);
        btnPause.setEnabled(true);
        btnPause.setText("⏸ Tạm dừng");
        btnPause.setBackground(new Color(243, 156, 18));
        btnStop.setEnabled(true);
        cbDatasets.setEnabled(false);
        btnBrowseCustom.setEnabled(false);
        chkTraceEachTransaction.setEnabled(false);

        currentDelayMs = sliderDelay.getValue();
        String manualInput = selected.contains("Running Example")
                ? manualInputArea.getText()
                : null;
        currentProcessLogFile = new File("logs", "HUDD_TDS_Process_"
            + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date()) + ".log");
        currentWorker = new SimulationWorker(selected, minutil, interval, windowSize, alpha,
            maxPattern, maxTxLimit, chkTraceEachTransaction.isSelected(),
            manualInput, currentProcessLogFile);
        currentWorker.execute();
    }

    private void onTogglePause() {
        if (currentWorker != null && !currentWorker.isDone()) {
            if (!currentWorker.isPaused()) {
                currentWorker.setPaused(true);
                btnPause.setText("▶ Tiếp tục");
                btnPause.setBackground(new Color(40, 167, 69));
                progressBar.setString("Đang tạm dừng luồng... (Nhấn Tiếp tục để chạy tiếp)");
            } else {
                currentWorker.setPaused(false);
                btnPause.setText("⏸ Tạm dừng");
                btnPause.setBackground(new Color(243, 156, 18));
                progressBar.setString("Đang tiếp tục phát luồng...");
            }
        }
    }

    private void onStopSimulation() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
            btnStop.setEnabled(false);
            btnPause.setEnabled(false);
            btnRun.setEnabled(true);
            cbDatasets.setEnabled(true);
            btnBrowseCustom.setEnabled(true);
            progressBar.setString("Đã dừng luồng bởi người dùng.");
        }
    }

    private void onReset() {
        huiTableModel.setRowCount(0);
        driftTableModel.setRowCount(0);
        chartPanel.clear();
        chartPanel.repaint();

        totalCheckpointsCount = 0;
        totalGlobalDriftsCount = 0;
        totalLocalDriftsCount = 0;

        lblCurrentTID.setText("T_0");
        lblHuiCount.setText("0");
        lblGlobalDistance.setText("0.0000");
        lblStatus.setText("ỔN ĐỊNH");
        lblStatus.setForeground(Color.BLACK);
        lblLocalDrift.setText("ỔN ĐỊNH");
        lblLocalDrift.setForeground(Color.BLACK);
        lblSpeed.setText("0");

        progressBar.setValue(0);
        progressBar.setString("Sẵn sàng phát luồng");
        calculationDetails.setText("Hệ thống đã đặt lại trạng thái ban đầu.");
        summaryStatsArea.setText("Chưa có thống kê thực nghiệm.");
        processLogArea.setText("");
        currentProcessLogFile = null;
    }

    private void onShowExportMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem itemExportHui = new JMenuItem("Xuất Nhật ký HUI (CSV)");
        itemExportHui.addActionListener(e -> exportHuiToCsv());
        menu.add(itemExportHui);

        JMenuItem itemExportDrift = new JMenuItem("Xuất Nhật ký Drift (CSV)");
        itemExportDrift.addActionListener(e -> exportDriftToCsv());
        menu.add(itemExportDrift);

        JMenuItem itemExportSummary = new JMenuItem("Xuất Báo cáo Thống kê Tổng thể (TXT)");
        itemExportSummary.addActionListener(e -> exportSummaryReport());
        menu.add(itemExportSummary);

        JMenuItem itemExportProcessLog = new JMenuItem("Xuất Nhật ký tiến trình (TXT)");
        itemExportProcessLog.addActionListener(e -> exportProcessLog());
        menu.add(itemExportProcessLog);

        menu.show(btnExport, 0, btnExport.getHeight());
    }

    private void exportHuiToCsv() {
        if (huiTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Chưa có dữ liệu HUI để xuất!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("HUDD_TDS_HUI_Report_" + System.currentTimeMillis() + ".csv"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(new FileWriter(saveFile))) {
                writer.println("Checkpoint TID,Itemset,TU,Faded Utility,Vector,Distance Dmo");
                for (int i = 0; i < huiTableModel.getRowCount(); i++) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            huiTableModel.getValueAt(i, 0),
                            huiTableModel.getValueAt(i, 1),
                            huiTableModel.getValueAt(i, 2),
                            huiTableModel.getValueAt(i, 3),
                            huiTableModel.getValueAt(i, 4),
                            huiTableModel.getValueAt(i, 5));
                }
                JOptionPane.showMessageDialog(this, "Xuất báo cáo HUI thành công:\n" + saveFile.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi lưu tệp: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportDriftToCsv() {
        if (driftTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Chưa ghi nhận sự kiện Drift nào để xuất!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("HUDD_TDS_Drift_Report_" + System.currentTimeMillis() + ".csv"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(new FileWriter(saveFile))) {
                writer.println("Checkpoint TID,Drift Type,Statistic Value,Epsilon Threshold,Details");
                for (int i = 0; i < driftTableModel.getRowCount(); i++) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            driftTableModel.getValueAt(i, 0),
                            driftTableModel.getValueAt(i, 1),
                            driftTableModel.getValueAt(i, 2),
                            driftTableModel.getValueAt(i, 3),
                            driftTableModel.getValueAt(i, 4));
                }
                JOptionPane.showMessageDialog(this, "Xuất báo cáo Drift thành công:\n" + saveFile.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi lưu tệp: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportSummaryReport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("HUDD_TDS_Summary_" + System.currentTimeMillis() + ".txt"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(new FileWriter(saveFile))) {
                writer.println(summaryStatsArea.getText());
                JOptionPane.showMessageDialog(this, "Xuất báo cáo tổng quan thành công:\n" + saveFile.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi lưu tệp: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportProcessLog() {
        if (currentProcessLogFile == null || !currentProcessLogFile.isFile()) {
            JOptionPane.showMessageDialog(this, "Chưa có nhật ký tiến trình để xuất.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(currentProcessLogFile.getName()));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            try {
                Files.copy(currentProcessLogFile.toPath(), saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(this, "Xuất nhật ký tiến trình thành công:\n" + saveFile.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi lưu tệp: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void appendProcessLog(String message) {
        processLogArea.append(message);
        processLogArea.append(System.lineSeparator());
        if (processLogArea.getDocument().getLength() > 500_000) {
            try {
                int trimLength = processLogArea.getDocument().getLength() - 400_000;
                String prefix = processLogArea.getDocument().getText(0, trimLength);
                int endOfLine = prefix.lastIndexOf('\n');
                if (endOfLine >= 0) {
                    processLogArea.getDocument().remove(0, endOfLine + 1);
                }
            } catch (BadLocationException ignored) {
            }
        }
        processLogArea.setCaretPosition(processLogArea.getDocument().getLength());
    }

    /**
     * Lớp SwingWorker xử lý luồng ngầm với hỗ trợ Pause/Resume và Stream Speed Control.
     */
    private class SimulationWorker extends SwingWorker<Void, SimulationUpdate> {
        private final String datasetName;
        private final double minutil;
        private final int interval;
        private final int windowSize;
        private final double alpha;
        private final int maxPattern;
        private final int maxTxLimit;
        private final boolean traceEachTransaction;
        private final String manualInput;
        private final File logFile;

        private volatile boolean paused = false;
        private final Object pauseLock = new Object();
        private BufferedWriter traceWriter;
        private long traceMessageCount;
        private SimulationService simulationService;

        private long startTimeMs = 0;
        private int totalProcessedTransactions = 0;

        public SimulationWorker(String datasetName, double minutil, int interval, int windowSize, double alpha,
                                int maxPattern, int maxTxLimit, boolean traceEachTransaction,
                                String manualInput, File logFile) {
            this.datasetName = datasetName;
            this.minutil = minutil;
            this.interval = interval;
            this.windowSize = windowSize;
            this.alpha = alpha;
            this.maxPattern = maxPattern;
            this.maxTxLimit = maxTxLimit;
            this.traceEachTransaction = traceEachTransaction;
            this.manualInput = manualInput;
            this.logFile = logFile;
        }

        private void trace(String message) {
            String entry = String.format("[%1$tF %1$tT.%1$tL] %2$s", new Date(), message);
            if (traceWriter != null) {
                try {
                    traceWriter.write(entry);
                    traceWriter.newLine();
                    if (++traceMessageCount % 50 == 0) {
                        traceWriter.flush();
                    }
                } catch (IOException ex) {
                    publish(SimulationUpdate.trace("[LOG ERROR] Không thể ghi tiếp tệp log: " + ex.getMessage()));
                    traceWriter = null;
                }
            }
            publish(SimulationUpdate.trace(entry));
        }

        public void setPaused(boolean paused) {
            this.paused = paused;
            if (!paused) {
                synchronized (pauseLock) {
                    pauseLock.notifyAll();
                }
            }
        }

        public boolean isPaused() {
            return paused;
        }

        @Override
        protected Void doInBackground() throws Exception {
            this.startTimeMs = System.currentTimeMillis();
            File logDirectory = logFile.getParentFile();
            if (logDirectory != null && !logDirectory.exists() && !logDirectory.mkdirs()) {
                throw new IOException("Không thể tạo thư mục log: " + logDirectory.getAbsolutePath());
            }
            traceWriter = Files.newBufferedWriter(logFile.toPath(), StandardCharsets.UTF_8);
            trace("[RUN] Bắt đầu phiên; dataset=" + datasetName
                    + ", minutil=" + minutil + ", interval=" + interval
                    + ", window=" + windowSize + ", alpha=" + alpha
                    + ", maxPattern=" + maxPattern + ", maxTx=" + maxTxLimit
                    + ", traceEachTransaction=" + traceEachTransaction);
            try {
            boolean runningExample = datasetName.contains("Running Example");
            Map<String, Double> externalUtilities = datasetService.loadInvestmentTable(
                    datasetName, customInvestmentFile, runningExample);
            trace("[DATA] Đã nạp bảng utility; số item=" + externalUtilities.size());

            simulationService = new SimulationService(
                    externalUtilities, minutil, interval, windowSize, alpha, maxPattern);
            simulationService.addListener(event -> {
                publish(SimulationUpdate.event(event));
                if (event.getType() == EventType.CHECKPOINT_CREATED) {
                    trace("[DRIFT] Hoàn tất kiểm định TID=" + event.getTid()
                            + "; global=" + (event.getGlobalDriftMessage() == null
                            ? "ổn định" : event.getGlobalDriftMessage())
                            + "; local=" + (event.getLocalDriftMessage() == null
                            ? "ổn định" : event.getLocalDriftMessage()));
                }
            });
            simulationService.setTraceListener(this::trace, traceEachTransaction);
            trace("[ENGINE] Đã khởi tạo SimulationService và gắn trace listener.");

            // 2. Mở luồng giao dịch
            BufferedReader reader;
            int totalLinesEstimate;
            if (datasetName.contains("Running Example")) {
                reader = new BufferedReader(new StringReader(manualInput));
            } else {
                reader = datasetService.openTransactionStream(datasetName, customTransactionFile);
            }
            totalLinesEstimate = datasetService.estimateTransactionCount(
                    datasetName, customTransactionFile);
            trace("[DATA] Đã mở transaction stream; ước tính số dòng=" + totalLinesEstimate);

            try (reader) {
                String line;
                int tid = 0;

                while ((line = reader.readLine()) != null) {
                    if (isCancelled()) {
                        break;
                    }

                    // Tạm dừng nếu được yêu cầu
                    if (paused) {
                        synchronized (pauseLock) {
                            while (paused && !isCancelled()) {
                                pauseLock.wait(100);
                            }
                        }
                    }
                    if (isCancelled()) {
                        break;
                    }

                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    // Giới hạn giao dịch nếu được cấu hình
                    if (maxTxLimit > 0 && tid >= maxTxLimit) {
                        break;
                    }

                    tid++;
                    totalProcessedTransactions++;
                    Transaction tx = simulationService.processLine(line, tid);
                    if (traceEachTransaction || tid == 1 || tid % interval == 0) {
                        trace(String.format(Locale.US, "[PARSE] TID=%d hợp lệ; items=%d, TU=%.4f",
                                tid, tx.getElements().size(), tx.getTransactionUtility()));
                    }

                    // Tính tốc độ xử lý
                    long elapsed = Math.max(1, System.currentTimeMillis() - startTimeMs);
                    int currentTxPerSec = (int) ((totalProcessedTransactions * 1000.0) / elapsed);

                    if (tid % interval == 0 || tid % 100 == 0) {
                        simulationService.publishProgress(tid,
                                maxTxLimit > 0 ? maxTxLimit : totalLinesEstimate, currentTxPerSec);
                    }

                    // Điều chỉnh độ trễ phát luồng
                    int delay = currentDelayMs;
                    if (delay > 0) {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    }
                }
            }

            if (!isCancelled()) {
                simulationService.finish(totalProcessedTransactions);
            }
            trace(isCancelled() ? "[RUN] Đã dừng theo yêu cầu." : "[RUN] Hoàn tất thành công; transaction stream đã xử lý xong.");
            return null;
            } catch (IOException | InterruptedException | RuntimeException ex) {
                if (simulationService != null && !isCancelled()) {
                    simulationService.reportError(ex, totalProcessedTransactions);
                }
                trace(isCancelled() ? "[RUN] Đã hủy trong khi xử lý."
                        : "[ERROR] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                throw ex;
            } finally {
                if (traceWriter != null) {
                    try {
                        traceWriter.flush();
                        traceWriter.close();
                    } finally {
                        traceWriter = null;
                    }
                }
            }
        }

        @Override
        protected void process(List<SimulationUpdate> updates) {
            for (SimulationUpdate upd : updates) {
                if (upd.traceMessage != null) {
                    appendProcessLog(upd.traceMessage);
                    continue;
                }
                SimulationEvent event = upd.event;
                if (event == null) {
                    continue;
                }

                if (event.getType() == EventType.SIMULATION_ERROR) {
                    appendProcessLog("[ERROR] " + event.getMessage());
                    continue;
                }
                if (event.getType() == EventType.TRANSACTION_PROCESSED) {
                    lblCurrentTID.setText("T_" + event.getTid());
                    int progress = (int) Math.min(100,
                            (event.getTid() * 100.0) / Math.max(1, event.getTotalEstimate()));
                    progressBar.setValue(progress);
                    progressBar.setString(String.format("TID %d / %d (%d%%)",
                            event.getTid(), event.getTotalEstimate(), progress));
                    lblSpeed.setText(String.valueOf(event.getSpeedTxPerSec()));
                    continue;
                }
                if (event.getType() == EventType.CHECKPOINT_CREATED) {
                    Checkpoint cp = event.getCheckpoint();
                    DriftResult globalResult = event.getGlobalDrift();
                    DriftResult localResult = event.getLocalDrift();
                    String globalDrift = globalResult.isDetected()
                            ? event.getGlobalDriftMessage() : null;
                    String localDrift = localResult.isDetected()
                            ? event.getLocalDriftMessage() : null;
                    totalCheckpointsCount++;
                    lblHuiCount.setText(String.valueOf(cp.getHuis().size()));
                    lblGlobalDistance.setText(String.format(Locale.US, "%.4f", cp.getGlobalDistance()));

                    boolean hasGlobalDrift = globalResult.isDetected();
                    boolean hasLocalDrift = localResult.isDetected();

                    // Cập nhật trạng thái Global Drift
                    if (hasGlobalDrift) {
                        totalGlobalDriftsCount++;
                        lblStatus.setText(globalDrift);
                        lblStatus.setForeground(new Color(220, 38, 38));
                        driftTableModel.addRow(new Object[]{
                                "TID " + cp.getTid(), "GLOBAL DRIFT",
                                String.format(Locale.US, "%.4f", cp.getGlobalDistance()),
                                "-", globalDrift
                        });
                    } else {
                        lblStatus.setText("ỔN ĐỊNH");
                        lblStatus.setForeground(new Color(0, 140, 0));
                    }

                    // Cập nhật trạng thái Local Drift
                    if (hasLocalDrift) {
                        totalLocalDriftsCount++;
                        lblLocalDrift.setText(localDrift);
                        lblLocalDrift.setForeground(new Color(217, 119, 6));
                        driftTableModel.addRow(new Object[]{
                                "TID " + cp.getTid(), "LOCAL DRIFT",
                                "-", "-", localDrift
                        });
                    } else {
                        lblLocalDrift.setText("ỔN ĐỊNH");
                        lblLocalDrift.setForeground(new Color(0, 140, 0));
                    }

                    // Thêm vào bảng HUI
                    for (int i = 0; i < cp.getHuis().size(); i++) {
                        HighUtilityItemset hui = cp.getHuis().get(i);
                        huiTableModel.addRow(new Object[]{
                                "TID " + cp.getTid(),
                                hui.getItems().toString(),
                                String.format(Locale.US, "%.2f",
                                        event.getTransaction().getTransactionUtility()),
                                String.format(Locale.US, "%.2f", hui.getTotalUtility()),
                                event.getItemsetVectors().get(i),
                                String.format(Locale.US, "%.4f", hui.getDistanceToRoot())
                        });
                    }

                    // Tự động cuộn bảng HUI nếu được bật
                    if (chkAutoScrollHui.isSelected() && huiTable.getRowCount() > 0) {
                        huiTable.scrollRectToVisible(huiTable.getCellRect(huiTable.getRowCount() - 1, 0, true));
                    }

                    // Cập nhật ChartPanel với đánh dấu điểm Drift
                    chartPanel.addCheckpoint(cp, hasGlobalDrift);

                    // Cập nhật chi tiết tính toán
                    calculationDetails.setText(String.format(Locale.US,
                            "Checkpoint TID %d: HUI=%d, Global Distance (DIS_HS)=%.4f | Global Drift: %s | Local Drift: %s",
                            cp.getTid(), cp.getHuis().size(), cp.getGlobalDistance(),
                            (hasGlobalDrift ? globalDrift : "Không (Ổn định)"),
                            (hasLocalDrift ? localDrift : "Không (Ổn định)")
                    ));

                    // Cập nhật bảng tổng quan thống kê
                    long elapsed = Math.max(1, System.currentTimeMillis() - startTimeMs);
                    summaryStatsArea.setText(String.format(Locale.US,
                            "=== BÁO CÁO TỔNG HỢP TIẾN TRÌNH HUDD-TDS ===%n"
                                    + "Dataset: %s%n"
                                    + "MinUtil: %.2f | Interval: %d | Window: %d | Alpha: %.2f | Max Len: %d%n"
                                    + "--------------------------------------------------------%n"
                                    + "Tổng số Giao dịch đã xử lý: %,d%n"
                                    + "Tổng số Checkpoint: %,d%n"
                                    + "Số lần phát hiện Global Drift: %,d%n"
                                    + "Số lần phát hiện Local Drift: %,d%n"
                                    + "Thời gian thực thi: %.2f giây%n"
                                    + "Tốc độ trung bình: %,d tx/giây%n"
                                    + "Trạng thái hiện tại: %s%n",
                            datasetName, minutil, interval, windowSize, alpha, maxPattern,
                            totalProcessedTransactions, totalCheckpointsCount,
                            totalGlobalDriftsCount, totalLocalDriftsCount,
                            elapsed / 1000.0,
                            (int) ((totalProcessedTransactions * 1000.0) / elapsed),
                            (paused ? "ĐANG TẠM DỪNG" : "ĐANG CHẠY")
                    ));
                }
            }
        }

        @Override
        protected void done() {
            btnRun.setEnabled(true);
            btnPause.setEnabled(false);
            btnStop.setEnabled(false);
            cbDatasets.setEnabled(true);
            btnBrowseCustom.setEnabled(true);
            chkTraceEachTransaction.setEnabled(true);

            boolean completed = false;
            try {
                get();
                completed = true;
                progressBar.setValue(100);
                progressBar.setString("Hoàn tất luồng xử lý! Tổng số: " + totalProcessedTransactions + " giao dịch.");
                appendProcessLog("[RUN] Hoàn tất thành công. Tệp log: " + logFile.getAbsolutePath());
            } catch (CancellationException ex) {
                progressBar.setString("Đã dừng luồng. Tổng số: " + totalProcessedTransactions + " giao dịch.");
                appendProcessLog("[RUN] Đã dừng. Tệp log: " + logFile.getAbsolutePath());
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                progressBar.setString("Luồng kết thúc do lỗi.");
                appendProcessLog("[ERROR] " + (cause == null ? ex.getMessage() : cause.toString()));
                JOptionPane.showMessageDialog(HUDD_TDS_GUI.this,
                        "Lỗi khi xử lý luồng: " + (cause == null ? ex.getMessage() : cause.getMessage()),
                        "Lỗi xử lý", JOptionPane.ERROR_MESSAGE);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                progressBar.setString("Không thể xác nhận trạng thái luồng.");
                appendProcessLog("[ERROR] Bị gián đoạn khi chờ kết quả worker.");
            }

            long elapsed = Math.max(1, System.currentTimeMillis() - startTimeMs);
            summaryStatsArea.setText(String.format(Locale.US,
                    "=== BÁO CÁO KẾT THÚC THỰC NGHIỆM HUDD-TDS ===%n"
                            + "Dataset: %s%n"
                            + "MinUtil: %.2f | Interval: %d | Window: %d | Alpha: %.2f | Max Len: %d%n"
                            + "--------------------------------------------------------%n"
                            + "Tổng số Giao dịch: %,d%n"
                            + "Tổng số Checkpoint: %,d%n"
                            + "Số lần phát hiện Global Drift: %,d%n"
                            + "Số lần phát hiện Local Drift: %,d%n"
                            + "Tổng thời gian: %.2f giây%n"
                            + "Tốc độ thông lượng: %,d tx/giây%n"
                                + "Trạng thái: %s%n",
                    datasetName, minutil, interval, windowSize, alpha, maxPattern,
                    totalProcessedTransactions, totalCheckpointsCount,
                    totalGlobalDriftsCount, totalLocalDriftsCount,
                    elapsed / 1000.0,
                            (int) ((totalProcessedTransactions * 1000.0) / elapsed),
                            completed ? "HOÀN TẤT THÀNH CÔNG ✓" : (isCancelled() ? "ĐÃ DỪNG" : "KẾT THÚC DO LỖI")
            ));
        }
    }

    private static class SimulationUpdate {
        final SimulationEvent event;
        final String traceMessage;

        private SimulationUpdate(SimulationEvent event) {
            this.event = event;
            this.traceMessage = null;
        }

        private SimulationUpdate(String traceMessage) {
            this.event = null;
            this.traceMessage = traceMessage;
        }

        static SimulationUpdate event(SimulationEvent event) {
            return new SimulationUpdate(event);
        }

        static SimulationUpdate trace(String message) {
            return new SimulationUpdate(message);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                     | UnsupportedLookAndFeelException ignored) {
            }
            new HUDD_TDS_GUI().setVisible(true);
        });
    }
}
