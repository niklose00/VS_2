package org.oxoo2a.sim4da.dsm;

import org.oxoo2a.sim4da.Message;

/**
 * Message used to propagate write operations between DSM nodes.  Each update
 * carries the key, the new value and a logical timestamp so recipients can
 * apply last-write-wins semantics.
 */
public class UpdateMessage extends Message {
    /** key that was written. */
    public final String key;
    /** written value. */
    public final String value;
    /** sender's logical timestamp. */
    public final long timestamp;

    /**
     * Construct a new update message.
     */
    public UpdateMessage(String key, String value, long timestamp) {
        super();
        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
    }

    /** Copy constructor required by {@link Message#copy()}. */
    private UpdateMessage(UpdateMessage original) {
        super(original);
        this.key = original.key;
        this.value = original.value;
        this.timestamp = original.timestamp;
    }
}
