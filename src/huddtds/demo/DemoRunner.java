package huddtds.demo;

import huddtds.algorithm.HUDD_TDS;
import huddtds.model.Checkpoint;
import huddtds.model.HighUtilityItemset;
import huddtds.model.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DemoRunner {

    public static void main(String[] args) {

        Map<String, Double> utilities = new HashMap<>();
        utilities.put("a", 5.0);
        utilities.put("b", 2.0);
        utilities.put("c", 1.0);
        utilities.put("d", 2.0);
        utilities.put("e", 3.0);
        utilities.put("g", 1.0);

        List<Transaction> stream = new ArrayList<>();

        /*
         * Giai doan 1:
         * [a,c,e] la HUI chinh.
         */
        addTransaction(stream, 1,
                new String[]{"a", "c", "e"},
                new int[]{2, 6, 2});

        addTransaction(stream, 2,
                new String[]{"a", "c", "e"},
                new int[]{2, 6, 2});

        addTransaction(stream, 3,
                new String[]{"a", "c", "e"},
                new int[]{2, 6, 2});

        addTransaction(stream, 4,
                new String[]{"a", "c", "e"},
                new int[]{2, 6, 2});

        addTransaction(stream, 5,
                new String[]{"a", "c", "e"},
                new int[]{2, 6, 2});

        addTransaction(stream, 6,
                new String[]{"a", "c", "e"},
                new int[]{2, 6, 2});

        /*
         * Giai doan 2:
         * [b,c,e] thay the [a,c,e].
         */
        addTransaction(stream, 7,
                new String[]{"b", "c", "e"},
                new int[]{4, 3, 4});

        addTransaction(stream, 8,
                new String[]{"b", "c", "e"},
                new int[]{4, 3, 4});

        addTransaction(stream, 9,
                new String[]{"b", "c", "e"},
                new int[]{4, 3, 4});

        addTransaction(stream, 10,
                new String[]{"b", "c", "e"},
                new int[]{4, 3, 4});

        /*
         * interval = 2
         * windowSize = 2
         *
         * => checkpoint T2, T4, T6, T8, T10
         */
        HUDD_TDS engine = new HUDD_TDS(
                utilities,
                22.0,
                2,
                2,
                0.05
        );

        for (Transaction tx : stream) {

            Checkpoint cp = engine.processTransaction(tx);

            if (cp == null) {
                continue;
            }

            System.out.println();
            System.out.println(
                    "========== CHECKPOINT T" + cp.getTid() + " =========="
            );

            System.out.println(
                    "globalDistance = " + cp.getGlobalDistance()
                            + " | HUI = " + cp.getHuis().size()
            );

            for (HighUtilityItemset hui : cp.getHuis()) {

                System.out.println(
                        "  - " + hui.getItems()
                                + " => utility=" + hui.getTotalUtility()
                                + ", Dmo=" + hui.getDistanceToRoot()
                );
            }

            String globalDrift = engine.checkGlobalDrift();
            String localDrift = engine.checkLocalDrift();

            if (globalDrift != null) {
                System.out.println("  GLOBAL: " + globalDrift);
            }

            if (localDrift != null) {
                System.out.println("  LOCAL: " + localDrift);
            }
        }
    }

    private static void addTransaction(
            List<Transaction> stream,
            int tid,
            String[] items,
            int[] quantities) {

        Transaction transaction = new Transaction(tid);

        for (int i = 0; i < items.length; i++) {
            transaction.addElement(items[i], quantities[i]);
        }

        stream.add(transaction);
    }
}