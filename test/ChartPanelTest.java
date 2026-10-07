package huddtds.demo;

import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class ChartPanelTest {
    public static void main(String[] args) {
        List<Checkpoint> input = Arrays.asList(
                new Checkpoint(440000),
                new Checkpoint(180000),
                new Checkpoint(280000));
        List<Checkpoint> sorted = ChartPanel.sortCheckpointsByTid(input);

        require(sorted.get(0).getTid() == 180000
                        && sorted.get(1).getTid() == 280000
                        && sorted.get(2).getTid() == 440000,
                "Checkpoint TIDs should be sorted numerically");
        require(ChartPanel.xForTid(180000, sorted, 60, 600)
                        < ChartPanel.xForTid(280000, sorted, 60, 600)
                        && ChartPanel.xForTid(280000, sorted, 60, 600)
                        < ChartPanel.xForTid(440000, sorted, 60, 600),
                "Chart x-coordinates should increase with numeric TID");

        double ordinaryValue = ChartPanel.transformMetric(500.0);
        double peakValue = ChartPanel.transformMetric(21000.0);
        require(ordinaryValue > 0.0 && peakValue > ordinaryValue,
                "Logarithmic display transform should preserve metric ordering");
        require(peakValue - ordinaryValue < 21000.0 - 500.0,
                "Logarithmic display transform should compress large peaks");
        require(Math.abs(ChartPanel.inverseTransformMetric(ordinaryValue) - 500.0) < 1e-9,
                "Axis labels should map back to original metric values");

        DriftResult globalDrift = DriftResult.globalDrift(180000, 280000, 12.0, 3.0, "TĂNG");
        DriftResult localDrift = DriftResult.localDrift(180000, 280000, 12.0, 3.0, "[a]");
        require(ChartPanel.isGlobalDrift(globalDrift),
                "Only a detected global drift should mark a chart point");
        require(!ChartPanel.isGlobalDrift(localDrift)
                        && !ChartPanel.isGlobalDrift(DriftResult.noDrift(180000, 280000))
                        && !ChartPanel.isGlobalDrift(null),
                "Local, absent, or null drift results should not mark a global drift point");

        ChartPanel panel = new ChartPanel();
        panel.updateChart(input);
        panel.addCheckpoint(new Checkpoint(380000), DriftResult.noDrift(280000, 380000));
        panel.addCheckpoint(new Checkpoint(480000),
                DriftResult.globalDrift(380000, 480000, 12.0, 3.0, "TĂNG"));
        panel.setSize(640, 360);
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();

        System.out.println("ChartPanelTest PASSED");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
