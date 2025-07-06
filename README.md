# sim4da

**A Java-based framework for simulating distributed algorithms.**

## Motivation

Distributed algorithms are notoriously difficult to prototype and test due to the complexity of networking, threading, and message handling. **sim4da** abstracts away these concerns, letting you concentrate on your algorithm’s logic. With minimal boilerplate, you can quickly spin up nodes, pass messages, and visualize behavior in a controlled simulation.

## Core Concepts

- **IS-A Node**  
  Extend the `org.oxoo2a.sim4da.Node` base class. The framework handles thread creation, message routing, and logging. Override the `engage()` method to implement your node’s algorithmic steps, using helper methods like `send()`, `broadcast()`, and `receive()`.

- **HAS-A NetworkConnection**  
  For scenarios where inheritance isn’t ideal, instantiate and manage a `NetworkConnection` directly:
  ```java
  public class CustomAgent {
      private final NetworkConnection nc = new NetworkConnection("Agent1");

      public CustomAgent() {
          nc.engage(this::runLogic);
      }

      private void runLogic() {
          // send, receive, process messages
      }
  }
  ```

## Quick Start

### Example: Token Ring with IS-A Node

```java
public class RingNode extends Node {
    public RingNode(String name) {
        super(name);
    }

    @Override
    protected void engage() {
        // send, receive, process token passing
    }
}
```

### Example: Custom Agent with HAS-A NetworkConnection

```java
public class Agent {
    private final NetworkConnection nc = new NetworkConnection("AgentX");

    public Agent() {
        nc.engage(this::main);
    }

    private void main() {
        Message msg = nc.receive();
        // algorithm logic
    }
}
```

## Logging Configuration

By default, a “hidden” `logback.xml` on the classpath configures **DEBUG**-level logging for all network activity and node operations. To customize:

1. Create your own `logback.xml` in `src/main/resources/`.
2. Define desired log levels, appenders, and formats.
3. The simulator will automatically pick up your configuration instead of the default.

## Further Reading

- See `OneRingToRuleThemAllTest.java` for a complete token-passing simulation example.
- Review Javadoc comments in `Node.java` and `NetworkConnection.java` for detailed API guidance.

## Distributed Shared Memory

`sim4da` contains a minimal DSM (Distributed Shared Memory) abstraction that
allows multiple simulated nodes to share a key–value store.  The
`DistributedSharedMemory` interface defines two operations:

```java
void write(String key, String value);
String read(String key);
```

Three example implementations highlight the different CAP trade-offs:

| Implementation | Guarantees | Description |
| -------------- | ---------- | ----------- |
| `CA_DSM` | Consistency & Availability | Uses a synchronous central map assuming no network partitions. |
| `AP_DSM` | Availability & Partition Tolerance | Each node keeps a local copy and gossips updates asynchronously. Reads are always local and eventually consistent. |
| `CP_DSM` | Consistency & Partition Tolerance | Employs simple quorum-based communication for reads and writes. Operations block until a majority of nodes acknowledge. |

See the classes in `src/org/oxoo2a/sim4da/dsm` and the accompanying tests in
`test/org/oxoo2a/sim4da/dsm` for usage examples.
