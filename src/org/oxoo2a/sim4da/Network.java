package org.oxoo2a.sim4da;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Network {

    private Network() {
    }

    private record Node ( NetworkConnection nc, NodeProxy np ) {}
    private final Map<String,Node> nodes = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(Network.class);
    private final ConcurrentMap<String, Integer> delays = new ConcurrentHashMap<>();
    private final Set<String> partitions = new ConcurrentSkipListSet();
    private static Network instance = null;
    public static Network getInstance() {
        if (instance == null) {
            synchronized (Network.class) {
                if (instance == null) {
                    instance = new Network();
                }
            }
        }
        return instance;
    }

    public void registerConnection(NetworkConnection networkConnection, NodeProxy nodeProxy) {
        logger.debug("Registering connection for " + networkConnection.NodeName());
        Node n = new Node(networkConnection, nodeProxy);
        nodes.put(networkConnection.NodeName(), n);
    }

    public List<NetworkConnection> getAllNetworkConnections () {
        List<NetworkConnection> ncs = new ArrayList<>(numberOfNodes());
        for (Node n : nodes.values()) {
            ncs.add(n.nc);
        }
        return ncs;
    }

    public int numberOfNodes() {
        return nodes.size();
    }

    public void send ( Message message, NetworkConnection sender, String receiver_name ) throws UnknownNodeException {
        if (!nodes.containsKey(receiver_name)) {
            logger.error("Attempt to send message to non-existent node " + receiver_name);
            throw new UnknownNodeException(receiver_name);
        }
        if (isPartitioned(sender.NodeName()) || isPartitioned(receiver_name)) return;
        Message copy = message.copy();
        copy.setSender(sender.NodeName());
        NodeProxy receiver = nodes.get(receiver_name).np;
        deliverWithDelay(receiver, copy, sender, getDelay(receiver_name));
    }

    public void send ( Message message, NetworkConnection sender ) {
        for (Node n : nodes.values()) {
            if (n.nc != sender) {
                if (isPartitioned(sender.NodeName()) || isPartitioned(n.nc.NodeName())) continue;
                Message copy = message.copy();
                copy.setSender(sender.NodeName());
                deliverWithDelay(n.np, copy, sender, getDelay(n.nc.NodeName()));
            }
        }
    }

    public Message receive(NetworkConnection receiver) {
        Node n = nodes.get(receiver.NodeName());
        Message m = n.np.receive();
        return m;
    }

    private boolean isPartitioned(String node) {
        return partitions.contains(node);
    }

    private int getDelay(String node) {
        return delays.getOrDefault(node, 0);
    }

    private void deliverWithDelay(NodeProxy receiver, Message msg, NetworkConnection sender, int delay) {
        if (delay <= 0) {
            receiver.deliver(msg, sender);
        } else {
            new Thread(() -> {
                try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
                receiver.deliver(msg, sender);
            }).start();
        }
    }

    public void setNodeDelay(String node, int delayMs) { delays.put(node, delayMs); }
    public void clearNodeDelay(String node) { delays.remove(node); }
    public void partitionNode(String node) { partitions.add(node); }
    public void reconnectNode(String node) { partitions.remove(node); }
    public void reset() {
        nodes.clear();
        delays.clear();
        partitions.clear();
    }

    public void shutdown() {
    }
}
