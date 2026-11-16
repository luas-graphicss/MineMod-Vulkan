package io.github.luasgraphicss.minemod.compatibility.dispatch.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CallDescription {
    private final String function;
    private final CallFamily family;
    private final Interception interception;
    private final Checkpoint checkpoint;
    private final List<NameArgument> nameArguments;

    public CallDescription(
            String function,
            CallFamily family,
            Interception interception,
            Checkpoint checkpoint,
            List<NameArgument> nameArguments) {
        if (interception == Interception.FORWARD && (checkpoint != Checkpoint.NONE || !nameArguments.isEmpty())) {
            throw new IllegalArgumentException(function + " cannot be forwarded untouched and still be translated");
        }
        this.function = Objects.requireNonNull(function, "function");
        this.family = Objects.requireNonNull(family, "family");
        this.interception = interception;
        this.checkpoint = Objects.requireNonNull(checkpoint, "checkpoint");
        this.nameArguments = Collections.unmodifiableList(new ArrayList<>(nameArguments));
    }

    public String function() {
        return function;
    }

    public CallFamily family() {
        return family;
    }

    public Interception interception() {
        return interception;
    }

    public Checkpoint checkpoint() {
        return checkpoint;
    }

    public List<NameArgument> nameArguments() {
        return nameArguments;
    }
}
