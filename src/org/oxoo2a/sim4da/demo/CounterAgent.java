package org.oxoo2a.sim4da.demo;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.dsm.DistributedSharedMemory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent representing one node in the DSM consistency demo.
 * Each agent periodically increments its own counter and reads
 * all counters via the provided DSM implementation.
 */
public class CounterAgent {
    private final String id;
    private final NetworkConnection nc;
    private final DistributedSharedMemory dsm;
    private final List<String> allIds;
    private final Map<String, Integer> lastSeen = new HashMap<>();
    private final Map<String, Integer> stagnation = new HashMap<>();
    private final PrintWriter log;

    private static final int WRITE_INTERVAL_MS = 500;
    private static final int STAGNATION_LIMIT = 5; // ticks

    private int localCounter = 0;
    private int tick = 0;

    public CounterAgent(String id,
                        java.util.function.Function<NetworkConnection, DistributedSharedMemory> dsmFactory,
                        List<String> allIds) throws IOException {
        this.id = id;
        this.nc = new NetworkConnection(id);
        this.dsm = dsmFactory.apply(nc);
        this.allIds = allIds;
        this.log = new PrintWriter(new FileWriter("consistency-" + id + ".txt"), true);
        nc.engage(this::run);
    }

    private void log(String type, String key, String details) {
        String msg = String.format("tick=%d node=%s type=%s key=%s %s", tick, id, type, key, details);
        log.println(msg);
        System.out.println(msg);
    }

    private void run() {
        while (!Thread.currentThread().isInterrupted()) {
            tick++;
            localCounter++;
            dsm.write(key(), Integer.toString(localCounter));

            // read back own value to detect divergence
            String ownVal = dsm.read(key());
            if (ownVal != null) {
                int readBack = Integer.parseInt(ownVal);
                if (readBack != localCounter) {
                    log("Divergenz", key(), "expected=" + localCounter + " got=" + readBack);
                }
            }

            // read all counters
            for (String otherId : allIds) {
                String k = "counter-" + otherId;
                String vStr = dsm.read(k);
                if (vStr == null) continue;
                int v = Integer.parseInt(vStr);
                Integer prev = lastSeen.get(k);
                if (prev != null) {
                    if (v < prev) {
                        log("Ruecksprung", k, "prev=" + prev + " curr=" + v);
                    }
                    if (v == prev) {
                        int c = stagnation.getOrDefault(k, 0) + 1;
                        stagnation.put(k, c);
                        if (c > STAGNATION_LIMIT) {
                            log("Stagnation", k, "value=" + v);
                        }
                    } else {
                        stagnation.put(k, 0);
                    }
                }
                lastSeen.put(k, v);
            }

            try {
                Thread.sleep(WRITE_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.close();
    }

    private String key() {
        return "counter-" + id;
    }
}
