package org.oxoo2a.sim4da.dsm;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Message;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Availability & Partition Tolerance implementation.
 * Each node keeps local copies and propagates writes asynchronously.
 */
public class AP_DSM implements DistributedSharedMemory {
    private final NetworkConnection nc;
    private final Map<String,ValueEntry> store = new HashMap<>();
    private final AtomicLong clock = new AtomicLong(0);

    public AP_DSM(NetworkConnection nc) {
        this.nc = nc;
        // start background listener
        Thread t = new Thread(this::processUpdates);
        t.start();
    }

    @Override
    public void write(String key, String value) {
        long ts = clock.incrementAndGet();
        store.put(key, new ValueEntry(value, ts));
        UpdateMessage msg = new UpdateMessage(key, value, ts);
        nc.send(msg); // broadcast
    }

    @Override
    public String read(String key) {
        ValueEntry e = store.get(key);
        return e != null ? e.value : null;
    }

    private void processUpdates() {
        while (true) {
            Message m = nc.receive();
            if (m instanceof UpdateMessage up) {
                ValueEntry cur = store.get(up.key);
                if (cur == null || up.timestamp > cur.timestamp) {
                    store.put(up.key, new ValueEntry(up.value, up.timestamp));
                    clock.updateAndGet(v -> Math.max(v, up.timestamp));
                }
            }
        }
    }

    private record ValueEntry(String value, long timestamp) {}
}
