package io.github.luasgraphicss.minemod.compatibility.translation.call;

import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallDescription;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.Checkpoint;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.Interception;
import io.github.luasgraphicss.minemod.compatibility.state.submission.SubmissionCheckpoints;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.OpenConstructs;
import io.github.luasgraphicss.minemod.compatibility.translation.names.NameArguments;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CallTranslator {
    public static final long DROPPED = 0;

    private final CallCatalog catalog;
    private final NameArguments names;
    private final SubmissionCheckpoints checkpoints;
    private final OpenConstructs constructs;
    private final List<CallObserver> observers;
    private final ProviderCalls provider;

    public CallTranslator(
            CallCatalog catalog,
            NameArguments names,
            SubmissionCheckpoints checkpoints,
            OpenConstructs constructs,
            List<CallObserver> observers,
            ProviderCalls provider) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.names = Objects.requireNonNull(names, "names");
        this.checkpoints = Objects.requireNonNull(checkpoints, "checkpoints");
        this.constructs = Objects.requireNonNull(constructs, "constructs");
        this.observers = new ArrayList<>(observers);
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public long call(String function, long[] arguments) {
        CallDescription description = catalog.describe(function)
                .filter(described -> described.interception() == Interception.TRANSLATE)
                .orElseThrow(() -> new IllegalArgumentException(function + " is not translated call by call"));
        long[] translated = arguments.clone();
        if (!names.translate(description, translated)) {
            return DROPPED;
        }
        constructs.observe(description.function(), arguments);
        for (CallObserver observer : observers) {
            observer.observe(description.function(), arguments);
        }
        if (!constructs.defersExecution()) {
            reach(description.checkpoint());
        }
        return provider.invoke(function, translated);
    }

    private void reach(Checkpoint checkpoint) {
        if (checkpoint == Checkpoint.DRAW) {
            checkpoints.beforeProviderDraw();
        } else if (checkpoint == Checkpoint.READ) {
            checkpoints.beforeSynchronousRead();
        }
    }
}
