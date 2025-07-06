package org.oxoo2a.sim4da.dsm;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Message;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DSM implementation providing <strong>A</strong>vailability and
 * <strong>P</strong>artition tolerance.  Every node maintains a local copy of
 * the data and broadcasts updates to others.  Updates are applied
 * asynchronously, so reads may observe stale values until gossip converges.
 */
public class AP_DSM implements DistributedSharedMemory {
    private final NetworkConnection nc;
    private final Map<String,ValueEntry> store = new HashMap<>();
    private final AtomicLong clock = new AtomicLong(0);

    /**
     * Construct an AP_DSM bound to the given network connection.  The
     * constructor spawns a background thread that continuously processes
     * update messages.
     */
    public AP_DSM(NetworkConnection nc) {
        this.nc = nc;
        // start background listener
        Thread t = new Thread(this::processUpdates);
        t.setDaemon(true); // allow JVM to exit after tests
        t.start();
    }

    @Override
    /**
     * Store the value locally and broadcast the update to all other nodes.
     */
    public void write(String key, String value) {
        long ts = clock.incrementAndGet();
        store.put(key, new ValueEntry(value, ts));
        UpdateMessage msg = new UpdateMessage(key, value, ts);
        nc.send(msg); // broadcast
    }

    @Override
    /**
     * Read the value from the local copy.  The result may be outdated while
     * updates are still in flight.
     */
    public String read(String key) {
        ValueEntry e = store.get(key);
        return e != null ? e.value : null;
    }

    /** Background thread that merges incoming updates using a last-write-wins
     * strategy based on timestamps. */
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
