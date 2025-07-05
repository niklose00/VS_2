package org.oxoo2a.app;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.dsm.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Simulation node maintaining a local counter and interacting with a
 * distributed shared memory implementation.
 */
public class CounterAgent {
    private final NetworkConnection nc;
    private final DistributedSharedMemory dsm;
    private final List<String> allNodes;
    private final int ticks;
    private final Random rnd = new Random();
    private final Map<String,Integer> lastSeen = new HashMap<>();
    private int counter = 0;

    public CounterAgent(String name, String dsmVariant, List<String> allNodes, int ticks) {
        this.nc = new NetworkConnection(name);
        this.dsm = createDSM(dsmVariant, nc);
        this.allNodes = allNodes;
        this.ticks = ticks;
    }

    public void engage() {
        nc.engage(this::run);
    }

    private void run() {
        for (int t=0; t<ticks; t++) {
            counter++;
            dsm.write(nc.NodeName(), Integer.toString(counter));

            for (String other : allNodes) {
                String val = dsm.read(other);
                Integer prev = lastSeen.get(other);
                String type = "ok";
                if (prev != null && val != null) {
                    int v = Integer.parseInt(val);
                    if (v < prev) type = "backjump";
                    else if (v == prev) type = "missing_update";
                }
                CSVLogger.log(t, nc.NodeName(), other, val, type, prev);
                if (val != null) lastSeen.put(other, Integer.parseInt(val));
            }
            try {
                Thread.sleep(rnd.nextInt(5)+1); // small random delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void join() { nc.join(); }

    private static DistributedSharedMemory createDSM(String variant, NetworkConnection nc) {
        return switch (variant.toUpperCase()) {
            case "AP" -> new AP_DSM(nc);
            case "CP" -> new CP_DSM(nc);
            default -> new CA_DSM();
        };
    }
}
