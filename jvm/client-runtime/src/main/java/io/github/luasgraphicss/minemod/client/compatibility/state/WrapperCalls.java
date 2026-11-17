package io.github.luasgraphicss.minemod.client.compatibility.state;

public final class WrapperCalls {
    private static final WrapperCall[] CALLS = WrapperCall.values();
    private static final long LOW_WORD = 0xFFFFFFFFL;

    private final long[][] arguments = new long[CALLS.length][];
    private final boolean[] isWaiting = new boolean[CALLS.length];
    private boolean hasWaiting;

    public WrapperCalls() {
        for (WrapperCall call : CALLS) {
            arguments[call.ordinal()] = new long[call.argumentCount()];
        }
    }

    public static long word(float value) {
        return Float.floatToRawIntBits(value) & LOW_WORD;
    }

    public static long word(boolean value) {
        return value ? 1 : 0;
    }

    public void keep(WrapperCall call, long argument) {
        long[] kept = argumentsOf(call, 1);
        kept[0] = argument;
    }

    public void keep(WrapperCall call, long first, long second) {
        long[] kept = argumentsOf(call, 2);
        kept[0] = first;
        kept[1] = second;
    }

    public void keep(WrapperCall call, long first, long second, long third, long fourth) {
        long[] kept = argumentsOf(call, 4);
        kept[0] = first;
        kept[1] = second;
        kept[2] = third;
        kept[3] = fourth;
    }

    public int replayInto(Provider provider) {
        if (!hasWaiting) {
            return 0;
        }
        hasWaiting = false;
        int replayed = 0;
        for (WrapperCall call : CALLS) {
            if (isWaiting[call.ordinal()]) {
                isWaiting[call.ordinal()] = false;
                provider.call(call.function(), arguments[call.ordinal()].clone());
                replayed++;
            }
        }
        return replayed;
    }

    private long[] argumentsOf(WrapperCall call, int given) {
        if (given != call.argumentCount()) {
            throw new IllegalArgumentException(
                    call.function() + " takes " + call.argumentCount() + " argument(s), not " + given);
        }
        isWaiting[call.ordinal()] = true;
        hasWaiting = true;
        return arguments[call.ordinal()];
    }

    public interface Provider {
        void call(String function, long[] words);
    }
}
