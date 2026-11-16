package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.contract.abi.AbiResult;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;

final class StreamReportFields {
    static final int COUNT = 3;

    private static final int COMMAND_COUNT = 0;
    private static final int FAILED_COMMAND = 1;
    private static final int FAILED_OFFSET = 2;

    private StreamReportFields() {}

    static StreamReport decode(int resultCode, long[] fields) {
        return new StreamReport(
                AbiResult.fromCode(resultCode),
                (int) fields[COMMAND_COUNT],
                (int) fields[FAILED_COMMAND],
                fields[FAILED_OFFSET]);
    }
}
