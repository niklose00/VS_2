package org.oxoo2a.sim4da.dsm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CA_DSM_Test {
    @Test
    void testConsistency() {
        CA_DSM dsm1 = new CA_DSM();
        CA_DSM dsm2 = new CA_DSM();
        dsm1.write("k", "v1");
        assertEquals("v1", dsm2.read("k"));
        dsm2.write("k", "v2");
        assertEquals("v2", dsm1.read("k"));
    }
}
