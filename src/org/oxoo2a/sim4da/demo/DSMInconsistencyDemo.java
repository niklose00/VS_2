package org.oxoo2a.sim4da.demo;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Simulator;
import org.oxoo2a.sim4da.dsm.*;

/**
 * Simple demo showing how different DSM strategies affect consistency.
 */
public class DSMInconsistencyDemo {
    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "CP";

        Simulator sim = Simulator.getInstance();
        NetworkConnection nc1 = new NetworkConnection("n1");
        NetworkConnection nc2 = new NetworkConnection("n2");

        DistributedSharedMemory dsm1;
        DistributedSharedMemory dsm2;
        switch (mode) {
            case "AP" -> {
                dsm1 = new AP_DSM(nc1);
                dsm2 = new AP_DSM(nc2);
            }
            case "CA" -> {
                dsm1 = new CA_DSM();
                dsm2 = new CA_DSM();
            }
            default -> {
                dsm1 = new CP_DSM(nc1);
                dsm2 = new CP_DSM(nc2);
            }
        }

        // Engage dummy node threads so the simulator can run
        nc1.engage(() -> {});
        nc2.engage(() -> {});

        sim.simulate(1);

        // Node1 writes a value which node2 should eventually see
        dsm1.write("key", "value");
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {}

        System.out.println("Node2 reads: " + dsm2.read("key"));

        sim.shutdown();
    }
}
