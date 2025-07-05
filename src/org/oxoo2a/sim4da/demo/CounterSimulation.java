package org.oxoo2a.sim4da.demo;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Simulator;
import org.oxoo2a.sim4da.dsm.AP_DSM;
import org.oxoo2a.sim4da.dsm.CA_DSM;
import org.oxoo2a.sim4da.dsm.CP_DSM;
import org.oxoo2a.sim4da.dsm.DistributedSharedMemory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a simulation of multiple CounterNodes using one of the DSM variants.
 * Usage: CounterSimulation <AP|CP|CA> [seconds] [nodes]
 */
public class CounterSimulation {

    public static void main(String[] args) throws IOException {
        String variant = args.length > 0 ? args[0] : "AP";
        int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 10;
        int nodes = args.length > 2 ? Integer.parseInt(args[2]) : 5;

        PrintWriter log = new PrintWriter(new FileWriter("counter-log.txt"));

        List<String> names = new ArrayList<>();
        for (int i = 0; i < nodes; i++) {
            names.add("n" + i);
        }

        List<CounterNode> nodeList = new ArrayList<>();
        for (String name : names) {
            NetworkConnection nc = new NetworkConnection(name);
            DistributedSharedMemory dsm = switch (variant) {
                case "CP" -> new CP_DSM(nc);
                case "CA" -> new CA_DSM();
                default -> new AP_DSM(nc);
            };
            CounterNode cn = new CounterNode(name, dsm, names, log, nc);
            nodeList.add(cn);
            cn.engage();
        }

        Simulator sim = Simulator.getInstance();
        sim.simulate(seconds);
        sim.shutdown();
        log.close();
    }
}
