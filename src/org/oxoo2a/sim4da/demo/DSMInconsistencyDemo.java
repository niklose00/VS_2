package org.oxoo2a.sim4da.demo;

import org.oxoo2a.sim4da.Simulator;

/**
 * Starts a small simulation with multiple {@link CounterAgent}s using
 * one of the DSM variants. The application prints detected inconsistencies
 * to the console, allowing users to compare the behaviour of CA, AP and CP
 * implementations.
 */
public class DSMInconsistencyDemo {
    public static void main(String[] args) {
        CounterAgent.DSMVariant variant = CounterAgent.DSMVariant.CA;
        if (args.length > 0) {
            try {
                variant = CounterAgent.DSMVariant.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException ignored) {
                System.err.println("Unknown DSM variant '" + args[0] + "', using CA");
            }
        }

        Simulator sim = Simulator.getInstance();
        int nodeCount = 3;
        String[] ids = new String[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            ids[i] = String.valueOf(i);
        }
        CounterAgent[] agents = new CounterAgent[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            agents[i] = new CounterAgent(ids[i], ids, variant);
        }

        // run simulation for a short period
        sim.simulate(5);
        sim.shutdown();
    }
}
