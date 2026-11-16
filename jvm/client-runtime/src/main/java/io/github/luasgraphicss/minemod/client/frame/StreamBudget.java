package io.github.luasgraphicss.minemod.client.frame;

public final class StreamBudget {
    private static final int RECORD_FRAMING_BYTES = 16;

    private long bytes;
    private long records;
    private boolean hasTaken;

    public StreamBudget(long bytes, long records) {
        if (bytes < 0 || records < 0) {
            throw new IllegalArgumentException("a stream budget cannot be negative: " + bytes + " bytes, " + records
                    + " records");
        }
        this.bytes = bytes;
        this.records = records;
    }

    public boolean take(long payloadBytes) {
        long recordBytes = RECORD_FRAMING_BYTES + payloadBytes;
        if (records == 0 || recordBytes > bytes) {
            return false;
        }
        bytes -= recordBytes;
        records--;
        hasTaken = true;
        return true;
    }

    public boolean hasTaken() {
        return hasTaken;
    }
}
