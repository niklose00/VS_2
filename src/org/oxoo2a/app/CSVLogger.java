package org.oxoo2a.app;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility for synchronized CSV logging of observation entries.
 */
public class CSVLogger {
    private static PrintWriter writer;

    /** Initialize the logger with given file path. */
    public static void init(String path) throws IOException {
        writer = new PrintWriter(new FileWriter(path));
        writer.println("tick,observer,observed,value,type,prev");
    }

    /** Log an entry to the CSV file. */
    public static synchronized void log(int tick, String observer, String observed,
                                        String value, String type, Integer prev) {
        if (writer == null) return;
        writer.printf("%d,%s,%s,%s,%s,%s%n", tick, observer, observed,
                value == null ? "" : value, type,
                prev == null ? "" : prev.toString());
    }

    /** Close the underlying writer. */
    public static void close() {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }
}
