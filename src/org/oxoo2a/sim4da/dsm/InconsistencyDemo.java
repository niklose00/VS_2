package org.oxoo2a.sim4da.dsm;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Simulator;

import java.util.*;
import java.util.function.Function;

/**
 * Small demo application that runs a set of counter nodes on top of a
 * {@link DistributedSharedMemory} implementation. Each node increments its own
 * counter and continuously reads the counters of all other nodes. Whenever a
 * counter value appears to go backwards, an anomaly message is printed. This
 * allows differences in the consistency guarantees of the DSM variants to be
 * observed in practice.
 *
 * <p>Usage: {@code java org.oxoo2a.sim4da.dsm.InconsistencyDemo [CA|CP|AP]}</p>
 */
public class InconsistencyDemo {

    /** Creates the appropriate DSM based on the desired variant. */
    private static DistributedSharedMemory createDSM(String variant, NetworkConnection nc) {
        return switch (variant.toUpperCase()) {
            case "CA" -> new CA_DSM();
            case "CP" -> new CP_DSM(nc);
            default -> new AP_DSM(nc);
        };
    }

    /** Simple agent maintaining a counter in the DSM. */
    private static class CounterAgent {
        private final NetworkConnection nc;
        private final DistributedSharedMemory dsm;
        private final List<String> keys;
        private int local = 0;
        private final Map<String,Integer> lastSeen = new HashMap<>();

        CounterAgent(String name, String variant, List<String> keys) {
            this.nc = new NetworkConnection(name);
            this.dsm = createDSM(variant, nc);
            this.keys = keys;
            nc.engage(this::run);
        }

        private void run() {
            while (!Thread.currentThread().isInterrupted()) {
                local++;
                dsm.write(nc.NodeName(), String.valueOf(local));
                for (String k : keys) {
                    String val = dsm.read(k);
                    if (val != null) {
                        int v = Integer.parseInt(val);
                        Integer last = lastSeen.get(k);
                        if (last != null && v < last) {
                            System.out.printf("[%s] ANOMALY for %s: %d -> %d%n",
                                    nc.NodeName(), k, last, v);
                        }
                        lastSeen.put(k, v);
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Runs the demo with three nodes for a few seconds.
     */
    public static void main(String[] args) {
        String variant = args.length > 0 ? args[0] : "AP";
        int nodeCount = 3;
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            names.add("N" + i);
        }
        names = Collections.unmodifiableList(names);
        for (String n : names) {
            new CounterAgent(n, variant, names);
        }
        Simulator sim = Simulator.getInstance();
        sim.simulate(2);
        sim.shutdown();
    }
}
