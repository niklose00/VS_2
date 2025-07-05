package org.oxoo2a.sim4da.demo;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Simulator;
import org.oxoo2a.sim4da.dsm.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple distributed application that continuously updates counters in a
 * Distributed Shared Memory and reports observable inconsistencies.
 * <p>
 * The application works with all three DSM variants (CA, CP, AP). Each node
 * maintains a local counter value and writes it to the DSM. A dedicated monitor
 * node periodically reads all counters and prints anomalies such as decreasing
 * values or missing updates.
 */
public class DSMInconsistencyDemo {

    /** Number of counter nodes created for the demonstration. */
    private static final int NODE_COUNT = 5;

    /** Key used by all nodes when writing to the shared counter. */
    private static final String SHARED_KEY = "shared_counter";

    /** All counter keys (n0..nX plus shared counter). */
    private static final String[] ALL_KEYS = new String[NODE_COUNT + 1];

    static {
        for (int i = 0; i < NODE_COUNT; i++) {
            ALL_KEYS[i] = "n" + i;
        }
        ALL_KEYS[NODE_COUNT] = SHARED_KEY;
    }

    /** ANSI color codes for console output. */
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String RESET = "\u001B[0m";

    /**
     * Counter node using a DSM instance to share its counter value.
     */
    static class CounterAgent {
        private final int id;
        private final DistributedSharedMemory dsm;
        private final NetworkConnection nc;

        private final boolean writeOwn;
        private final boolean writeShared;

        private int local = 0;

        private final Map<String, Integer> lastSeen = new HashMap<>();
        private int rollbacks = 0;
        private int missing = 0;
        private int consistent = 0;

        CounterAgent(int id, DistributedSharedMemory dsm, NetworkConnection nc,
                     boolean writeOwn, boolean writeShared) {
            this.id = id;
            this.dsm = dsm;
            this.nc = nc;
            this.writeOwn = writeOwn;
            this.writeShared = writeShared;
            for (String key : ALL_KEYS) {
                lastSeen.put(key, -1);
            }
            this.nc.engage(this::run);
        }

        Metrics getMetrics() {
            return new Metrics(rollbacks, missing, consistent);
        }

        private void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (writeOwn) {
                    local++;
                    dsm.write(key(), Integer.toString(local));
                }

                if (writeShared) {
                    String curStr = dsm.read(SHARED_KEY);
                    int cur = curStr == null ? 0 : Integer.parseInt(curStr);
                    dsm.write(SHARED_KEY, Integer.toString(cur + 1));
                }

                for (String other : ALL_KEYS) {
                    String valStr = dsm.read(other);
                    int val = valStr == null ? -1 : Integer.parseInt(valStr);
                    int last = lastSeen.getOrDefault(other, -1);
                    if (last != -1 && val != -1) {
                        if (val < last) {
                            rollbacks++;
                            System.out.println(RED + "[" + id() + "] Rollback for " + other + ": " + last + " -> " + val + RESET);
                        } else if (val > last + 1) {
                            missing++;
                            System.out.println(YELLOW + "[" + id() + "] Missing update for " + other + ": jumped from " + last + " to " + val + RESET);
                        } else {
                            consistent++;
                            System.out.println(GREEN + "[" + id() + "] " + other + "=" + val + RESET);
                        }
                    }
                    lastSeen.put(other, val);
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private String key() {
            return "n" + id;
        }

        private String id() {
            return "n" + id;
        }
    }

    private record Metrics(int rollbacks, int missing, int consistent) {}


    /**
     * Creates the correct DSM instance for the given variant string.
     */
    private static DistributedSharedMemory createDSM(String variant, NetworkConnection nc) {
        return switch (variant) {
            case "AP" -> new AP_DSM(nc);
            case "CP" -> new CP_DSM(nc);
            default -> new CA_DSM();
        };
    }

    /**
     * Starts the demonstration. Optional argument selects the DSM variant (CA,
     * CP or AP). Default is CA.
     */
    public static void main(String[] args) {
        String variant = args.length > 0 ? args[0] : "CA";
        Simulator sim = Simulator.getInstance();

        CounterAgent[] agents = new CounterAgent[NODE_COUNT];
        for (int i = 0; i < NODE_COUNT; i++) {
            NetworkConnection nc = new NetworkConnection("n" + i);
            DistributedSharedMemory dsm = createDSM(variant, nc);
            boolean writeOwn = i >= 2;           // nodes 2..4 write their own counters
            boolean writeShared = i != 1;        // node 1 is read-only
            agents[i] = new CounterAgent(i, dsm, nc, writeOwn, writeShared);
        }

        sim.simulate(5);

        for (int i = 0; i < NODE_COUNT; i++) {
            Metrics m = agents[i].getMetrics();
            System.out.printf("Node n%d -> rollbacks:%d missing:%d consistent:%d%n",
                    i, m.rollbacks, m.missing, m.consistent);
        }

        sim.shutdown();
    }
}