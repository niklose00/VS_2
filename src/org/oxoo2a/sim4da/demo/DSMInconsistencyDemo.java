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
    private static final int NODE_COUNT = 3;

    /**
     * Counter node using a DSM instance to share its counter value.
     */
    static class CounterAgent {
        private final int id;
        private final DistributedSharedMemory dsm;
        private final NetworkConnection nc;

        CounterAgent(int id, DistributedSharedMemory dsm, NetworkConnection nc) {
            this.id = id;
            this.dsm = dsm;
            this.nc = nc;
            this.nc.engage(this::run);
        }

        private void run() {
            int value = 0;
            for (int i = 0; i < 20; i++) {
                value++;
                dsm.write(key(), Integer.toString(value));
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
            }
        }

        private String key() {
            return "n" + id;
        }
    }

    /**
     * Monitor node that reads all counters and prints anomalies.
     */
    static class MonitorAgent {
        private final DistributedSharedMemory dsm;
        private final NetworkConnection nc;
        private final Map<String, Integer> lastSeen = new HashMap<>();

        MonitorAgent(DistributedSharedMemory dsm, NetworkConnection nc) {
            this.dsm = dsm;
            this.nc = nc;
            this.nc.engage(this::run);
        }

        private void run() {
            for (int i = 0; i < 40; i++) {
                for (int id = 0; id < NODE_COUNT; id++) {
                    String key = "n" + id;
                    String val = dsm.read(key);
                    if (val == null) continue;
                    int current = Integer.parseInt(val);
                    Integer last = lastSeen.get(key);
                    if (last != null && current < last) {
                        System.out.printf("[Monitor] Rollback for %s: %d -> %d%n", key, last, current);
                    } else if (last != null && current > last + 1) {
                        System.out.printf("[Monitor] Missing updates for %s: jumped from %d to %d%n", key, last, current);
                    }
                    lastSeen.put(key, current);
                }
                System.out.println("[Monitor] Current view: " + lastSeen);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
        }
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
        for (int i = 0; i < NODE_COUNT; i++) {
            NetworkConnection nc = new NetworkConnection("n" + i);
            DistributedSharedMemory dsm = createDSM(variant, nc);
            agents[i] = new CounterAgent(i, dsm, nc);
        }

        NetworkConnection monitorNc = new NetworkConnection("monitor");
        DistributedSharedMemory monitorDsm = createDSM(variant, monitorNc);
        new MonitorAgent(monitorDsm, monitorNc);

        sim.simulate(5);
        sim.shutdown();
    }
}