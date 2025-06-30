package org.oxoo2a.sim4da.dsm;

import org.junit.jupiter.api.Test;
import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Simulator;

import static org.junit.jupiter.api.Assertions.*;

public class CP_DSM_Test {
    @Test
    void testQuorumWriteRead() {
        Simulator sim = Simulator.getInstance();
        NetworkConnection nc1 = new NetworkConnection("n1");
        NetworkConnection nc2 = new NetworkConnection("n2");
        CP_DSM dsm1 = new CP_DSM(nc1);
        CP_DSM dsm2 = new CP_DSM(nc2);
        nc1.engage(() -> {});
        nc2.engage(() -> {});
        sim.simulate(1);
        dsm1.write("k","v");
        try { Thread.sleep(200); } catch (InterruptedException e) {}
        assertEquals("v", dsm2.read("k"));
        sim.shutdown();
    }
}
