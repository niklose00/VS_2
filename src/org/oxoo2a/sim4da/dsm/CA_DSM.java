package org.oxoo2a.sim4da.dsm;

import java.util.Map;
import java.util.HashMap;

/**
 * Consistency & Availability implementation.
 * Assumes no network partitions and performs synchronous writes to a central store.
 */
public class CA_DSM implements DistributedSharedMemory {
    private static final Map<String,String> CENTRAL_STORE = new HashMap<>();

    @Override
    public synchronized void write(String key, String value) {
        CENTRAL_STORE.put(key, value);
    }

    @Override
    public synchronized String read(String key) {
        return CENTRAL_STORE.get(key);
    }
}
