package org.oxoo2a.sim4da.examples;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.dsm.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Node maintaining a local counter in the DSM and
 * checking for inconsistencies among all known nodes.
 */
public class CounterNode {
    private final NetworkConnection nc;
    private final DistributedSharedMemory dsm;
    private final String[] keys;
    private final Map<String, Integer> lastSeen = new HashMap<>();
    private final int iterations;

    public CounterNode(String name, String variant, String[] keys, int iterations) {
        this.nc = new NetworkConnection(name);
        this.dsm = switch (variant) {
            case "AP" -> new AP_DSM(nc);
            case "CP" -> new CP_DSM(nc);
            default -> new CA_DSM();
        };
        this.keys = keys;
        this.iterations = iterations;
        nc.engage(this::run);
    }

    private void run() {
        int localCounter = 0;
        for (int i = 0; i < iterations; i++) {
            localCounter++;
            dsm.write(nc.NodeName(), Integer.toString(localCounter));
            checkAll();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void checkAll() {
        for (String key : keys) {
            String valStr = dsm.read(key);
            if (valStr == null) {
                continue;
            }
            int value = Integer.parseInt(valStr);
            Integer prev = lastSeen.get(key);
            if (prev != null && value < prev) {
                System.out.printf("\u001B[31m%s saw counter of %s decrease from %d to %d\u001B[0m%n",
                        nc.NodeName(), key, prev, value);
            }
            lastSeen.put(key, value);
        }
    }
}
