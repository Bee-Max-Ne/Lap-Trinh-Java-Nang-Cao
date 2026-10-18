package huddtds.demo;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;
import javax.swing.JPanel;

/**
 * Panel đồ họa trực quan hóa số liệu phân bố Trôi dạt (Drift Donut Chart)
 * và Tài nguyên & Hiệu năng hệ thống (RAM Heap, Thông lượng, Garbage Collection).
 * Giúp người dùng so sánh trực quan hiệu quả trước và sau khi áp dụng Design Patterns.
 */
public class SummaryChartsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private int totalCp = 0;
    private int globalDrifts = 0;
    private int localDrifts = 0;

    private double currentRamMb = 0.0;
    private double peakRamMb = 0.0;
    private double maxRamMb = 0.0;

    private int avgTps = 0;
    private int peakTps = 0;

    private long gcCount = 0;
    private long gcTimeMs = 0;

    public SummaryChartsPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(380, 420));
        setMinimumSize(new Dimension(320, 360));
    }

    public synchronized void updateData(
            int totalCp, int gDrift, int lDrift,
            double currentRamMb, double peakRamMb, double maxRamMb,
            int avgTps, int peakTps,
            long gcCount, long gcTimeMs
    ) {
        this.totalCp = totalCp;
        this.globalDrifts = gDrift;
        this.localDrifts = lDrift;
        this.currentRamMb = currentRamMb;
        this.peakRamMb = peakRamMb;
        this.maxRamMb = maxRamMb;
        this.avgTps = avgTps;
        this.peakTps = peakTps;
        this.gcCount = gcCount;
        this.gcTimeMs = gcTimeMs;
        repaint();
    }

    public synchronized void reset() {
        this.totalCp = 0;
        this.globalDrifts = 0;
        this.localDrifts = 0;
        this.currentRamMb = 0.0;
        this.peakRamMb = 0.0;
        this.maxRamMb = 0.0;
        this.avgTps = 0;
        this.peakTps = 0;
        this.gcCount = 0;
        this.gcTimeMs = 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Nền chung
        g2.setColor(new Color(248, 250, 252));
        g2.fillRect(0, 0, w, h);

        int padding = 10;
        int cardWidth = w - padding * 2;
        int halfHeight = (h - padding * 3) / 2;

        // Card 1: Biểu đồ Donut tỷ lệ trôi dạt
        drawDriftDonutCard(g2, padding, padding, cardWidth, Math.max(180, halfHeight));

        // Card 2: Biểu đồ thanh tài nguyên & hiệu năng
        int card2Y = padding * 2 + Math.max(180, halfHeight);
        int card2H = h - card2Y - padding;
        if (card2H >= 140) {
            drawResourceMetersCard(g2, padding, card2Y, cardWidth, card2H);
        }

        g2.dispose();
    }

    private void drawDriftDonutCard(Graphics2D g2, int x, int y, int width, int height) {
        // Khung viền Card
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(x, y, width, height, 10, 10));
        g2.setColor(new Color(226, 232, 240));
        g2.draw(new RoundRectangle2D.Float(x, y, width, height, 10, 10));

        // Tiêu đề Card
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(new Color(30, 41, 59));
        g2.drawString("📊 TỶ LỆ TRÔI DẠT (CONCEPT DRIFT DISTRIBUTION)", x + 12, y + 20);

        int stableCount = Math.max(0, totalCp - globalDrifts - localDrifts);
        double driftRate = totalCp > 0 ? (((double) (globalDrifts + localDrifts) / totalCp) * 100.0) : 0.0;

        int donutDiameter = Math.min(130, Math.min(height - 40, (width / 2) - 10));
        donutDiameter = Math.max(80, donutDiameter);
        int donutX = x + 16;
        int donutY = y + 28 + (height - 35 - donutDiameter) / 2;

        if (totalCp == 0) {
            // Chưa có dữ liệu
            g2.setColor(new Color(226, 232, 240));
            g2.setStroke(new BasicStroke(16));
            g2.drawOval(donutX + 8, donutY + 8, donutDiameter - 16, donutDiameter - 16);
            g2.setStroke(new BasicStroke(1));

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(new Color(148, 163, 184));
            FontMetrics fm = g2.getFontMetrics();
            String msg = "Chưa có";
            g2.drawString(msg, donutX + (donutDiameter - fm.stringWidth(msg)) / 2, donutY + donutDiameter / 2);
            String msg2 = "dữ liệu";
            g2.drawString(msg2, donutX + (donutDiameter - fm.stringWidth(msg2)) / 2, donutY + donutDiameter / 2 + 14);
        } else {
            // Vẽ các lát cắt Donut
            double stableAngle = (stableCount * 360.0) / totalCp;
            double globalAngle = (globalDrifts * 360.0) / totalCp;
            double localAngle = (localDrifts * 360.0) / totalCp;

            // Đảm bảo tổng góc đủ 360 độ
            double curAngle = 90.0; // Bắt đầu từ góc 12 giờ

            Shape outer = new Ellipse2D.Double(donutX, donutY, donutDiameter, donutDiameter);
            int holeSize = (int) (donutDiameter * 0.58);
            int holeX = donutX + (donutDiameter - holeSize) / 2;
            int holeY = donutY + (donutDiameter - holeSize) / 2;
            Shape inner = new Ellipse2D.Double(holeX, holeY, holeSize, holeSize);

            // 1. Stable Arc (Xanh lá)
            if (stableCount > 0) {
                drawDonutSlice(g2, outer, inner, donutX, donutY, donutDiameter, curAngle, stableAngle, new Color(16, 185, 129));
                curAngle += stableAngle;
            }

            // 2. Local Drift Arc (Vàng Cam)
            if (localDrifts > 0) {
                drawDonutSlice(g2, outer, inner, donutX, donutY, donutDiameter, curAngle, localAngle, new Color(245, 158, 11));
                curAngle += localAngle;
            }

            // 3. Global Drift Arc (Đỏ)
            if (globalDrifts > 0) {
                drawDonutSlice(g2, outer, inner, donutX, donutY, donutDiameter, curAngle, globalAngle, new Color(239, 68, 68));
            }

            // Vẽ lỗ giữa (Donut hole)
            g2.setColor(Color.WHITE);
            g2.fill(inner);
            g2.setColor(new Color(241, 245, 249));
            g2.draw(inner);

            // Chữ ở trung tâm lỗ Donut
            g2.setColor(new Color(15, 23, 42));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            FontMetrics fmb = g2.getFontMetrics();
            String rateText = String.format(Locale.US, "%.1f%%", driftRate);
            g2.drawString(rateText, holeX + (holeSize - fmb.stringWidth(rateText)) / 2, holeY + (holeSize / 2) - 1);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(new Color(100, 116, 139));
            FontMetrics fmp = g2.getFontMetrics();
            String subText = "Trôi dạt";
            g2.drawString(subText, holeX + (holeSize - fmp.stringWidth(subText)) / 2, holeY + (holeSize / 2) + 13);
        }

        // Chú giải (Legend) ở bên phải Donut
        int legendX = donutX + donutDiameter + 16;
        int legendY = y + 36;
        int legendBoxSize = 10;

        // Dòng 1: Ổn định
        drawLegendItem(g2, legendX, legendY, legendBoxSize, new Color(16, 185, 129),
                "Ổn định", stableCount, totalCp > 0 ? (stableCount * 100.0 / totalCp) : 0.0);

        // Dòng 2: Global Drift
        drawLegendItem(g2, legendX, legendY + 24, legendBoxSize, new Color(239, 68, 68),
                "Global Drift", globalDrifts, totalCp > 0 ? (globalDrifts * 100.0 / totalCp) : 0.0);

        // Dòng 3: Local Drift
        drawLegendItem(g2, legendX, legendY + 48, legendBoxSize, new Color(245, 158, 11),
                "Local Drift", localDrifts, totalCp > 0 ? (localDrifts * 100.0 / totalCp) : 0.0);

        // Dòng 4: Tổng số
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(new Color(71, 85, 105));
        g2.drawString(String.format(Locale.US, "Tổng: %,d Checkpoints", totalCp), legendX, legendY + 76);
    }

    private void drawDonutSlice(Graphics2D g2, Shape outer, Shape inner, int ox, int oy, int diam, double startAngle, double angleExt, Color color) {
        Arc2D.Double arc = new Arc2D.Double(ox, oy, diam, diam, startAngle, angleExt, Arc2D.PIE);
        Area slice = new Area(arc);
        slice.intersect(new Area(outer));
        slice.subtract(new Area(inner));

        g2.setColor(color);
        g2.fill(slice);
    }

    private void drawLegendItem(Graphics2D g2, int x, int y, int boxSize, Color color, String label, int count, double pct) {
        g2.setColor(color);
        g2.fill(new RoundRectangle2D.Float(x, y, boxSize, boxSize, 3, 3));

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(51, 65, 85));
        String text = String.format(Locale.US, "%s: %,d (%.1f%%)", label, count, pct);
        g2.drawString(text, x + boxSize + 6, y + boxSize - 1);
    }

    private void drawResourceMetersCard(Graphics2D g2, int x, int y, int width, int height) {
        // Khung viền Card
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(x, y, width, height, 10, 10));
        g2.setColor(new Color(226, 232, 240));
        g2.draw(new RoundRectangle2D.Float(x, y, width, height, 10, 10));

        // Tiêu đề Card kèm chú thích vạch đỏ
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(new Color(30, 41, 59));
        g2.drawString("⚡ TIÊU THỤ TÀI NGUYÊN & HIỆU NĂNG", x + 12, y + 20);

        // Chú giải vạch đỏ
        g2.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        g2.setColor(new Color(220, 38, 38));
        String redMarkNote = "[Vạch đỏ | : Mức Đỉnh]";
        g2.drawString(redMarkNote, x + width - g2.getFontMetrics().stringWidth(redMarkNote) - 14, y + 20);

        int barX = x + 14;
        int barWidth = width - 28;
        int curY = y + 38;

        // 1. THANH RAM HEAP
        double ramRatio = maxRamMb > 0 ? Math.min(1.0, currentRamMb / maxRamMb) : 0.0;
        double peakRatio = maxRamMb > 0 ? Math.min(1.0, peakRamMb / maxRamMb) : 0.0;
        String ramLabel = String.format(Locale.US, "RAM Heap: %.1f MB (Đỉnh: %.1f MB | Max: %.0f MB)", currentRamMb, peakRamMb, maxRamMb);
        drawProgressBar(g2, barX, curY, barWidth, 14, ramRatio, peakRatio,
                new Color(59, 130, 246), new Color(99, 102, 241), ramLabel,
                String.format(Locale.US, "%.1f%% Heap", ramRatio * 100));

        curY += 40;

        // 2. THANH THÔNG LƯỢNG (THROUGHPUT)
        double tpsRatio = peakTps > 0 ? Math.min(1.0, (double) avgTps / peakTps) : 0.0;
        String tpsLabel = String.format(Locale.US, "Thông lượng: %,d tx/s (Đỉnh: %,d tx/s)", avgTps, peakTps);
        drawProgressBar(g2, barX, curY, barWidth, 14, tpsRatio, 1.0,
                new Color(139, 92, 246), new Color(168, 85, 247), tpsLabel,
                avgTps > 0 ? String.format(Locale.US, "%,d tx/s", avgTps) : "0");

        curY += 40;

        // 3. THU GOM RÁC (GARBAGE COLLECTION) THỐNG KÊ
        if (curY + 28 <= y + height) {
            drawGcStatusBadge(g2, barX, curY, barWidth);
        }
    }

    private void drawProgressBar(Graphics2D g2, int x, int y, int width, int barHeight,
                                double ratio, double peakRatio, Color startColor, Color endColor,
                                String headerText, String valText) {
        // Tiêu đề phía trên thanh
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(71, 85, 105));
        g2.drawString(headerText, x, y - 4);

        int barY = y;
        // Thanh nền xám
        g2.setColor(new Color(241, 245, 249));
        g2.fill(new RoundRectangle2D.Float(x, barY, width, barHeight, 6, 6));
        g2.setColor(new Color(203, 213, 225));
        g2.draw(new RoundRectangle2D.Float(x, barY, width, barHeight, 6, 6));

        // Thanh tiến độ có gradient
        int fillW = (int) (width * Math.max(0.0, Math.min(1.0, ratio)));
        if (fillW > 0) {
            GradientPaint gp = new GradientPaint(x, barY, startColor, x + fillW, barY, endColor);
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(x, barY, fillW, barHeight, 6, 6));
        }

        // Vạch đánh dấu Đỉnh (Peak)
        if (peakRatio > 0.0 && peakRatio <= 1.0) {
            int peakX = x + (int) (width * peakRatio);
            g2.setColor(new Color(220, 38, 38));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(peakX, barY - 2, peakX, barY + barHeight + 2);
            g2.setStroke(new BasicStroke(1.0f));
        }

        // Chữ phần trăm / giá trị bên phải
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.setColor(new Color(30, 41, 59));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(valText, x + width - fm.stringWidth(valText) - 4, barY + barHeight - 3);
    }

    private void drawGcStatusBadge(Graphics2D g2, int x, int y, int width) {
        g2.setColor(new Color(241, 245, 249));
        g2.fill(new RoundRectangle2D.Float(x, y, width, 26, 6, 6));
        g2.setColor(new Color(226, 232, 240));
        g2.draw(new RoundRectangle2D.Float(x, y, width, 26, 6, 6));

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(51, 65, 85));

        String gcText = String.format(Locale.US, "♻ GC: %d lần thu gom | Tạm dừng: %d ms | Kiến trúc: Baseline (No Patterns)",
                gcCount, gcTimeMs);
        g2.drawString(gcText, x + 8, y + 17);
    }
}
