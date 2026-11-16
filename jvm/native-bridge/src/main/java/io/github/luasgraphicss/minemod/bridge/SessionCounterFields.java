package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.contract.session.SessionCounters;

final class SessionCounterFields {
    static final int COUNT = 8;

    private static final int SUBMISSIONS_ACCEPTED = 0;
    private static final int SUBMISSIONS_REJECTED = 1;
    private static final int SUBMISSIONS_ABORTED = 2;
    private static final int COMMANDS_EXECUTED = 3;
    private static final int BUFFERS_CREATED = 4;
    private static final int BUFFERS_DESTROYED = 5;
    private static final int LIVE_BUFFERS = 6;
    private static final int LIVE_BUFFER_BYTES = 7;

    private SessionCounterFields() {}

    static SessionCounters decode(long[] fields) {
        return new SessionCounters(
                fields[SUBMISSIONS_ACCEPTED],
                fields[SUBMISSIONS_REJECTED],
                fields[SUBMISSIONS_ABORTED],
                fields[COMMANDS_EXECUTED],
                fields[BUFFERS_CREATED],
                fields[BUFFERS_DESTROYED],
                fields[LIVE_BUFFERS],
                fields[LIVE_BUFFER_BYTES]);
    }
}
