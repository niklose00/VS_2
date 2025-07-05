package org.oxoo2a.sim4da.demo;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Simulator;
import org.oxoo2a.sim4da.dsm.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

/**
 * Simple distributed application that continuously updates counters in a
 * Distributed Shared Memory and reports observable inconsistencies.
 * <p>
 * The application works with all three DSM variants (CA, CP, AP). Each node
 * maintains a local counter value and writes it to the DSM. All nodes
 * periodically read every counter and report anomalies such as decreasing
 * values or missing updates.
 */
public class DSMInconsistencyDemo {

    /** Number of counter nodes created for the demonstration. */
    private static final int NODE_COUNT = 5;

    /** Key used for the globally shared counter */
    private static final String SHARED_KEY = "shared_counter";

    /**
     * Counter node using a DSM instance to share its counter value.
     */
    static class CounterAgent {
        enum Role { WRITE_ONLY, READ_ONLY, READ_WRITE }

        private static final String RED = "\u001B[31m";
        private static final String YELLOW = "\u001B[33m";
        private static final String GREEN = "\u001B[32m";
        private static final String RESET = "\u001B[0m";

        private final int id;
        private final DistributedSharedMemory dsm;
        private final NetworkConnection nc;
        private final Role role;
        private final String[] allKeys;
        private final Map<String, Integer> lastSeen = new HashMap<>();

        // metrics accessible from outside for aggregation
        int rollbackCount = 0;
        int missingUpdateCount = 0;
        int okCount = 0;

        private int sharedValue = 0;

        CounterAgent(int id, DistributedSharedMemory dsm, NetworkConnection nc,
                     Role role, String[] allKeys) {
            this.id = id;
            this.dsm = dsm;
            this.nc = nc;
            this.role = role;
            this.allKeys = allKeys;
            for (String k : allKeys) {
                lastSeen.put(k, -1);
            }
            this.nc.engage(this::run);
        }

        private void run() {
            int value = 0;
            for (int step = 0; step < 40; step++) {
                if (role != Role.READ_ONLY) {
                    if (role != Role.WRITE_ONLY) {
                        value++;
                        dsm.write(key(), Integer.toString(value));
                    }
                    // writers update shared counter
                    if (role == Role.WRITE_ONLY) {
                        sharedValue++;
                        dsm.write(SHARED_KEY, Integer.toString(sharedValue));
                    } else {
                        String s = dsm.read(SHARED_KEY);
                        int sv = s == null ? 0 : Integer.parseInt(s);
                        dsm.write(SHARED_KEY, Integer.toString(sv + 1));
                        sharedValue = sv + 1;
                    }
                }

                if (role != Role.WRITE_ONLY) {
                    // readers check counters
                    for (String other : allKeys) {
                        String valStr = dsm.read(other);
                        if (valStr == null) continue;
                        int current = Integer.parseInt(valStr);
                        int last = lastSeen.getOrDefault(other, -1);
                        if (last >= 0) {
                            if (current < last) {
                                rollbackCount++;
                                System.out.printf(RED + "[%s] Rollback for %s: %d -> %d" + RESET + "%n",
                                        key(), other, last, current);
                            } else if (current > last + 1) {
                                missingUpdateCount++;
                                System.out.printf(YELLOW + "[%s] Missing update for %s: %d -> %d" + RESET + "%n",
                                        key(), other, last, current);
                            } else {
                                okCount++;
                                System.out.printf(GREEN + "[%s] %s = %d" + RESET + "%n", key(), other, current);
                            }
                        }
                        lastSeen.put(other, current);
                    }

                    // tabular output of all counters
                    StringBuilder sb = new StringBuilder();
                    sb.append("[Tick ").append(step).append("] ");
                    for (String k : allKeys) {
                        String val = dsm.read(k);
                        sb.append(k).append("=").append(val == null ? "-" : val).append(" ");
                    }
                    System.out.println(sb.toString().trim());
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }

            System.out.printf("[%s] Rollbacks: %d Missing: %d Consistent: %d%n",
                    key(), rollbackCount, missingUpdateCount, okCount);
        }

        private String key() { return "n" + id; }
    }


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
        String[] keys = new String[NODE_COUNT + 1];
        for (int i = 0; i < NODE_COUNT; i++) {
            keys[i] = "n" + i;
        }
        keys[NODE_COUNT] = SHARED_KEY;

        for (int i = 0; i < NODE_COUNT; i++) {
            NetworkConnection nc = new NetworkConnection("n" + i);
            DistributedSharedMemory dsm = createDSM(variant, nc);
            CounterAgent.Role role;
            if (i == 0) role = CounterAgent.Role.WRITE_ONLY;
            else if (i == 1) role = CounterAgent.Role.READ_ONLY;
            else role = CounterAgent.Role.READ_WRITE;
            agents[i] = new CounterAgent(i, dsm, nc, role, keys);
        }

        sim.simulate(5);
        sim.shutdown();

        int totalRollbacks = java.util.Arrays.stream(agents).mapToInt(a -> a.rollbackCount).sum();
        int totalMissing = java.util.Arrays.stream(agents).mapToInt(a -> a.missingUpdateCount).sum();
        int totalOk = java.util.Arrays.stream(agents).mapToInt(a -> a.okCount).sum();

        System.out.printf("== Gesamtergebnisse ==%nRollbacks: %d, Fehlende Updates: %d, OK: %d%n",
                totalRollbacks, totalMissing, totalOk);
    }
}