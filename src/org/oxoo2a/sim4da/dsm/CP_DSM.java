package org.oxoo2a.sim4da.dsm;

import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Message;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consistency & Partition Tolerance implementation using simple quorums.
 */
public class CP_DSM implements DistributedSharedMemory {
    private final NetworkConnection nc;
    private final Map<String,ValueEntry> store = new ConcurrentHashMap<>();
    private final AtomicInteger clock = new AtomicInteger(0);

    public CP_DSM(NetworkConnection nc) {
        this.nc = nc;
        Thread t = new Thread(this::processMessages);
        t.start();
    }

    @Override
    public void write(String key, String value) {
        int ts = clock.incrementAndGet();
        UpdateMessage msg = new UpdateMessage(key, value, ts);
        QuorumTracker tracker = new QuorumTracker();
        nc.send(msg); // broadcast
        tracker.awaitQuorum();
        store.put(key, new ValueEntry(value, ts));
    }

    @Override
    public String read(String key) {
        RequestMessage req = new RequestMessage(key);
        QuorumTracker tracker = new QuorumTracker();
        nc.send(req); // broadcast
        tracker.awaitQuorum();
        ValueEntry ve = store.get(key);
        return ve == null ? null : ve.value;
    }

    private void processMessages() {
        while (true) {
            Message m = nc.receive();
            switch (m) {
                case UpdateMessage up -> {
                    ValueEntry cur = store.get(up.key);
                    if (cur == null || up.timestamp > cur.timestamp) {
                        store.put(up.key, new ValueEntry(up.value, (int)up.timestamp));
                        clock.updateAndGet(v -> Math.max(v, (int) up.timestamp));
                    }
                    AckMessage ack = new AckMessage();
                    nc.sendBlindly(ack, m.getSender());
                }
                case RequestMessage req -> {
                    ValueEntry ve = store.get(req.key);
                    ResponseMessage resp = new ResponseMessage(req.key, ve==null?null:ve.value, clock.get());
                    nc.sendBlindly(resp, m.getSender());
                }
                case AckMessage ack -> {
                    QuorumTracker.notifyAck();
                }
                case ResponseMessage resp -> {
                    QuorumTracker.notifyAck();
                }
                default -> {
                }
            }
        }
    }

    // local types
    private record ValueEntry(String value, int timestamp) {}

    private static class QuorumTracker {
        private static final int nodes = org.oxoo2a.sim4da.Network.getInstance().numberOfNodes();
        private static final AtomicInteger received = new AtomicInteger(0);
        private static CountDownLatch latch = new CountDownLatch(required());

        static void notifyAck() {
            if (received.incrementAndGet() >= required()) {
                latch.countDown();
            }
        }

        static int required() { return nodes/2 + 1; }

        void awaitQuorum() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            received.set(0);
            latch = new CountDownLatch(required());
        }
    }

    // Message types
    private static class AckMessage extends Message {
        AckMessage() {}
        private AckMessage(AckMessage original) { super(original); }
    }
    private static class RequestMessage extends Message {
        final String key;
        RequestMessage(String key) { super(); this.key = key; }
        private RequestMessage(RequestMessage o) { super(o); this.key = o.key; }
    }
    private static class ResponseMessage extends Message {
        final String key;
        final String value;
        final int timestamp;
        ResponseMessage(String key, String value, int timestamp) {
            super();
            this.key = key;
            this.value = value;
            this.timestamp = timestamp;
        }
        private ResponseMessage(ResponseMessage o) {
            super(o); this.key = o.key; this.value = o.value; this.timestamp = o.timestamp; }
    }
}
