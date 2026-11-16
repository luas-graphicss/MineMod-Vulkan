package io.github.luasgraphicss.minemod.compatibility.translation.legacy;

import io.github.luasgraphicss.minemod.compatibility.state.submission.SubmissionCheckpoints;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.Objects;

public final class DisplayListCalls {
    private static final String CALL_LISTS = "glCallLists";

    private final DisplayListNames lists;
    private final OpenConstructs constructs;
    private final SubmissionCheckpoints checkpoints;
    private final ProviderCalls provider;

    public DisplayListCalls(
            DisplayListNames lists, OpenConstructs constructs, SubmissionCheckpoints checkpoints, ProviderCalls provider) {
        this.lists = Objects.requireNonNull(lists, "lists");
        this.constructs = Objects.requireNonNull(constructs, "constructs");
        this.checkpoints = Objects.requireNonNull(checkpoints, "checkpoints");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public void callLists(int[] offsets) {
        int[] backings = lists.resolveCalls(offsets);
        long[] arguments = new long[backings.length];
        for (int index = 0; index < backings.length; index++) {
            arguments[index] = backings[index];
        }
        if (!constructs.defersExecution()) {
            checkpoints.beforeProviderDraw();
        }
        provider.invoke(CALL_LISTS, arguments);
    }
}
