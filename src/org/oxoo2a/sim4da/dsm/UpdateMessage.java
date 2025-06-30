package org.oxoo2a.sim4da.dsm;

import org.oxoo2a.sim4da.Message;

/**
 * Message used to propagate write operations between DSM nodes.
 */
public class UpdateMessage extends Message {
    public final String key;
    public final String value;
    public final long timestamp;

    public UpdateMessage(String key, String value, long timestamp) {
        super();
        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
    }

    // Copy constructor required by Message
    private UpdateMessage(UpdateMessage original) {
        super(original);
        this.key = original.key;
        this.value = original.value;
        this.timestamp = original.timestamp;
    }
}
