package org.oxoo2a.sim4da.demo;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Simulator;
import org.oxoo2a.sim4da.dsm.DistributedSharedMemory;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node that maintains a counter in the DSM and periodically
 * performs reads/writes to demonstrate DSM behavior.
 */
public class CounterNode {
    private final String name;
    private final NetworkConnection nc;
    private final DistributedSharedMemory dsm;
    private final List<String> allNodes;
    private final Map<String, Integer> lastSeen = new HashMap<>();
    private final Map<String, Integer> stagnation = new HashMap<>();
    private final PrintWriter log;
    private int localCounter = 0;
    private int tick = 0;
    private static final int STAGNATION_THRESHOLD = 3;

    public CounterNode(String name,
                       DistributedSharedMemory dsm,
                       List<String> allNodes,
                       PrintWriter log,
                       NetworkConnection nc) {
        this.name = name;
        this.nc = nc;
        this.dsm = dsm;
        this.allNodes = allNodes;
        this.log = log;
    }

    public void engage() {
        nc.engage(this::run);
    }

    private void run() {
        while (Simulator.getInstance().isSimulating()) {
            tick++;
            localCounter++;
            String key = key(name);
            dsm.write(key, String.valueOf(localCounter));

            // read all counters
            for (String node : allNodes) {
                String k = key(node);
                String vStr = dsm.read(k);
                int v = vStr == null ? 0 : Integer.parseInt(vStr);

                Integer prev = lastSeen.get(k);
                if (prev != null) {
                    if (v < prev) {
                        log("Ruecksprung", k, v + " < " + prev);
                    } else if (v == prev) {
                        stagnation.put(k, stagnation.getOrDefault(k, 0) + 1);
                        if (stagnation.get(k) >= STAGNATION_THRESHOLD) {
                            log("Stagnation", k, "value=" + v);
                            stagnation.put(k, 0);
                        }
                    } else {
                        stagnation.put(k, 0);
                    }
                }
                lastSeen.put(k, v);
            }

            // divergence: read own key after write
            String selfRead = dsm.read(key);
            if (selfRead != null && Integer.parseInt(selfRead) != localCounter) {
                log("Divergenz", key, "expected=" + localCounter + " got=" + selfRead);
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void log(String type, String key, String details) {
        synchronized (log) {
            log.printf("tick=%d node=%s type=%s key=%s %s%n", tick, name, type, key, details);
            log.flush();
        }
    }

    private static String key(String name) {
        return "counter-" + name;
    }
}
