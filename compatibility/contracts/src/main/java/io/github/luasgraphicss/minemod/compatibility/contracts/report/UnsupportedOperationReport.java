package io.github.luasgraphicss.minemod.compatibility.contracts.report;

import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import java.util.Objects;
import java.util.Optional;

public final class UnsupportedOperationReport {
    private final String operation;
    private final OperationSupport support;
    private final CallSite callSite;
    private final Optional<ResourceContext> resource;

    private UnsupportedOperationReport(
            String operation, OperationSupport support, CallSite callSite, Optional<ResourceContext> resource) {
        if (support == OperationSupport.SUPPORTED) {
            throw new IllegalArgumentException("a supported operation is not reported: " + operation);
        }
        this.operation = Objects.requireNonNull(operation, "operation");
        this.support = Objects.requireNonNull(support, "support");
        this.callSite = Objects.requireNonNull(callSite, "callSite");
        this.resource = resource;
    }

    public static UnsupportedOperationReport of(String operation, OperationSupport support, CallSite callSite) {
        return new UnsupportedOperationReport(operation, support, callSite, Optional.empty());
    }

    public static UnsupportedOperationReport onResource(
            String operation, OperationSupport support, CallSite callSite, ResourceContext resource) {
        return new UnsupportedOperationReport(operation, support, callSite, Optional.of(resource));
    }

    public String operation() {
        return operation;
    }

    public OperationSupport support() {
        return support;
    }

    public CallSite callSite() {
        return callSite;
    }

    public Optional<ResourceContext> resource() {
        return resource;
    }

    @Override
    public String toString() {
        String summary = operation + " is " + support.description() + ", called from " + callSite;
        return resource.map(context -> summary + " on " + context).orElse(summary);
    }
}
