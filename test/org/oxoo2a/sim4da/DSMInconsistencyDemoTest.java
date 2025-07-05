package org.oxoo2a.sim4da;

import org.junit.jupiter.api.Test;
import org.oxoo2a.sim4da.demo.CounterAgent;
import org.oxoo2a.sim4da.dsm.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Demonstrates a simple counter application using different DSM implementations.
 * Each agent updates its own counter and checks all others for inconsistencies.
 * Anomalies are printed to stdout.
 */
public class DSMInconsistencyDemoTest {

    private void runDemo(Function<NetworkConnection, DistributedSharedMemory> factory) {
        final int nodes = 3;
        Simulator sim = Simulator.getInstance();
        String[] names = new String[nodes];
        for (int i = 0; i < nodes; i++) {
            names[i] = String.valueOf(i);
        }
        List<CounterAgent> agents = new ArrayList<>();
        for (String n : names) {
            agents.add(new CounterAgent(n, factory, names));
        }
        sim.simulate(2);
        sim.shutdown();
    }

    @Test
    void demoAP() { runDemo(AP_DSM::new); }

    @Test
    void demoCP() { runDemo(CP_DSM::new); }

    @Test
    void demoCA() { runDemo(nc -> new CA_DSM()); }
}
