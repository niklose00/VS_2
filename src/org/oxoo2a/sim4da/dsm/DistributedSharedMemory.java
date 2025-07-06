package org.oxoo2a.sim4da.dsm;

/**
 * Minimal interface for a distributed key–value store used in the
 * {@code sim4da} examples.  Implementations may offer different consistency and
 * availability guarantees.
 */
public interface DistributedSharedMemory {

    /**
     * Store or update a value associated with {@code key}.
     *
     * @param key   identifier for the entry
     * @param value value to store
     */
    void write(String key, String value);

    /**
     * Retrieve the value for {@code key} or {@code null} if none exists.
     *
     * @param key the identifier to look up
     * @return the last known value or {@code null}
     */
    String read(String key);
}
