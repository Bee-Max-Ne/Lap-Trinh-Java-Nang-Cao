package huddtds.demo;

import huddtds.model.Checkpoint;
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
    private MetricMode metricMode = MetricMode.GLOBAL_DISTANCE;

    // Hover state
    private Point mousePoint = null;
    private int hoveredIndex = -1;

    public ChartPanel() {
        this.checkpoints = new ArrayList<>();
        this.driftTids = new HashSet<>();
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
        if (data != null) {
            checkpoints.addAll(data);
        }
        hoveredIndex = -1;
        repaint();
    }

    public synchronized void addCheckpoint(Checkpoint cp) {
        addCheckpoint(cp, false);
    }

    public synchronized void addCheckpoint(Checkpoint cp, boolean isGlobalDrift) {
        if (cp != null) {
            checkpoints.add(cp);
            if (isGlobalDrift) {
                driftTids.add(cp.getTid());
            }
            repaint();
        }
    }

    public synchronized void markDrift(int tid) {
        driftTids.add(tid);
        repaint();
    }

    public synchronized void clear() {
        checkpoints.clear();
        driftTids.clear();
        hoveredIndex = -1;
        repaint();
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

        int bestIdx = -1;
        double bestDist = 24.0; // bán kính bắt điểm
        for (int i = 0; i < checkpoints.size(); i++) {
            int x = paddingLeft + (int) ((double) i / Math.max(1, checkpoints.size() - 1) * plotWidth);
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
        g2.drawString(metricMode.getTitle(), paddingLeft, 22);

        // Chú giải (Legend) ở góc trên phải
        drawLegend(g2, width - paddingRight - 160, 10);

        List<Checkpoint> snapshot;
        Set<Integer> driftSnapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(checkpoints);
            driftSnapshot = new HashSet<>(driftTids);
        }

        if (snapshot.isEmpty() || plotWidth <= 0 || plotHeight <= 0) {
            g2.setColor(new Color(148, 163, 184));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString("Chưa có dữ liệu checkpoint để vẽ đồ thị.", paddingLeft + 15, paddingTop + 40);
            g2.dispose();
            return;
        }

        // Tìm min/max
        double minValue = Double.POSITIVE_INFINITY;
        double maxValue = Double.NEGATIVE_INFINITY;
        for (Checkpoint cp : snapshot) {
            double v = getMetricValue(cp);
            minValue = Math.min(minValue, v);
            maxValue = Math.max(maxValue, v);
        }

        if (minValue > maxValue) {
            minValue = 0.0;
            maxValue = 1.0;
        }
        double valueRange = maxValue - minValue;
        if (valueRange == 0.0) {
            valueRange = Math.max(1.0, Math.abs(maxValue) * 0.2);
        }
        double chartMin = Math.max(0.0, minValue - valueRange * 0.15);
        double chartMax = maxValue + valueRange * 0.15;
        if (chartMax <= chartMin) chartMax = chartMin + 1.0;

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
            double val = chartMax - ratio * (chartMax - chartMin);

            g2.setColor(new Color(241, 245, 249));
            g2.drawLine(paddingLeft, y, paddingLeft + plotWidth, y);

            g2.setColor(new Color(100, 116, 139));
            String label = (metricMode == MetricMode.HUI_COUNT)
                    ? String.format(Locale.US, "%.0f", val)
                    : String.format(Locale.US, "%.3f", val);
            g2.drawString(label, 6, y + 4);
        }

        // Tính tọa độ các điểm
        int n = snapshot.size();
        int[] xCoords = new int[n];
        int[] yCoords = new int[n];

        for (int i = 0; i < n; i++) {
            xCoords[i] = paddingLeft + (int) ((double) i / Math.max(1, n - 1) * plotWidth);
            double normalized = (getMetricValue(snapshot.get(i)) - chartMin) / (chartMax - chartMin);
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
                g2.drawString("T" + cp.getTid(), x - 12, paddingTop + plotHeight + 18);
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
            String line3 = isDrift ? "⚡ TRÔI DẠT (DRIFT)" : "✓ ỔN ĐỊNH";

            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int boxWidth = Math.max(fm.stringWidth(line1), Math.max(fm.stringWidth(line2), fm.stringWidth(line3))) + 16;
            int boxHeight = 52;
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
            g2.drawString(line3, boxX + 8, boxY + 45);
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
        g2.drawString("Trôi dạt (Drift)", x + 76, y + 9);
    }
}

