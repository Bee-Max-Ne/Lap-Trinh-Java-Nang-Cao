package huddtds.demo;

import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;
import javax.swing.JPanel;

/**
 * Panel đồ thị tương tác hiển thị biến thiên của luồng dữ liệu theo Checkpoint:
 * - Hỗ trợ 2 chế độ hiển thị: Khoảng cách toàn cục (DISHS) hoặc Số lượng HUI.
 * - Đánh dấu trực quan các điểm phát hiện Trôi dạt (Drift) bằng màu Đỏ nổi bật.
 * - Tương tác di chuột (Hover Tooltip) xem thông tin chi tiết từng Checkpoint.
 * - Khử răng cưa (Antialiasing) và đồ họa gradient chuyên nghiệp.
 */
public class ChartPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public static final int MAX_RETAINED_CHECKPOINTS = 1000;

    public enum MetricMode {
        GLOBAL_DISTANCE("Khoảng cách toàn cục (DISHS)"),
        HUI_COUNT("Số lượng HUI (Đường)"),
        BAR_HUI_COUNT("Số lượng HUI (Cột phân màu Drift)"),
        RAM_USAGE("Tiêu thụ RAM (MB)"),
        THROUGHPUT("Thông lượng (tx/giây)");

        private final String title;
        MetricMode(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    private final List<Checkpoint> checkpoints;
    private final Set<Integer> driftTids;
    private final Map<Integer, DriftResult> driftResults;
    private MetricMode metricMode = MetricMode.GLOBAL_DISTANCE;

    // Hover state
    private Point mousePoint = null;
    private int hoveredIndex = -1;

    public ChartPanel() {
        this.checkpoints = new ArrayList<>();
        this.driftTids = new HashSet<>();
        this.driftResults = new HashMap<>();
        setPreferredSize(new Dimension(440, 260));
        setBackground(Color.WHITE);

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePoint = e.getPoint();
                updateHoveredIndex();
                repaint();
            }
        });
    }

    public synchronized void setMetricMode(MetricMode mode) {
        if (mode != null && this.metricMode != mode) {
            this.metricMode = mode;
            repaint();
        }
    }

    public MetricMode getMetricMode() {
        return metricMode;
    }

    public synchronized void updateChart(List<Checkpoint> data) {
        checkpoints.clear();
        driftTids.clear();
        driftResults.clear();
        if (data != null) {
            checkpoints.addAll(sortCheckpointsByTid(data));
            trimOldCheckpoints();
        }
        hoveredIndex = -1;
        repaint();
    }

    public synchronized void addCheckpoint(Checkpoint cp) {
        addCheckpoint(cp, false);
    }

    public synchronized void addCheckpoint(Checkpoint cp, boolean isGlobalDrift) {
        if (cp != null) {
            insertCheckpointInTidOrder(cp);
            if (isGlobalDrift) {
                driftTids.add(cp.getTid());
            }
            trimOldCheckpoints();
            repaint();
        }
    }

    public synchronized void addCheckpoint(Checkpoint cp, DriftResult globalDrift) {
        if (cp == null) {
            return;
        }
        insertCheckpointInTidOrder(cp);
        if (isGlobalDrift(globalDrift)) {
            driftTids.add(cp.getTid());
            driftResults.put(cp.getTid(), globalDrift);
        }
        trimOldCheckpoints();
        repaint();
    }

    public synchronized void markDrift(int tid) {
        driftTids.add(tid);
        repaint();
    }

    public synchronized void clear() {
        checkpoints.clear();
        driftTids.clear();
        driftResults.clear();
        hoveredIndex = -1;
        repaint();
    }

    private void insertCheckpointInTidOrder(Checkpoint checkpoint) {
        checkpoints.removeIf(existing -> existing.getTid() == checkpoint.getTid());
        driftTids.remove(checkpoint.getTid());
        driftResults.remove(checkpoint.getTid());
        int insertionIndex = Collections.binarySearch(
                checkpoints, checkpoint, Comparator.comparingInt(Checkpoint::getTid));
        checkpoints.add(insertionIndex < 0 ? -insertionIndex - 1 : insertionIndex, checkpoint);
    }

    private void trimOldCheckpoints() {
        while (checkpoints.size() > MAX_RETAINED_CHECKPOINTS) {
            int removedTid = checkpoints.remove(0).getTid();
            driftTids.remove(removedTid);
            driftResults.remove(removedTid);
        }
    }

    public int getRetainedCheckpointCount() {
        return checkpoints.size();
    }

    private synchronized double getMetricValue(Checkpoint cp) {
        if (metricMode == MetricMode.HUI_COUNT || metricMode == MetricMode.BAR_HUI_COUNT) {
            return cp.getHuis() != null ? cp.getHuis().size() : 0.0;
        } else if (metricMode == MetricMode.RAM_USAGE) {
            return cp.getRamUsageMb();
        } else if (metricMode == MetricMode.THROUGHPUT) {
            return cp.getThroughputTxPerSec();
        }
        return cp.getGlobalDistance();
    }

    private synchronized void updateHoveredIndex() {
        if (mousePoint == null || checkpoints.isEmpty()) {
            hoveredIndex = -1;
            return;
        }

        int paddingLeft = 60;
        int paddingRight = 25;
        int plotWidth = getWidth() - paddingLeft - paddingRight;
        if (plotWidth <= 0) {
            hoveredIndex = -1;
            return;
        }

        List<Checkpoint> sortedCheckpoints = sortCheckpointsByTid(checkpoints);
        int bestIdx = -1;
        double bestDist = 24.0; // bán kính bắt điểm
        for (int i = 0; i < sortedCheckpoints.size(); i++) {
            int x = xForTid(sortedCheckpoints.get(i).getTid(), sortedCheckpoints,
                    paddingLeft, plotWidth);
            double dist = Math.abs(x - mousePoint.x);
            if (dist < bestDist) {
                bestDist = dist;
                bestIdx = i;
            }
        }
        hoveredIndex = bestIdx;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        int paddingLeft = 65;
        int paddingTop = 35;
        int paddingRight = 25;
        int paddingBottom = 48;

        int plotWidth = width - paddingLeft - paddingRight;
        int plotHeight = height - paddingTop - paddingBottom;

        // Nền đồ thị
        g2.setColor(new Color(248, 250, 253));
        g2.fillRect(0, 0, width, height);

        // Tiêu đề đồ thị
        g2.setColor(new Color(30, 41, 59));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.drawString(metricMode == MetricMode.GLOBAL_DISTANCE ? metricMode.getTitle() + " (log scale)" : metricMode.getTitle(), paddingLeft, 22);

        // Chú giải (Legend) ở góc trên phải
        drawLegend(g2, width - paddingRight, 12);

        List<Checkpoint> snapshot;
        Set<Integer> driftSnapshot;
        Map<Integer, DriftResult> driftResultSnapshot;
        synchronized (this) {
                snapshot = sortCheckpointsByTid(checkpoints);
            driftSnapshot = new HashSet<>(driftTids);
            driftResultSnapshot = new HashMap<>(driftResults);
        }

        if (snapshot.isEmpty() || plotWidth <= 0 || plotHeight <= 0) {
            g2.setColor(new Color(148, 163, 184));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString("Chưa có dữ liệu checkpoint để vẽ đồ thị.", paddingLeft + 15, paddingTop + 40);
            g2.dispose();
            return;
        }

        // Dùng log1p để giữ được chi tiết ở vùng thấp mà vẫn biểu diễn được các đỉnh lớn.
        double minTransformed = Double.POSITIVE_INFINITY;
        double maxTransformed = Double.NEGATIVE_INFINITY;
        for (Checkpoint cp : snapshot) {
            double transformed = applyMetricTransform(getMetricValue(cp));
            minTransformed = Math.min(minTransformed, transformed);
            maxTransformed = Math.max(maxTransformed, transformed);
        }

        if (!Double.isFinite(minTransformed) || !Double.isFinite(maxTransformed)) {
            g2.setColor(new Color(148, 163, 184));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString("Dữ liệu đồ thị chứa giá trị không hữu hạn.", paddingLeft + 15, paddingTop + 40);
            g2.dispose();
            return;
        }
        double transformedRange = maxTransformed - minTransformed;
        if (transformedRange == 0.0) {
            transformedRange = Math.max(1.0, Math.abs(maxTransformed) * 0.2);
        }
        double chartMin = minTransformed - transformedRange * 0.10;
        double chartMax = maxTransformed + transformedRange * 0.15;
        if (minTransformed >= 0.0) {
            chartMin = 0.0;
        }
        if (chartMin == chartMax) {
            chartMax = chartMin + 1.0;
        }

        // Vùng vẽ trắng
        g2.setColor(Color.WHITE);
        g2.fillRect(paddingLeft, paddingTop, plotWidth, plotHeight);
        g2.setColor(new Color(226, 232, 240));
        g2.drawRect(paddingLeft, paddingTop, plotWidth, plotHeight);

        // Lưới ngang và trục Y
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        int ticks = 4;
        for (int t = 0; t <= ticks; t++) {
            double ratio = (double) t / ticks;
            int y = paddingTop + (int) (ratio * plotHeight);
            double val = applyInverseMetricTransform(chartMax - ratio * (chartMax - chartMin));

            g2.setColor(new Color(241, 245, 249));
            g2.drawLine(paddingLeft, y, paddingLeft + plotWidth, y);

            g2.setColor(new Color(100, 116, 139));
            String label;
            if (metricMode == MetricMode.HUI_COUNT || metricMode == MetricMode.BAR_HUI_COUNT) {
                label = String.format(Locale.US, "%.0f", val);
            } else if (metricMode == MetricMode.RAM_USAGE) {
                label = String.format(Locale.US, "%.1f MB", val);
            } else if (metricMode == MetricMode.THROUGHPUT) {
                label = String.format(Locale.US, "%,.0f", val);
            } else {
                label = formatYAxisValue(val);
            }
            g2.drawString(label, 6, y + 4);
        }

        // Tính tọa độ các điểm
        int n = snapshot.size();
        int[] xCoords = new int[n];
        int[] yCoords = new int[n];

        for (int i = 0; i < n; i++) {
            xCoords[i] = xForTid(snapshot.get(i).getTid(), snapshot, paddingLeft, plotWidth);
            double transformed = applyMetricTransform(getMetricValue(snapshot.get(i)));
            double normalized = (transformed - chartMin) / (chartMax - chartMin);
            yCoords[i] = paddingTop + plotHeight - (int) (normalized * plotHeight);
        }

        if (metricMode == MetricMode.BAR_HUI_COUNT) {
            // Vẽ biểu đồ cột
            int barWidth = Math.max(3, (plotWidth / Math.max(1, n)) - 2);
            for (int i = 0; i < n; i++) {
                int x = xCoords[i] - barWidth / 2;
                int y = yCoords[i];
                int h = Math.max(2, paddingTop + plotHeight - y);

                boolean isDrift = driftSnapshot.contains(snapshot.get(i).getTid());

                if (isDrift) {
                    g2.setColor(new Color(239, 68, 68, 190));
                } else {
                    g2.setColor(new Color(59, 130, 246, 190));
                }
                g2.fillRect(x, y, barWidth, h);

                if (isDrift) {
                    g2.setColor(new Color(220, 38, 38));
                } else {
                    g2.setColor(new Color(37, 99, 235));
                }
                g2.drawRect(x, y, barWidth, h);
            }
        } else {
            // Vẽ gradient diện tích dưới đường line
            if (n > 1) {
                Path2D area = new Path2D.Double();
                area.moveTo(xCoords[0], paddingTop + plotHeight);
                for (int i = 0; i < n; i++) {
                    area.lineTo(xCoords[i], yCoords[i]);
                }
                area.lineTo(xCoords[n - 1], paddingTop + plotHeight);
                area.closePath();

                Color startCol, endCol;
                if (metricMode == MetricMode.RAM_USAGE) {
                    startCol = new Color(16, 185, 129, 85); // Xanh ngọc
                    endCol = new Color(16, 185, 129, 5);
                } else if (metricMode == MetricMode.THROUGHPUT) {
                    startCol = new Color(147, 51, 234, 85); // Tím
                    endCol = new Color(147, 51, 234, 5);
                } else {
                    startCol = new Color(59, 130, 246, 75); // Xanh dương
                    endCol = new Color(59, 130, 246, 5);
                }
                g2.setPaint(new GradientPaint(0, paddingTop, startCol, 0, paddingTop + plotHeight, endCol));
                g2.fill(area);
            }

            // Vẽ đường nối
            if (metricMode == MetricMode.RAM_USAGE) {
                g2.setColor(new Color(5, 150, 105)); // Xanh ngọc đậm
            } else if (metricMode == MetricMode.THROUGHPUT) {
                g2.setColor(new Color(126, 34, 206)); // Tím đậm
            } else {
                g2.setColor(new Color(37, 99, 235));
            }
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < n - 1; i++) {
                g2.drawLine(xCoords[i], yCoords[i], xCoords[i + 1], yCoords[i + 1]);
            }

            // Vạch đứt đánh dấu Đỉnh (Peak) cho RAM và Thông lượng
            if (metricMode == MetricMode.RAM_USAGE || metricMode == MetricMode.THROUGHPUT) {
                int peakIdx = -1;
                double peakVal = -1.0;
                for (int i = 0; i < n; i++) {
                    double v = getMetricValue(snapshot.get(i));
                    if (v > peakVal) {
                        peakVal = v;
                        peakIdx = i;
                    }
                }
                if (peakIdx >= 0 && peakVal > 0) {
                    int peakY = yCoords[peakIdx];
                    g2.setColor(new Color(239, 68, 68, 140));
                    g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0));
                    g2.drawLine(paddingLeft, peakY, paddingLeft + plotWidth, peakY);

                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    g2.setColor(new Color(220, 38, 38));
                    String peakStr = (metricMode == MetricMode.RAM_USAGE)
                            ? String.format(Locale.US, "Đỉnh: %.1f MB", peakVal)
                            : String.format(Locale.US, "Đỉnh: %,d tx/s", (int) peakVal);
                    g2.drawString(peakStr, paddingLeft + plotWidth - g2.getFontMetrics().stringWidth(peakStr) - 4, peakY - 3);
                    g2.setStroke(new BasicStroke(1.0f));
                }
            }
        }

        // Vẽ các điểm checkpoint và nhãn trục X
        int labelStep = Math.max(1, n / 6);
        for (int i = 0; i < n; i++) {
            Checkpoint cp = snapshot.get(i);
            int x = xCoords[i];
            int y = yCoords[i];
            boolean isDrift = driftSnapshot.contains(cp.getTid());

            // Nhãn trục X
            if (i % labelStep == 0 || i == n - 1) {
                g2.setColor(new Color(100, 116, 139));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                String tidLabel = "T" + cp.getTid();
                g2.drawString(tidLabel, x - g2.getFontMetrics().stringWidth(tidLabel) / 2,
                        paddingTop + plotHeight + 18);
            }

            // Vẽ điểm mốc trực quan (Tự động thích ứng theo mật độ điểm để chống rối mắt / đè lấn)
            if (metricMode != MetricMode.BAR_HUI_COUNT) {
                boolean isDense = (n > 35);
                boolean isUltraDense = (n > 85);

                if (isDrift) {
                    if (isUltraDense) {
                        // Mật độ rất dày (> 85 điểm): Chấm đỏ gọn 4px, không vẽ quầng halo lớn
                        g2.setColor(new Color(220, 38, 38));
                        g2.fillOval(x - 2, y - 2, 4, 4);
                    } else if (isDense) {
                        // Mật độ dày vừa (35 - 85 điểm): Chấm đỏ 6px có tâm trắng
                        g2.setColor(new Color(220, 38, 38));
                        g2.fillOval(x - 3, y - 3, 6, 6);
                        g2.setColor(Color.WHITE);
                        g2.fillOval(x - 1, y - 1, 2, 2);
                    } else {
                        // Mật độ thưa (<= 35 điểm): Giữ quầng phát sáng nổi bật
                        g2.setColor(new Color(239, 68, 68, 90));
                        g2.fillOval(x - 7, y - 7, 14, 14);
                        g2.setColor(new Color(220, 38, 38));
                        g2.fillOval(x - 4, y - 4, 8, 8);
                        g2.setColor(Color.WHITE);
                        g2.fillOval(x - 2, y - 2, 4, 4);
                    }
                } else {
                    // Điểm ổn định: Khi dữ liệu quá dày (> 85 điểm), ẩn bớt để đường xu hướng (line) mượt và rõ nét
                    if (!isUltraDense) {
                        int r = isDense ? 2 : 3;
                        g2.setColor(Color.WHITE);
                        g2.fillOval(x - r - 1, y - r - 1, (r + 1) * 2, (r + 1) * 2);
                        if (metricMode == MetricMode.RAM_USAGE) {
                            g2.setColor(new Color(5, 150, 105));
                        } else if (metricMode == MetricMode.THROUGHPUT) {
                            g2.setColor(new Color(126, 34, 206));
                        } else {
                            g2.setColor(new Color(37, 99, 235));
                        }
                        g2.fillOval(x - r, y - r, r * 2, r * 2);
                    }
                }
            }
        }

        // Vẽ Tooltip nếu người dùng đang hover một điểm
        if (hoveredIndex >= 0 && hoveredIndex < n) {
            Checkpoint hCp = snapshot.get(hoveredIndex);
            int hX = xCoords[hoveredIndex];
            int hY = yCoords[hoveredIndex];
            boolean isDrift = driftSnapshot.contains(hCp.getTid());

            // Vạch dọc chỉ báo
            g2.setColor(new Color(148, 163, 184, 180));
            g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0));
            g2.drawLine(hX, paddingTop, hX, paddingTop + plotHeight);

            // Điểm hover nổi bật
            g2.setColor(isDrift ? new Color(220, 38, 38) : new Color(37, 99, 235));
            g2.fillOval(hX - 6, hY - 6, 12, 12);
            g2.setColor(Color.WHITE);
            g2.fillOval(hX - 3, hY - 3, 6, 6);

            // Hộp Tooltip tương thích theo chỉ số
            String line1 = "TID: " + hCp.getTid();
            String line2;
            String line3;
            String line4;
            String line5;
            DriftResult driftResult = driftResultSnapshot.get(hCp.getTid());

            if (metricMode == MetricMode.RAM_USAGE) {
                line2 = String.format(Locale.US, "RAM Heap: %.2f MB", hCp.getRamUsageMb());
                line3 = String.format(Locale.US, "Tốc độ: %,d tx/s | HUI: %d", hCp.getThroughputTxPerSec(), hCp.getHuis().size());
                line4 = isDrift ? "MỐC PHÁT HIỆN TRÔI DẠT" : "LUỒNG DỮ LIỆU ỔN ĐỊNH";
                line5 = String.format(Locale.US, "Khoảng cách DISHS: %.4f", hCp.getGlobalDistance());
            } else if (metricMode == MetricMode.THROUGHPUT) {
                line2 = String.format(Locale.US, "Thông lượng: %,d tx/giây", hCp.getThroughputTxPerSec());
                line3 = String.format(Locale.US, "RAM Heap: %.1f MB | HUI: %d", hCp.getRamUsageMb(), hCp.getHuis().size());
                line4 = isDrift ? "MỐC PHÁT HIỆN TRÔI DẠT" : "LUỒNG DỮ LIỆU ỔN ĐỊNH";
                line5 = String.format(Locale.US, "Khoảng cách DISHS: %.4f", hCp.getGlobalDistance());
            } else if (metricMode == MetricMode.HUI_COUNT || metricMode == MetricMode.BAR_HUI_COUNT) {
                line2 = String.format(Locale.US, "Số lượng HUI: %d tập mục", hCp.getHuis().size());
                line3 = String.format(Locale.US, "RAM: %.1f MB | Tốc độ: %,d tx/s", hCp.getRamUsageMb(), hCp.getThroughputTxPerSec());
                line4 = isDrift ? "MỐC PHÁT HIỆN TRÔI DẠT" : "LUỒNG DỮ LIỆU ỔN ĐỊNH";
                line5 = String.format(Locale.US, "Khoảng cách DISHS: %.4f", hCp.getGlobalDistance());
            } else {
                line2 = String.format(Locale.US, "Khoảng cách DISHS: %.4f", hCp.getGlobalDistance());
                line3 = String.format(Locale.US, "HUI: %d | RAM: %.1f MB | Tốc độ: %,d tx/s", hCp.getHuis().size(), hCp.getRamUsageMb(), hCp.getThroughputTxPerSec());
                line4 = isDrift ? "TRÔI DẠT TOÀN CỤC (GLOBAL DRIFT)" : "ỔN ĐỊNH (STABLE)";
                line5 = driftResult == null
                        ? (isDrift ? "Phát hiện Trôi dạt Toàn cục" : "Hệ thống trạng thái Ổn định")
                        : String.format(Locale.US, "|U-V|=%.3g >= epsilon=%.3g",
                                driftResult.getStatistic(), driftResult.getThreshold());
            }

            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int boxWidth = Math.max(Math.max(fm.stringWidth(line1), fm.stringWidth(line2)),
                    Math.max(Math.max(fm.stringWidth(line3), fm.stringWidth(line4)), fm.stringWidth(line5))) + 18;
            int boxHeight = 84;
            int boxX = Math.min(width - boxWidth - 10, Math.max(10, hX - boxWidth / 2));
            int boxY = (hY - boxHeight - 12 < paddingTop) ? (hY + 14) : (hY - boxHeight - 12);
            if (boxY + boxHeight > height - 10) {
                boxY = height - boxHeight - 10;
            }

            g2.setColor(new Color(15, 23, 42, 235));
            g2.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);
            g2.setColor(new Color(51, 65, 85));
            g2.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);

            g2.setColor(Color.WHITE);
            g2.drawString(line1, boxX + 9, boxY + 14);
            g2.setColor(new Color(56, 189, 248));
            g2.drawString(line2, boxX + 9, boxY + 29);
            g2.setColor(new Color(226, 232, 240));
            g2.drawString(line3, boxX + 9, boxY + 45);
            g2.setColor(isDrift ? new Color(248, 113, 113) : new Color(74, 222, 128));
            g2.drawString(line4, boxX + 9, boxY + 61);
            g2.setColor(new Color(203, 213, 225));
            g2.drawString(line5, boxX + 9, boxY + 76);
        }

        g2.dispose();
    }

    private void drawLegend(Graphics2D g2, int rightEdgeX, int y) {
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        FontMetrics fm = g2.getFontMetrics();

        Color normalColor;
        String normalText;
        Color driftColor = new Color(220, 38, 38);
        String driftText;
        boolean isBar = (metricMode == MetricMode.BAR_HUI_COUNT);

        switch (metricMode) {
            case GLOBAL_DISTANCE -> {
                normalColor = new Color(37, 99, 235);
                normalText = "DISHS (Ổn định)";
                driftText = "Trôi dạt toàn cục";
            }
            case HUI_COUNT -> {
                normalColor = new Color(37, 99, 235);
                normalText = "Số lượng HUI";
                driftText = "Mốc có Drift";
            }
            case BAR_HUI_COUNT -> {
                normalColor = new Color(59, 130, 246);
                normalText = "HUI khi Ổn định";
                driftText = "HUI khi Trôi dạt";
            }
            case RAM_USAGE -> {
                normalColor = new Color(5, 150, 105);
                normalText = "RAM Heap (Ổn định)";
                driftText = "RAM khi có Drift";
            }
            case THROUGHPUT -> {
                normalColor = new Color(126, 34, 206);
                normalText = "Tốc độ (Ổn định)";
                driftText = "Tốc độ khi có Drift";
            }
            default -> {
                normalColor = new Color(37, 99, 235);
                normalText = "Ổn định";
                driftText = "Trôi dạt";
            }
        }

        int symbolSize = 8;
        int gap = 5;
        int item1W = symbolSize + gap + fm.stringWidth(normalText);
        int item2W = symbolSize + gap + fm.stringWidth(driftText);
        int spacing = 12;
        int totalW = item1W + spacing + item2W;

        int startX = rightEdgeX - totalW;

        // Vẽ Item 1 (Normal)
        g2.setColor(normalColor);
        if (isBar) {
            g2.fillRect(startX, y + 1, symbolSize, symbolSize);
        } else {
            g2.fillOval(startX, y + 1, symbolSize, symbolSize);
        }
        g2.setColor(new Color(51, 65, 85));
        g2.drawString(normalText, startX + symbolSize + gap, y + symbolSize);

        // Vẽ Item 2 (Drift)
        int x2 = startX + item1W + spacing;
        g2.setColor(driftColor);
        if (isBar) {
            g2.fillRect(x2, y + 1, symbolSize, symbolSize);
        } else {
            g2.fillOval(x2, y + 1, symbolSize, symbolSize);
            g2.setColor(new Color(239, 68, 68, 120));
            g2.drawOval(x2 - 1, y, symbolSize + 2, symbolSize + 2);
        }
        g2.setColor(new Color(51, 65, 85));
        g2.drawString(driftText, x2 + symbolSize + gap, y + symbolSize);
    }

    public static List<Checkpoint> sortCheckpointsByTid(List<Checkpoint> data) {
        List<Checkpoint> sorted = new ArrayList<>(data);
        sorted.sort(Comparator.comparingInt(Checkpoint::getTid));
        return sorted;
    }

    public static boolean isGlobalDrift(DriftResult result) {
        return result != null
                && result.isDetected()
                && result.getType() == DriftResult.DriftType.GLOBAL_DRIFT;
    }

    public static double transformMetric(double value) {
        return Math.copySign(Math.log1p(Math.abs(value)), value);
    }

    public static double inverseTransformMetric(double value) {
        return Math.copySign(Math.expm1(Math.abs(value)), value);
    }

    private double applyMetricTransform(double value) {
        if (metricMode == MetricMode.RAM_USAGE || metricMode == MetricMode.THROUGHPUT
                || metricMode == MetricMode.HUI_COUNT || metricMode == MetricMode.BAR_HUI_COUNT) {
            return value;
        }
        return transformMetric(value);
    }

    private double applyInverseMetricTransform(double value) {
        if (metricMode == MetricMode.RAM_USAGE || metricMode == MetricMode.THROUGHPUT
                || metricMode == MetricMode.HUI_COUNT || metricMode == MetricMode.BAR_HUI_COUNT) {
            return value;
        }
        return inverseTransformMetric(value);
    }

    public static int xForTid(int tid,
                              List<Checkpoint> sortedCheckpoints,
                              int paddingLeft,
                              int plotWidth) {
        int firstTid = sortedCheckpoints.get(0).getTid();
        int lastTid = sortedCheckpoints.get(sortedCheckpoints.size() - 1).getTid();
        if (firstTid == lastTid) {
            return paddingLeft + plotWidth / 2;
        }
        double ratio = ((double) tid - firstTid) / ((double) lastTid - firstTid);
        return paddingLeft + (int) Math.round(ratio * plotWidth);
    }

    static String formatYAxisValue(double val) {
        double absVal = Math.abs(val);
        if (absVal >= 1_000_000) {
            return String.format(Locale.US, "%.2fM", val / 1_000_000.0);
        } else if (absVal >= 10_000) {
            return String.format(Locale.US, "%.1fK", val / 1_000.0);
        } else if (absVal >= 1_000) {
            return String.format(Locale.US, "%.0f", val);
        } else if (absVal >= 10) {
            return String.format(Locale.US, "%.1f", val);
        } else if (absVal >= 0.01 || val == 0.0) {
            return String.format(Locale.US, "%.2f", val);
        } else {
            return String.format(Locale.US, "%.3g", val);
        }
    }
}
