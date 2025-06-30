package org.oxoo2a.sim4da.dsm;

public interface DistributedSharedMemory {
    void write(String key, String value);
    String read(String key);
}
