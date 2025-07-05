package org.oxoo2a.sim4da.demo;

import org.oxoo2a.sim4da.Simulator;
import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.dsm.AP_DSM;
import org.oxoo2a.sim4da.dsm.CA_DSM;
import org.oxoo2a.sim4da.dsm.CP_DSM;
import org.oxoo2a.sim4da.dsm.DistributedSharedMemory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Entry point creating several {@link CounterAgent} instances using
 * a selected DSM implementation.
 */
public class DSMDemo {
    public static void main(String[] args) throws IOException {
        String variant = args.length > 0 ? args[0].toUpperCase(Locale.ROOT) : "AP";
        int nodes = args.length > 1 ? Integer.parseInt(args[1]) : 5;

        Function<NetworkConnection, DistributedSharedMemory> factory;
        switch (variant) {
            case "CP" -> factory = CP_DSM::new;
            case "CA" -> factory = nc -> new CA_DSM();
            default -> factory = AP_DSM::new;
        }

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < nodes; i++) {
            ids.add(String.valueOf(i));
        }

        List<CounterAgent> agents = new ArrayList<>();
        for (String id : ids) {
            agents.add(new CounterAgent(id, factory, ids));
        }

        Simulator simulator = Simulator.getInstance();
        simulator.simulate(10); // run for 10 seconds
        simulator.shutdown();
    }
}
