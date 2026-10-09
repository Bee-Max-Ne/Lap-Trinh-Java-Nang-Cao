package huddtds.application;

import huddtds.application.event.EventType;
import huddtds.application.event.SimulationEvent;
import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;
import huddtds.model.HighUtilityItemset;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.StringJoiner;

/**
 * Ghi đầy đủ kết quả checkpoint ra CSV để không phụ thuộc vào lịch sử giữ trong RAM.
 */
public final class CheckpointHistoryWriter implements Closeable {
    private static final String HEADER = "LoaiBanGhi,TID,GlobalDistance,HuiCount,"
            + "GlobalDrift,GlobalStatistic,GlobalThreshold,GlobalDirection,"
            + "LocalDrift,LocalStatistic,LocalThreshold,LocalItemsets,"
            + "Itemset,TotalUtility,DistanceToRoot,ItemUtilities";

    private final BufferedWriter writer;

    public CheckpointHistoryWriter(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        writer.write(HEADER);
        writer.newLine();
    }

    public void append(SimulationEvent event) throws IOException {
        if (event == null || event.getType() != EventType.CHECKPOINT_CREATED
                || event.getCheckpoint() == null) {
            throw new IllegalArgumentException("Sự kiện phải chứa checkpoint đã tạo");
        }

        Checkpoint checkpoint = event.getCheckpoint();
        if (checkpoint.getHuis().isEmpty()) {
            writeRow(event, checkpoint, null);
            return;
        }
        for (HighUtilityItemset hui : checkpoint.getHuis()) {
            writeRow(event, checkpoint, hui);
        }
    }

    private void writeRow(
            SimulationEvent event,
            Checkpoint checkpoint,
            HighUtilityItemset hui) throws IOException {
        DriftResult global = event.getGlobalDrift();
        DriftResult local = event.getLocalDrift();
        StringJoiner localItemsets = new StringJoiner(";");
        if (local != null) {
            for (String itemset : local.getAffectedItemsets()) {
                localItemsets.add(itemset);
            }
        }

        StringJoiner itemUtilities = new StringJoiner(";");
        String itemset = "";
        String totalUtility = "";
        String distanceToRoot = "";
        if (hui != null) {
            StringJoiner items = new StringJoiner(";");
            for (String item : hui.getItems()) {
                items.add(item);
                Double utility = hui.getItemUtilities().get(item);
                itemUtilities.add(item + "=" + (utility == null ? "" : utility));
            }
            itemset = items.toString();
            totalUtility = Double.toString(hui.getTotalUtility());
            distanceToRoot = Double.toString(hui.getDistanceToRoot());
        }

        String[] fields = {
                hui == null ? "CHECKPOINT" : "HUI",
                Integer.toString(checkpoint.getTid()),
                Double.toString(checkpoint.getGlobalDistance()),
                Integer.toString(checkpoint.getHuis().size()),
                Boolean.toString(global != null && global.isDetected()),
                global == null ? "" : Double.toString(global.getStatistic()),
                global == null ? "" : Double.toString(global.getThreshold()),
                global == null || global.getDirection() == null ? "" : global.getDirection(),
                Boolean.toString(local != null && local.isDetected()),
                local == null ? "" : Double.toString(local.getStatistic()),
                local == null ? "" : Double.toString(local.getThreshold()),
                localItemsets.toString(),
                itemset,
                totalUtility,
                distanceToRoot,
                itemUtilities.toString()
        };

        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(csv(fields[i]));
        }
        writer.newLine();
    }

    private static String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
