package org.oxoo2a.sim4da.demo;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Simulator;
import org.oxoo2a.sim4da.dsm.DistributedSharedMemory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Simple agent that increments a counter in the DSM and
 * checks counters of all peers for anomalies.
 */
public class CounterAgent {
    private final NetworkConnection nc;
    private final DistributedSharedMemory dsm;
    private final String[] peers;
    private final Map<String,Integer> last = new HashMap<>();
    private final String myKey;

    public CounterAgent(String name,
                        Function<NetworkConnection, DistributedSharedMemory> factory,
                        String[] peerNames) {
        this.nc = new NetworkConnection(name);
        this.dsm = factory.apply(nc);
        this.peers = peerNames;
        this.myKey = "counter-" + name;
        nc.engage(this::run);
    }

    private void run() {
        int value = 0;
        Simulator sim = Simulator.getInstance();
        while (sim.isSimulating()) {
            value++;
            dsm.write(myKey, String.valueOf(value));
            for (String p : peers) {
                String key = "counter-" + p;
                String vStr = dsm.read(key);
                if (vStr != null) {
                    int v = Integer.parseInt(vStr);
                    Integer prev = last.get(p);
                    if (prev != null && v < prev) {
                        System.out.printf("[%s] Anomaly: %s decreased from %d to %d%n",
                                nc.NodeName(), p, prev, v);
                    }
                    last.put(p, v);
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
