package org.oxoo2a.app;

import org.oxoo2a.sim4da.Simulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for running the counter/DSM simulation.
 */
public class DSMSimulation {
    public static void main(String[] args) throws IOException {
        String variant = args.length > 0 ? args[0] : "CA";
        int nodes = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        int ticks = args.length > 2 ? Integer.parseInt(args[2]) : 200;

        CSVLogger.init("dsm_log.csv");

        List<String> names = new ArrayList<>();
        List<CounterAgent> agents = new ArrayList<>();
        for (int i=0;i<nodes;i++) {
            names.add("n"+i);
        }
        for (String n : names) {
            CounterAgent c = new CounterAgent(n, variant, names, ticks);
            c.engage();
            agents.add(c);
        }

        Simulator.getInstance().simulate();
        Simulator.getInstance().shutdown();
        CSVLogger.close();
    }
}
