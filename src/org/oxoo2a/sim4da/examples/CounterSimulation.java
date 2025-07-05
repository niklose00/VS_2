package org.oxoo2a.sim4da.examples;

import org.oxoo2a.sim4da.Simulator;

import java.util.ArrayList;
import java.util.List;

/**
 * Launches a small cluster of CounterNodes using a selectable DSM variant.
 * Usage: CounterSimulation <CA|AP|CP> [nodes] [iterations]
 */
public class CounterSimulation {
    public static void main(String[] args) {
        String variant = args.length > 0 ? args[0] : "CA";
        int nodeCount = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        int iterations = args.length > 2 ? Integer.parseInt(args[2]) : 20;

        Simulator sim = Simulator.getInstance();
        String[] keys = new String[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            keys[i] = "n" + i;
        }

        List<CounterNode> nodes = new ArrayList<>();
        for (String k : keys) {
            nodes.add(new CounterNode(k, variant, keys, iterations));
        }

        sim.simulate();
        sim.shutdown();
    }
}
