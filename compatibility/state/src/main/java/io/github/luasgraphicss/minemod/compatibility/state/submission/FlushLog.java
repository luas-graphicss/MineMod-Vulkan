package io.github.luasgraphicss.minemod.compatibility.state.submission;

import java.util.ArrayList;
import java.util.List;

public final class FlushLog {
    private static final FlushReason[] REASONS = FlushReason.values();

    private final long[] frames;
    private final int[] checkpoints;
    private final int[] reasons;
    private final long[] countsByReason = new long[REASONS.length];
    private long recordedCount;

    public FlushLog(int recentCapacity) {
        if (recentCapacity < 1) {
            throw new IllegalArgumentException("a flush log needs room for at least one record: " + recentCapacity);
        }
        this.frames = new long[recentCapacity];
        this.checkpoints = new int[recentCapacity];
        this.reasons = new int[recentCapacity];
    }

    public void record(long frame, int checkpoint, FlushReason reason) {
        int position = (int) (recordedCount % frames.length);
        frames[position] = frame;
        checkpoints[position] = checkpoint;
        reasons[position] = reason.ordinal();
        countsByReason[reason.ordinal()]++;
        recordedCount++;
    }

    public List<FlushRecord> recent() {
        int kept = (int) Math.min(recordedCount, frames.length);
        List<FlushRecord> records = new ArrayList<>(kept);
        for (long sequence = recordedCount - kept; sequence < recordedCount; sequence++) {
            int position = (int) (sequence % frames.length);
            records.add(new FlushRecord(frames[position], checkpoints[position], REASONS[reasons[position]]));
        }
        return records;
    }

    public long count(FlushReason reason) {
        return countsByReason[reason.ordinal()];
    }

    public long recordedCount() {
        return recordedCount;
    }
}
