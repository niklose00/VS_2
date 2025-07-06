package org.oxoo2a.sim4da.dsm;

import java.util.Map;
import java.util.HashMap;

/**
 * Distributed shared memory fulfilling <strong>C</strong>onsistency and
 * <strong>A</strong>vailability.  All operations access a central in-memory
 * map and are synchronized.  Network partitions are assumed not to occur.
 */
public class CA_DSM implements DistributedSharedMemory {
    private static final Map<String,String> CENTRAL_STORE = new HashMap<>();

    /**
     * Create a new CA_DSM instance.  All instances operate on the same central
     * store to provide immediate consistency.
     */
    public CA_DSM() {}

    @Override
    public synchronized void write(String key, String value) {
        CENTRAL_STORE.put(key, value);
    }

    @Override
    public synchronized String read(String key) {
        return CENTRAL_STORE.get(key);
    }
}
