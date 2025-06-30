package org.oxoo2a.sim4da.dsm;

import org.junit.jupiter.api.Test;
import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Simulator;

import static org.junit.jupiter.api.Assertions.*;

public class AP_DSM_Test {
    @Test
    void testEventualConsistency() {
        Simulator sim = Simulator.getInstance();
        NetworkConnection nc1 = new NetworkConnection("n1");
        NetworkConnection nc2 = new NetworkConnection("n2");

        AP_DSM dsm1 = new AP_DSM(nc1);
        AP_DSM dsm2 = new AP_DSM(nc2);

        nc1.engage(() -> {}); // start threads
        nc2.engage(() -> {});

        sim.simulate(1); // run

        dsm1.write("key","value");
        try { Thread.sleep(200); } catch (InterruptedException e) {}
        assertEquals("value", dsm2.read("key"));
        sim.shutdown();
    }
}
