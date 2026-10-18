package huddtds.demo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.TableModel;

/**
 * Tiện ích xuất báo cáo toàn diện cho hệ thống HUDD-TDS:
 * - Xuất Dashboard báo cáo HTML tự chứa (Self-contained HTML Report) nhúng sẵn ảnh Base64
 *   của toàn bộ các biểu đồ phân tích và bảng số liệu thực nghiệm.
 * - Hỗ trợ in trực tiếp hoặc lưu thành file PDF chuẩn A4 qua trình duyệt.
 * - Hỗ trợ xuất ảnh PNG từng biểu đồ hoặc toàn bộ bộ ảnh biểu đồ.
 */
public class ReportExporter {

    /**
     * Chụp lại giao diện của một Component Swing thành BufferedImage.
     */
    public static BufferedImage captureComponent(Component comp, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        Dimension originalSize = comp.getSize();
        comp.setSize(width, height);
        comp.doLayout();
        comp.validate();
        comp.paint(g2);
        g2.dispose();

        // Khôi phục kích thước ban đầu
        if (originalSize.width > 0 && originalSize.height > 0) {
            comp.setSize(originalSize);
            comp.doLayout();
            comp.validate();
        }
        return image;
    }

    /**
     * Chuyển đổi BufferedImage thành chuỗi Data URI Base64 dạng PNG.
     */
    public static String toBase64Png(BufferedImage img) {
        if (img == null) return "";
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Xuất tệp Báo cáo Dashboard HTML đầy đủ bao gồm số liệu và toàn bộ 6 biểu đồ.
     */
    public static void exportFullHtmlReport(
            Component parent,
            String datasetName,
            String[] kpiValues, // [Time, Throughput, RAM, HUI, DriftRate]
            TableModel statsTableModel,
            String summaryText,
            ChartPanel chartPanel,
            SummaryChartsPanel summaryChartsPanel
    ) {
        JFileChooser chooser = new JFileChooser();
        String defaultName = "HUDD_TDS_FullReport_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".html";
        chooser.setSelectedFile(new File(defaultName));
        chooser.setDialogTitle("Xuất Báo cáo Toàn diện (HTML Dashboard)");

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File targetFile = chooser.getSelectedFile();
        if (!targetFile.getName().toLowerCase().endsWith(".html")) {
            targetFile = new File(targetFile.getParentFile(), targetFile.getName() + ".html");
        }

        try {
            // 1. Chụp ảnh 5 chế độ của ChartPanel
            ChartPanel.MetricMode originalMode = chartPanel.getMetricMode();
            int imgW = 720;
            int imgH = 380;

            chartPanel.setMetricMode(ChartPanel.MetricMode.GLOBAL_DISTANCE);
            BufferedImage imgGlobalDis = captureComponent(chartPanel, imgW, imgH);

            chartPanel.setMetricMode(ChartPanel.MetricMode.HUI_COUNT);
            BufferedImage imgHuiCount = captureComponent(chartPanel, imgW, imgH);

            chartPanel.setMetricMode(ChartPanel.MetricMode.BAR_HUI_COUNT);
            BufferedImage imgBarHui = captureComponent(chartPanel, imgW, imgH);

            chartPanel.setMetricMode(ChartPanel.MetricMode.RAM_USAGE);
            BufferedImage imgRam = captureComponent(chartPanel, imgW, imgH);

            chartPanel.setMetricMode(ChartPanel.MetricMode.THROUGHPUT);
            BufferedImage imgThroughput = captureComponent(chartPanel, imgW, imgH);

            // Khôi phục lại chế độ người dùng đang chọn trên GUI
            chartPanel.setMetricMode(originalMode);
            chartPanel.repaint();

            // 2. Chụp ảnh SummaryChartsPanel
            BufferedImage imgSummary = null;
            if (summaryChartsPanel != null) {
                imgSummary = captureComponent(summaryChartsPanel, imgW, 420);
                summaryChartsPanel.repaint();
            }

            // 3. Tạo mã HTML tự chứa
            String htmlContent = buildHtmlDocument(
                    datasetName,
                    kpiValues,
                    statsTableModel,
                    summaryText,
                    toBase64Png(imgGlobalDis),
                    toBase64Png(imgHuiCount),
                    toBase64Png(imgBarHui),
                    toBase64Png(imgRam),
                    toBase64Png(imgThroughput),
                    toBase64Png(imgSummary)
            );

            // 4. Ghi tệp với mã hóa UTF-8
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {
                writer.write(htmlContent);
            }

            // 5. Thông báo và mở file trong trình duyệt
            int openChoice = JOptionPane.showConfirmDialog(parent,
                    "Xuất báo cáo toàn diện thành công!\n" + targetFile.getAbsolutePath() + "\n\nBạn có muốn mở báo cáo trong trình duyệt ngay bây giờ không?",
                    "Xuất Báo cáo Thành công",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);

            if (openChoice == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(targetFile.toURI());
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "Lỗi khi xuất báo cáo HTML: " + ex.getMessage(),
                    "Lỗi xuất báo cáo",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Lưu ảnh biểu đồ hiện tại ra file PNG.
     */
    public static void exportCurrentChartImage(Component parent, ChartPanel chartPanel) {
        JFileChooser chooser = new JFileChooser();
        String safeTitle = chartPanel.getMetricMode().name().toLowerCase();
        chooser.setSelectedFile(new File("HUDD_TDS_Chart_" + safeTitle + "_" + System.currentTimeMillis() + ".png"));
        chooser.setDialogTitle("Lưu ảnh biểu đồ hiện tại (PNG)");

        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getParentFile(), file.getName() + ".png");
            }
            try {
                BufferedImage img = captureComponent(chartPanel, Math.max(700, chartPanel.getWidth()), Math.max(400, chartPanel.getHeight()));
                ImageIO.write(img, "png", file);
                JOptionPane.showMessageDialog(parent,
                        "Lưu ảnh biểu đồ thành công:\n" + file.getAbsolutePath(),
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent, "Lỗi khi lưu ảnh: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Xuất toàn bộ 5 biểu đồ ra thư mục ảnh PNG riêng biệt.
     */
    public static void exportAllChartsToImages(Component parent, ChartPanel chartPanel, SummaryChartsPanel summaryChartsPanel) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Chọn thư mục lưu bộ ảnh biểu đồ");

        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            if (!dir.exists()) dir.mkdirs();

            try {
                ChartPanel.MetricMode originalMode = chartPanel.getMetricMode();
                int w = 800;
                int h = 420;

                ChartPanel.MetricMode[] modes = ChartPanel.MetricMode.values();
                for (ChartPanel.MetricMode m : modes) {
                    chartPanel.setMetricMode(m);
                    BufferedImage img = captureComponent(chartPanel, w, h);
                    File out = new File(dir, "Chart_" + m.name() + ".png");
                    ImageIO.write(img, "png", out);
                }
                chartPanel.setMetricMode(originalMode);
                chartPanel.repaint();

                if (summaryChartsPanel != null) {
                    BufferedImage sumImg = captureComponent(summaryChartsPanel, 800, 480);
                    File sumOut = new File(dir, "Chart_SUMMARY_DONUT_METERS.png");
                    ImageIO.write(sumImg, "png", sumOut);
                    summaryChartsPanel.repaint();
                }

                JOptionPane.showMessageDialog(parent,
                        "Đã xuất toàn bộ biểu đồ vào thư mục:\n" + dir.getAbsolutePath(),
                        "Xuất hoàn tất", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent, "Lỗi khi xuất ảnh: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static String buildHtmlDocument(
            String datasetName,
            String[] kpi,
            TableModel tableModel,
            String summaryText,
            String b64GlobalDis,
            String b64HuiCount,
            String b64BarHui,
            String b64Ram,
            String b64Throughput,
            String b64Summary
    ) {
        StringBuilder sb = new StringBuilder(64 * 1024);
        String exportTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

        sb.append("<!DOCTYPE html>\n<html lang=\"vi\">\n<head>\n");
        sb.append("    <meta charset=\"UTF-8\">\n");
        sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("    <title>Báo Cáo Thực Nghiệm HUDD-TDS</title>\n");
        sb.append("    <style>\n");
        sb.append("        :root {\n");
        sb.append("            --primary: #2563eb;\n");
        sb.append("            --primary-dark: #1d4ed8;\n");
        sb.append("            --bg: #f8fafc;\n");
        sb.append("            --card-bg: #ffffff;\n");
        sb.append("            --text-main: #0f172a;\n");
        sb.append("            --text-muted: #64748b;\n");
        sb.append("            --border: #e2e8f0;\n");
        sb.append("        }\n");
        sb.append("        * { box-sizing: border-box; margin: 0; padding: 0; }\n");
        sb.append("        body {\n");
        sb.append("            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\n");
        sb.append("            background-color: var(--bg);\n");
        sb.append("            color: var(--text-main);\n");
        sb.append("            padding: 24px;\n");
        sb.append("            line-height: 1.5;\n");
        sb.append("        }\n");
        sb.append("        .container { max-width: 1200px; margin: 0 auto; }\n");
        sb.append("        .header {\n");
        sb.append("            background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);\n");
        sb.append("            color: white;\n");
        sb.append("            padding: 28px 32px;\n");
        sb.append("            border-radius: 14px;\n");
        sb.append("            margin-bottom: 24px;\n");
        sb.append("            box-shadow: 0 4px 16px rgba(0,0,0,0.08);\n");
        sb.append("            display: flex;\n");
        sb.append("            justify-content: space-between;\n");
        sb.append("            align-items: center;\n");
        sb.append("            flex-wrap: wrap;\n");
        sb.append("            gap: 16px;\n");
        sb.append("        }\n");
        sb.append("        .header h1 { font-size: 22px; font-weight: 700; letter-spacing: -0.5px; }\n");
        sb.append("        .header p { color: #94a3b8; font-size: 13px; margin-top: 4px; }\n");
        sb.append("        .btn-print {\n");
        sb.append("            background: #3b82f6;\n");
        sb.append("            color: white;\n");
        sb.append("            border: none;\n");
        sb.append("            padding: 10px 18px;\n");
        sb.append("            border-radius: 8px;\n");
        sb.append("            font-size: 13px;\n");
        sb.append("            font-weight: 600;\n");
        sb.append("            cursor: pointer;\n");
        sb.append("            box-shadow: 0 2px 6px rgba(59,130,246,0.3);\n");
        sb.append("            transition: all 0.2s;\n");
        sb.append("        }\n");
        sb.append("        .btn-print:hover { background: #2563eb; }\n");
        sb.append("        .kpi-grid {\n");
        sb.append("            display: grid;\n");
        sb.append("            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n");
        sb.append("            gap: 14px;\n");
        sb.append("            margin-bottom: 28px;\n");
        sb.append("        }\n");
        sb.append("        .kpi-card {\n");
        sb.append("            background: var(--card-bg);\n");
        sb.append("            padding: 18px 20px;\n");
        sb.append("            border-radius: 12px;\n");
        sb.append("            border: 1px solid var(--border);\n");
        sb.append("            box-shadow: 0 2px 8px rgba(0,0,0,0.02);\n");
        sb.append("        }\n");
        sb.append("        .kpi-card .title { font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; }\n");
        sb.append("        .kpi-card .value { font-size: 20px; font-weight: 700; color: var(--text-main); margin-top: 6px; }\n");
        sb.append("        .section-title {\n");
        sb.append("            font-size: 17px;\n");
        sb.append("            font-weight: 700;\n");
        sb.append("            color: #1e293b;\n");
        sb.append("            margin-bottom: 14px;\n");
        sb.append("            display: flex;\n");
        sb.append("            align-items: center;\n");
        sb.append("            gap: 8px;\n");
        sb.append("        }\n");
        sb.append("        .charts-grid {\n");
        sb.append("            display: grid;\n");
        sb.append("            grid-template-columns: repeat(auto-fit, minmax(540px, 1fr));\n");
        sb.append("            gap: 20px;\n");
        sb.append("            margin-bottom: 32px;\n");
        sb.append("        }\n");
        sb.append("        .chart-card {\n");
        sb.append("            background: var(--card-bg);\n");
        sb.append("            border-radius: 12px;\n");
        sb.append("            border: 1px solid var(--border);\n");
        sb.append("            overflow: hidden;\n");
        sb.append("            box-shadow: 0 2px 8px rgba(0,0,0,0.03);\n");
        sb.append("        }\n");
        sb.append("        .chart-card .card-header {\n");
        sb.append("            padding: 12px 18px;\n");
        sb.append("            background: #f8fafc;\n");
        sb.append("            border-bottom: 1px solid var(--border);\n");
        sb.append("            font-size: 13px;\n");
        sb.append("            font-weight: 600;\n");
        sb.append("            color: #334155;\n");
        sb.append("        }\n");
        sb.append("        .chart-card img {\n");
        sb.append("            width: 100%;\n");
        sb.append("            height: auto;\n");
        sb.append("            display: block;\n");
        sb.append("        }\n");
        sb.append("        .table-card {\n");
        sb.append("            background: var(--card-bg);\n");
        sb.append("            border-radius: 12px;\n");
        sb.append("            border: 1px solid var(--border);\n");
        sb.append("            overflow: hidden;\n");
        sb.append("            margin-bottom: 32px;\n");
        sb.append("            box-shadow: 0 2px 8px rgba(0,0,0,0.02);\n");
        sb.append("        }\n");
        sb.append("        table { width: 100%; border-collapse: collapse; font-size: 13px; }\n");
        sb.append("        th, td { padding: 10px 18px; text-align: left; border-bottom: 1px solid var(--border); }\n");
        sb.append("        th { background: #f8fafc; font-weight: 600; color: #475569; }\n");
        sb.append("        tr.group-row { background: #f1f5f9; font-weight: 700; color: #1e293b; }\n");
        sb.append("        tr:hover:not(.group-row) { background: #f8fafc; }\n");
        sb.append("        .raw-report {\n");
        sb.append("            background: #0f172a;\n");
        sb.append("            color: #e2e8f0;\n");
        sb.append("            padding: 20px;\n");
        sb.append("            border-radius: 12px;\n");
        sb.append("            font-family: 'Consolas', 'Courier New', monospace;\n");
        sb.append("            font-size: 12px;\n");
        sb.append("            line-height: 1.6;\n");
        sb.append("            overflow-x: auto;\n");
        sb.append("            white-space: pre-wrap;\n");
        sb.append("            margin-bottom: 32px;\n");
        sb.append("        }\n");
        sb.append("        .footer {\n");
        sb.append("            text-align: center;\n");
        sb.append("            font-size: 12px;\n");
        sb.append("            color: var(--text-muted);\n");
        sb.append("            padding: 20px 0;\n");
        sb.append("            border-top: 1px solid var(--border);\n");
        sb.append("        }\n");
        sb.append("        @media print {\n");
        sb.append("            body { background: white; padding: 0; }\n");
        sb.append("            .btn-print { display: none; }\n");
        sb.append("            .header { box-shadow: none; border-radius: 0; }\n");
        sb.append("            .charts-grid { grid-template-columns: 1fr; }\n");
        sb.append("            .chart-card { break-inside: avoid; page-break-inside: avoid; }\n");
        sb.append("            .table-card { break-inside: avoid; page-break-inside: avoid; }\n");
        sb.append("        }\n");
        sb.append("    </style>\n</head>\n<body>\n");
        sb.append("    <div class=\"container\">\n");
        sb.append("        <div class=\"header\">\n");
        sb.append("            <div>\n");
        sb.append("                <h1>BÁO CÁO KHOA HỌC & HIỆU NĂNG THỰC NGHIỆM HUDD-TDS</h1>\n");
        sb.append("                <p>Mô hình: Baseline (Chưa áp dụng Design Patterns) | Bộ dữ liệu: ")
          .append(escapeHtml(datasetName)).append(" | Xuất lúc: ").append(exportTime).append("</p>\n");
        sb.append("            </div>\n");
        sb.append("            <button class=\"btn-print\" onclick=\"window.print()\">🖨 In báo cáo / Lưu PDF</button>\n");
        sb.append("        </div>\n");

        // KPI Cards
        sb.append("        <div class=\"kpi-grid\">\n");
        String[] titles;
        String[] borderColors;
        if (kpi != null && kpi.length >= 6) {
            titles = new String[]{"Thời gian thực thi", "Thông lượng (TPS)", "Bộ nhớ RAM Heap", "Đối tượng đã cấp phát", "Số lượng HUI duy nhất", "Tần suất Trôi dạt (Drift)"};
            borderColors = new String[]{"#a855f7", "#0284c7", "#f59e0b", "#0d9488", "#10b981", "#ef4444"};
        } else {
            titles = new String[]{"Thời gian thực thi", "Thông lượng (TPS)", "Bộ nhớ RAM Heap", "Số lượng HUI duy nhất", "Tần suất Trôi dạt (Drift)"};
            borderColors = new String[]{"#a855f7", "#0284c7", "#f59e0b", "#10b981", "#ef4444"};
        }
        for (int i = 0; i < titles.length; i++) {
            String val = (kpi != null && i < kpi.length && kpi[i] != null) ? kpi[i] : "-";
            sb.append("            <div class=\"kpi-card\" style=\"border-left: 4px solid ").append(borderColors[i]).append(";\">\n");
            sb.append("                <div class=\"title\">").append(titles[i]).append("</div>\n");
            sb.append("                <div class=\"value\">").append(escapeHtml(val)).append("</div>\n");
            sb.append("            </div>\n");
        }
        sb.append("        </div>\n");

        // Section I: Charts Grid
        sb.append("        <div class=\"section-title\">📊 I. HỆ THỐNG BIỂU ĐỒ TRỰC QUAN THỰC NGHIỆM</div>\n");
        sb.append("        <div class=\"charts-grid\">\n");
        addChartCard(sb, "1. Khoảng cách toàn cục (DISHS) qua từng Checkpoint", b64GlobalDis);
        addChartCard(sb, "2. Số lượng tập mục độ lợi cao HUI (Dạng đường)", b64HuiCount);
        addChartCard(sb, "3. Số lượng HUI (Dạng cột phân màu Trôi dạt)", b64BarHui);
        addChartCard(sb, "4. Tiêu thụ Bộ nhớ RAM Heap (MB)", b64Ram);
        addChartCard(sb, "5. Thông lượng xử lý giao dịch (tx/giây)", b64Throughput);
        if (b64Summary != null && !b64Summary.isEmpty()) {
            addChartCard(sb, "6. Tổng kết Phân bố Trôi dạt & Chỉ số Tài nguyên", b64Summary);
        }
        sb.append("        </div>\n");

        // Section II: Table
        sb.append("        <div class=\"section-title\">📋 II. BẢNG SỐ LIỆU THỰC NGHIỆM CHI TIẾT</div>\n");
        sb.append("        <div class=\"table-card\">\n");
        sb.append("            <table>\n");
        sb.append("                <thead>\n");
        sb.append("                    <tr><th style=\"width: 50%;\">Chỉ số / Tiêu chí đánh giá</th><th style=\"width: 50%;\">Giá trị thực nghiệm</th></tr>\n");
        sb.append("                </thead>\n");
        sb.append("                <tbody>\n");
        if (tableModel != null) {
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                Object col0 = tableModel.getValueAt(r, 0);
                Object col1 = tableModel.getValueAt(r, 1);
                String s0 = (col0 != null) ? col0.toString() : "";
                String s1 = (col1 != null) ? col1.toString() : "";
                if (s0.startsWith("▶")) {
                    sb.append("                    <tr class=\"group-row\"><td colspan=\"2\">").append(escapeHtml(s0)).append("</td></tr>\n");
                } else {
                    sb.append("                    <tr><td>").append(escapeHtml(s0)).append("</td><td><strong>").append(escapeHtml(s1)).append("</strong></td></tr>\n");
                }
            }
        }
        sb.append("                </tbody>\n");
        sb.append("            </table>\n");
        sb.append("        </div>\n");

        // Section III: Raw Summary Text
        if (summaryText != null && !summaryText.trim().isEmpty()) {
            sb.append("        <div class=\"section-title\">📝 III. BÁO CÁO TỔNG KẾT VĂN BẢN (SCIENTIFIC LOG)</div>\n");
            sb.append("        <div class=\"raw-report\">").append(escapeHtml(summaryText)).append("</div>\n");
        }

        // Footer
        sb.append("        <div class=\"footer\">Hệ thống Khai phá Luồng dữ liệu và Giám sát Trôi dạt Độ lợi HUDD-TDS | Lập Trình Java Nâng Cao</div>\n");
        sb.append("    </div>\n</body>\n</html>\n");

        return sb.toString();
    }

    private static void addChartCard(StringBuilder sb, String title, String b64Img) {
        sb.append("            <div class=\"chart-card\">\n");
        sb.append("                <div class=\"card-header\">").append(escapeHtml(title)).append("</div>\n");
        if (b64Img != null && !b64Img.isEmpty()) {
            sb.append("                <img src=\"").append(b64Img).append("\" alt=\"").append(escapeHtml(title)).append("\">\n");
        } else {
            sb.append("                <div style=\"padding: 40px; text-align: center; color: #94a3b8;\">Chưa có dữ liệu biểu đồ</div>\n");
        }
        sb.append("            </div>\n");
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
