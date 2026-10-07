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

    public enum MetricMode {
        GLOBAL_DISTANCE("Khoảng cách toàn cục (DISHS)"),
        HUI_COUNT("Số lượng HUI");

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

    private synchronized double getMetricValue(Checkpoint cp) {
        if (metricMode == MetricMode.HUI_COUNT) {
            return cp.getHuis() != null ? cp.getHuis().size() : 0.0;
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

        int paddingLeft = 60;
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
        g2.drawString(metricMode.getTitle() + " (log1p)", paddingLeft, 22);

        // Chú giải (Legend) ở góc trên phải
        drawLegend(g2, width - paddingRight - 140, 10);

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
            double transformed = transformMetric(getMetricValue(cp));
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
            double val = inverseTransformMetric(chartMax - ratio * (chartMax - chartMin));

            g2.setColor(new Color(241, 245, 249));
            g2.drawLine(paddingLeft, y, paddingLeft + plotWidth, y);

            g2.setColor(new Color(100, 116, 139));
            String label = (metricMode == MetricMode.HUI_COUNT)
                    ? String.format(Locale.US, "%.0f", val)
                    : formatYAxisValue(val);
            g2.drawString(label, 6, y + 4);
        }

        // Tính tọa độ các điểm
        int n = snapshot.size();
        int[] xCoords = new int[n];
        int[] yCoords = new int[n];

        for (int i = 0; i < n; i++) {
            xCoords[i] = xForTid(snapshot.get(i).getTid(), snapshot, paddingLeft, plotWidth);
            double transformed = transformMetric(getMetricValue(snapshot.get(i)));
            double normalized = (transformed - chartMin) / (chartMax - chartMin);
            yCoords[i] = paddingTop + plotHeight - (int) (normalized * plotHeight);
        }

        // Vẽ gradient diện tích dưới đường line
        if (n > 1) {
            Path2D area = new Path2D.Double();
            area.moveTo(xCoords[0], paddingTop + plotHeight);
            for (int i = 0; i < n; i++) {
                area.lineTo(xCoords[i], yCoords[i]);
            }
            area.lineTo(xCoords[n - 1], paddingTop + plotHeight);
            area.closePath();

            Color startCol = new Color(59, 130, 246, 75);
            Color endCol = new Color(59, 130, 246, 5);
            g2.setPaint(new GradientPaint(0, paddingTop, startCol, 0, paddingTop + plotHeight, endCol));
            g2.fill(area);
        }

        // Vẽ đường nối
        g2.setColor(new Color(37, 99, 235));
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < n - 1; i++) {
            g2.drawLine(xCoords[i], yCoords[i], xCoords[i + 1], yCoords[i + 1]);
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

            // Điểm tròn
            if (isDrift) {
                // Điểm Drift màu đỏ nổi bật với quầng cảnh báo
                g2.setColor(new Color(239, 68, 68, 90));
                g2.fillOval(x - 8, y - 8, 16, 16);
                g2.setColor(new Color(220, 38, 38));
                g2.fillOval(x - 5, y - 5, 10, 10);
                g2.setColor(Color.WHITE);
                g2.fillOval(x - 2, y - 2, 4, 4);
            } else {
                g2.setColor(Color.WHITE);
                g2.fillOval(x - 4, y - 4, 8, 8);
                g2.setColor(new Color(37, 99, 235));
                g2.fillOval(x - 3, y - 3, 6, 6);
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

            // Hộp Tooltip
            String line1 = "TID: " + hCp.getTid();
            String line2 = String.format(Locale.US, "DISHS: %.4f | HUI: %d", hCp.getGlobalDistance(), hCp.getHuis().size());
            DriftResult driftResult = driftResultSnapshot.get(hCp.getTid());
            String line3 = isDrift ? "TRÔI DẠT TOÀN CỤC" : "ỔN ĐỊNH";
            String line4 = driftResult == null
                    ? "Đánh dấu theo kết quả kiểm định drift"
                    : String.format(Locale.US, "|U-V|=%.3g >= epsilon=%.3g",
                            driftResult.getStatistic(), driftResult.getThreshold());

            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int boxWidth = Math.max(Math.max(fm.stringWidth(line1), fm.stringWidth(line2)),
                    Math.max(fm.stringWidth(line3), fm.stringWidth(line4))) + 16;
            int boxHeight = 66;
            int boxX = Math.min(width - boxWidth - 10, Math.max(10, hX - boxWidth / 2));
            int boxY = Math.max(paddingTop + 5, hY - boxHeight - 10);

            g2.setColor(new Color(15, 23, 42, 225));
            g2.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);
            g2.setColor(new Color(51, 65, 85));
            g2.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);

            g2.setColor(Color.WHITE);
            g2.drawString(line1, boxX + 8, boxY + 15);
            g2.setColor(new Color(226, 232, 240));
            g2.drawString(line2, boxX + 8, boxY + 30);
            g2.setColor(isDrift ? new Color(248, 113, 113) : new Color(74, 222, 128));
            g2.drawString(line3, boxX + 8, boxY + 44);
            g2.setColor(new Color(226, 232, 240));
            g2.drawString(line4, boxX + 8, boxY + 58);
        }

        g2.dispose();
    }

    private void drawLegend(Graphics2D g2, int x, int y) {
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        // Normal
        g2.setColor(new Color(37, 99, 235));
        g2.fillOval(x, y + 2, 7, 7);
        g2.setColor(new Color(71, 85, 105));
        g2.drawString("Ổn định", x + 11, y + 9);

        // Drift
        g2.setColor(new Color(220, 38, 38));
        g2.fillOval(x + 65, y + 2, 7, 7);
        g2.setColor(new Color(71, 85, 105));
        g2.drawString("Trôi dạt toàn cục", x + 76, y + 9);
    }

    static List<Checkpoint> sortCheckpointsByTid(List<Checkpoint> data) {
        List<Checkpoint> sorted = new ArrayList<>(data);
        sorted.sort(Comparator.comparingInt(Checkpoint::getTid));
        return sorted;
    }

    static boolean isGlobalDrift(DriftResult result) {
        return result != null
                && result.isDetected()
                && result.getType() == DriftResult.DriftType.GLOBAL_DRIFT;
    }

    static double transformMetric(double value) {
        return Math.copySign(Math.log1p(Math.abs(value)), value);
    }

    static double inverseTransformMetric(double value) {
        return Math.copySign(Math.expm1(Math.abs(value)), value);
    }

    static int xForTid(int tid,
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
