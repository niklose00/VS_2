package org.oxoo2a.sim4da.demo;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.dsm.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple agent that increments a counter in the DSM and periodically
 * reads counters of all known agents to detect inconsistencies.
 */
public class CounterAgent {
    private final String id;
    private final String[] allIds;
    private final NetworkConnection nc;
    private final DistributedSharedMemory dsm;
    private final Map<String,Integer> lastSeen = new HashMap<>();

    public enum DSMVariant { CA, AP, CP }

    public CounterAgent(String id, String[] allIds, DSMVariant variant) {
        this.id = id;
        this.allIds = allIds;
        this.nc = new NetworkConnection(id);
        this.dsm = createDSM(variant);
        for (String other : allIds) {
            lastSeen.put(other, -1);
        }
        nc.engage(this::run);
    }

    private DistributedSharedMemory createDSM(DSMVariant v) {
        return switch (v) {
            case CA -> new CA_DSM();
            case AP -> new AP_DSM(nc);
            case CP -> new CP_DSM(nc);
        };
    }

    private void run() {
        while (!Thread.currentThread().isInterrupted()) {
            // increment own counter
            String curStr = dsm.read(id);
            int cur = curStr == null ? 0 : Integer.parseInt(curStr);
            dsm.write(id, Integer.toString(cur + 1));

            // read all counters and detect regressions
            for (String other : allIds) {
                String valStr = dsm.read(other);
                int val = valStr == null ? -1 : Integer.parseInt(valStr);
                int prev = lastSeen.getOrDefault(other, -1);
                if (val < prev) {
                    System.out.printf("[%s] Inconsistency for %s: %d -> %d%n", id, other, prev, val);
                }
                lastSeen.put(other, val);
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
