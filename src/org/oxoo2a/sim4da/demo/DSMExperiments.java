package org.oxoo2a.sim4da.demo;

import org.oxoo2a.sim4da.Network;
import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Simulator;
import org.oxoo2a.sim4da.dsm.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Runs systematic experiments for the three DSM variants (AP, CP, CA)
 * under different network conditions. Results of each run are printed
 * to stdout.
 */
public class DSMExperiments {

    private static final int NODE_COUNT = 5;
    private static final String SHARED_KEY = "shared_counter";

    /** Simple container for aggregated statistics of an agent. */
    private record Result(int rollbacks, int missing, int ok) {}

    /**
     * Roles used for the counter agents. Matches DSMInconsistencyDemo.
     */
    private enum Role { WRITE_ONLY, READ_ONLY, READ_WRITE }

    /**
     * Counter agent identical to the one used in DSMInconsistencyDemo
     * but packaged for reuse.
     */
    private static class CounterAgent {
        private final int id;
        private final DistributedSharedMemory dsm;
        private final NetworkConnection nc;
        private final Role role;
        private final String[] allKeys;
        private final Map<String,Integer> lastSeen = new HashMap<>();

        int rollbackCount = 0;
        int missingUpdateCount = 0;
        int okCount = 0;

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
            int sharedValue = 0;
            for (int step = 0; step < 40; step++) {
                if (role != Role.READ_ONLY) {
                    if (role != Role.WRITE_ONLY) {
                        value++;
                        dsm.write(key(), Integer.toString(value));
                    }

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
                    for (String other : allKeys) {
                        String valStr = dsm.read(other);
                        if (valStr == null) continue;
                        int current = Integer.parseInt(valStr);
                        int last = lastSeen.getOrDefault(other, -1);
                        if (last >= 0) {
                            if (current < last) {
                                rollbackCount++;
                            } else if (current > last + 1) {
                                missingUpdateCount++;
                            } else {
                                okCount++;
                            }
                        }
                        lastSeen.put(other, current);
                    }
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }

        private String key() { return "n" + id; }
    }

    private static DistributedSharedMemory createDSM(String variant, NetworkConnection nc) {
        return switch (variant) {
            case "AP" -> new AP_DSM(nc);
            case "CP" -> new CP_DSM(nc);
            default -> new CA_DSM();
        };
    }

    private static Result runOnce(String variant, boolean withDelay, boolean withPartition) {
        Network net = Network.getInstance();
        net.reset();
        if (withDelay) {
            net.setNodeDelay("n2", 300);
            net.setNodeDelay("n3", 300);
        }
        if (withPartition) {
            net.partitionNode("n2");
            net.partitionNode("n3");
        }

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
            Role role;
            if (i == 0) role = Role.WRITE_ONLY;
            else if (i == 1) role = Role.READ_ONLY;
            else role = Role.READ_WRITE;
            agents[i] = new CounterAgent(i, dsm, nc, role, keys);
        }

        sim.simulate(5);

        int totalRollbacks = java.util.Arrays.stream(agents).mapToInt(a -> a.rollbackCount).sum();
        int totalMissing = java.util.Arrays.stream(agents).mapToInt(a -> a.missingUpdateCount).sum();
        int totalOk = java.util.Arrays.stream(agents).mapToInt(a -> a.okCount).sum();

        sim.shutdown();
        Simulator.reset();
        net.reset();

        return new Result(totalRollbacks, totalMissing, totalOk);
    }

    private static void runVariant(String variant) {
        Result normal = runOnce(variant, false, false);
        System.out.printf("| %s | normal | %d / %d / %d |%n", variant, normal.rollbacks, normal.missing, normal.ok);

        Result delay = runOnce(variant, true, false);
        System.out.printf("| %s | delay | %d / %d / %d |%n", variant, delay.rollbacks, delay.missing, delay.ok);

        Result partition = runOnce(variant, false, true);
        System.out.printf("| %s | partition n2+n3 | %d / %d / %d |%n", variant, partition.rollbacks, partition.missing, partition.ok);
    }

    public static void runAP() { runVariant("AP"); }
    public static void runCP() { runVariant("CP"); }
    public static void runCA() { runVariant("CA"); }

    public static void main(String[] args) {
        runAP();
        runCP();
        runCA();
    }
}

