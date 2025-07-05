package org.oxoo2a.sim4da.demo;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Simulator;
import org.oxoo2a.sim4da.dsm.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        private final BufferedWriter log;

        private int rollbackCount = 0;
        private int missingUpdateCount = 0;
        private int okCount = 0;

        CounterAgent(int id, DistributedSharedMemory dsm, NetworkConnection nc,
                     Role role, String[] allKeys, BufferedWriter log) {
            this.id = id;
            this.dsm = dsm;
            this.nc = nc;
            this.role = role;
            this.allKeys = allKeys;
            this.log = log;
            for (String k : allKeys) {
                lastSeen.put(k, -1);
            }
            this.nc.engage(this::run);
        }

        private void writeLog(String line) {
            if (log == null) return;
            synchronized (log) {
                try {
                    log.write(line);
                    log.newLine();
                    log.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void run() {
            int value = 0;
            int sharedValue = 0;
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
                    }
                }

                if (role != Role.WRITE_ONLY) {
                    // allowed readers check counters
                    for (String other : allKeys) {
                        String valStr = dsm.read(other);
                        if (valStr == null) continue;
                        int current = Integer.parseInt(valStr);
                        int last = lastSeen.getOrDefault(other, -1);
                        if (last >= 0) {
                            if (current < last) {
                                rollbackCount++;
                                String line = String.format("tick=%d node=%s observed=%s type=rollback from=%d to=%d",
                                        step, key(), other, last, current);
                                System.out.printf(RED + "[%s] Rollback for %s: %d -> %d" + RESET + "%n",
                                        key(), other, last, current);
                                writeLog(line);
                            } else if (current > last + 1) {
                                missingUpdateCount++;
                                String line = String.format("tick=%d node=%s observed=%s type=missing from=%d to=%d",
                                        step, key(), other, last, current);
                                System.out.printf(YELLOW + "[%s] Missing update for %s: %d -> %d" + RESET + "%n",
                                        key(), other, last, current);
                                writeLog(line);
                            } else {
                                okCount++;
                                String line = String.format("tick=%d node=%s observed=%s type=ok value=%d",
                                        step, key(), other, current);
                                System.out.printf(GREEN + "[%s] %s = %d" + RESET + "%n", key(), other, current);
                                writeLog(line);
                            }
                        }
                        lastSeen.put(other, current);
                    }
                }

                if (id == 1) {
                    StringBuilder row = new StringBuilder();
                    row.append("tick=").append(step).append(" state:");
                    for (String k : allKeys) {
                        String label = k.equals(SHARED_KEY) ? "shared" : k;
                        String val = dsm.read(k);
                        row.append(" ").append(label).append("=")
                                .append(val == null ? "null" : val);
                    }
                    String rowStr = row.toString();
                    System.out.println(rowStr.replaceFirst("tick=", "[Tick ")
                            .replaceFirst(" state:", "]"));
                    writeLog(rowStr);
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    break;
                }
            }

            String summary = String.format("[%s] Rollbacks: %d Missing: %d Consistent: %d",
                    key(), rollbackCount, missingUpdateCount, okCount);
            System.out.println(summary);
            writeLog(summary);
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

        try (BufferedWriter log = new BufferedWriter(new FileWriter("simlog.txt"))) {
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
                agents[i] = new CounterAgent(i, dsm, nc, role, keys, log);
            }

            sim.simulate(5);

            int totalRollbacks = java.util.Arrays.stream(agents).mapToInt(a -> a.rollbackCount).sum();
            int totalMissing = java.util.Arrays.stream(agents).mapToInt(a -> a.missingUpdateCount).sum();
            int totalOk = java.util.Arrays.stream(agents).mapToInt(a -> a.okCount).sum();
            String result = String.format("== Gesamtergebnisse ==%nRollbacks: %d, Fehlende Updates: %d, OK: %d",
                    totalRollbacks, totalMissing, totalOk);
            System.out.println(result);
            synchronized (log) {
                log.write(result);
                log.newLine();
                log.flush();
            }

            sim.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}