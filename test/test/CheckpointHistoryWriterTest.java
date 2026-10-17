package test;

import huddtds.application.CheckpointHistoryWriter;
import huddtds.application.event.EventType;
import huddtds.application.event.SimulationEvent;
import huddtds.model.Checkpoint;
import huddtds.model.DriftResult;
import huddtds.model.HighUtilityItemset;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public class CheckpointHistoryWriterTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("huddtds-history-test");
        Path output = directory.resolve("checkpoint-history.csv");
        try {
            Checkpoint checkpoint = new Checkpoint(42);
            HighUtilityItemset hui = new HighUtilityItemset(
                    new LinkedHashSet<>(Arrays.asList("item,one", "item\"two")));
            hui.getItemUtilities().put("item,one", 2.5);
            hui.getItemUtilities().put("item\"two", 3.5);
            hui.setTotalUtility(6.0);
            hui.setDistanceToRoot(1.25);
            checkpoint.getHuis().add(hui);
            checkpoint.setGlobalDistance(1.25);

            SimulationEvent event = new SimulationEvent.Builder()
                    .withType(EventType.CHECKPOINT_CREATED)
                    .withTid(42)
                    .withCheckpoint(checkpoint)
                    .withGlobalDrift(DriftResult.globalDrift(41, 42, 0.5, 0.25, "TĂNG"))
                    .withLocalDrift(DriftResult.localDrift(41, 42, 1.0, 0.5, "[item,one]"))
                    .build();

            try (CheckpointHistoryWriter writer = new CheckpointHistoryWriter(output)) {
                writer.append(event);

                Checkpoint emptyCheckpoint = new Checkpoint(43);
                writer.append(new SimulationEvent.Builder()
                        .withType(EventType.CHECKPOINT_CREATED)
                        .withTid(43)
                        .withCheckpoint(emptyCheckpoint)
                        .withGlobalDrift(DriftResult.noDrift(42, 43))
                        .withLocalDrift(DriftResult.noDrift(42, 43))
                        .build());
            }

            List<String> rows = Files.readAllLines(output);
            require(rows.size() == 3, "Expected one header and two checkpoint rows");
            require(rows.get(1).contains("\"item,one;item\"\"two\""),
                    "Item names should be CSV escaped");
            require(rows.get(1).contains("\"6.0\"")
                            && rows.get(1).contains("\"1.25\"")
                            && rows.get(1).contains("\"true\""),
                    "HUI utility and drift details should be persisted");
            require(rows.get(2).startsWith("\"CHECKPOINT\",\"43\""),
                    "A checkpoint without HUI should still be persisted");

            System.out.println("CheckpointHistoryWriterTest PASSED");
        } finally {
            Files.deleteIfExists(output);
            Files.deleteIfExists(directory);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
