package io.github.luasgraphicss.minemod.contract.stream;

import io.github.luasgraphicss.minemod.contract.abi.AbiResult;

public final class StreamReport {
    public static final int NO_COMMAND = -1;
    public static final long NO_OFFSET = -1L;

    private final AbiResult result;
    private final int commandCount;
    private final int failedCommand;
    private final long failedOffset;

    public StreamReport(AbiResult result, int commandCount, int failedCommand, long failedOffset) {
        this.result = result;
        this.commandCount = commandCount;
        this.failedCommand = failedCommand;
        this.failedOffset = failedOffset;
    }

    public static StreamReport accepted(int commandCount) {
        return new StreamReport(AbiResult.OK, commandCount, NO_COMMAND, NO_OFFSET);
    }

    public AbiResult result() {
        return result;
    }

    public boolean isAccepted() {
        return result == AbiResult.OK;
    }

    public int commandCount() {
        return commandCount;
    }

    public int failedCommand() {
        return failedCommand;
    }

    public long failedOffset() {
        return failedOffset;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StreamReport)) {
            return false;
        }
        StreamReport report = (StreamReport) other;
        return report.result == result
                && report.commandCount == commandCount
                && report.failedCommand == failedCommand
                && report.failedOffset == failedOffset;
    }

    @Override
    public int hashCode() {
        int hash = result.hashCode();
        hash = 31 * hash + commandCount;
        hash = 31 * hash + failedCommand;
        return 31 * hash + Long.hashCode(failedOffset);
    }

    @Override
    public String toString() {
        return result + " after " + commandCount + " command(s), failed command " + failedCommand
                + " at offset " + failedOffset;
    }
}
