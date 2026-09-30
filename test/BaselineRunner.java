package test;

import huddtds.algorithm.HUDD_TDS;
import huddtds.model.Checkpoint;
import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class BaselineRunner {
    public static void main(String[] args) throws Exception {
        Map<String, Double> externalUtilities = new HashMap<>();
        externalUtilities.put("a", 5.0);
        externalUtilities.put("b", 2.0);
        externalUtilities.put("c", 1.0);
        externalUtilities.put("d", 2.0);
        externalUtilities.put("e", 3.0);
        externalUtilities.put("g", 1.0);

        List<Transaction> stream = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("data/running_example.txt"))) {
            String line;
            int tid = 1;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Transaction tx = new Transaction(tid++);
                for (String token : line.trim().split("\\s+")) {
                    String[] parts = token.split(":", 2);
                    if (parts.length == 2) {
                        tx.addElement(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    }
                }
                stream.add(tx);
            }
        }

        System.out.println("Total transactions loaded: " + stream.size());

        // Default GUI parameters: minutil=15.0, interval=1, windowSize=2, alpha=0.10
        HUDD_TDS engine = new HUDD_TDS(externalUtilities, 15.0, 1, 2, 0.10);

        long start = System.currentTimeMillis();
        int cpCount = 0;
        for (Transaction tx : stream) {
            Checkpoint cp = engine.processTransaction(tx);
            if (cp != null) {
                cpCount++;
                System.out.println("\n--- Checkpoint TID: " + cp.getTid() + " ---");
                System.out.println("HUI count: " + cp.getHuis().size());
                System.out.println("Global Distance: " + cp.getGlobalDistance());
                for (HighUtilityItemset hui : cp.getHuis()) {
                    System.out.println("  Itemset: " + hui.getItems() + " | Utility: " + hui.getTotalUtility() + " | Dmo: " + hui.getDistanceToRoot());
                }
                String gDrift = engine.checkGlobalDrift();
                String lDrift = engine.checkLocalDrift();
                System.out.println("Global Drift: " + (gDrift != null ? gDrift : "None"));
                System.out.println("Local Drift: " + (lDrift != null ? lDrift : "None"));
            }
        }
        long duration = System.currentTimeMillis() - start;
        System.out.println("\nExecution time: " + duration + " ms");
    }
}
