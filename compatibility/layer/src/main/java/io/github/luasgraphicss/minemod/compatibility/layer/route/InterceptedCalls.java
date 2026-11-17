package io.github.luasgraphicss.minemod.compatibility.layer.route;

import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallDescription;
import io.github.luasgraphicss.minemod.compatibility.layer.capability.ExposedExtensions;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import io.github.luasgraphicss.minemod.compatibility.translation.call.CallTranslator;
import io.github.luasgraphicss.minemod.compatibility.translation.framebuffer.VirtualFramebufferCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.DisplayListCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.DisplayListNames;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.query.BindingQueries;
import io.github.luasgraphicss.minemod.compatibility.translation.query.QueryCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.report.OperationReporter;
import java.util.Objects;

public final class InterceptedCalls {
    private final CallCatalog catalog;
    private final CallTranslator translator;
    private final ErrorLedger errors;
    private final ProviderCalls provider;
    private final ObjectRoutes objects;
    private final FramebufferRoutes framebuffers;
    private final SpanRoutes spans;
    private final StateQueryRoutes stateQueries;
    private final CapabilityRoutes capabilities;

    private InterceptedCalls(Builder builder) {
        this.catalog = builder.catalog;
        this.translator = builder.translator;
        this.errors = builder.errors;
        this.provider = builder.provider;
        ListRoutes lists = new ListRoutes(
                builder.listNames, builder.listCalls, builder.errors, builder.reporter, builder.memory);
        this.objects = new ObjectRoutes(builder.names, builder.queries, lists, builder.provider, builder.memory);
        this.framebuffers =
                new FramebufferRoutes(builder.framebuffers, builder.names, builder.provider, builder.memory);
        this.spans = new SpanRoutes(builder.queries);
        this.stateQueries = new StateQueryRoutes(
                builder.bindings, builder.names, builder.exposed, builder.provider, builder.memory);
        this.capabilities = new CapabilityRoutes(builder.exposed, builder.profile, builder.provider, builder.errors);
    }

    public static Builder builder() {
        return new Builder();
    }

    public long call(String function, long[] words) {
        CallDescription description = catalog.describe(function)
                .orElseThrow(() -> new IllegalArgumentException(function + " is not described by the call catalog"));
        String coreFunction = description.function();
        switch (description.interception()) {
            case FORWARD:
                return provider.invoke(function, words);
            case TRANSLATE:
                return translator.call(function, words);
            case OBJECTS:
                return objects.call(coreFunction, function, words);
            case FRAMEBUFFER:
                framebuffers.call(coreFunction, function, words);
                return 0;
            case SPANS:
                return spans.call(coreFunction, function, words);
            case ERRORS:
                return errors.take();
            case CAPABILITIES:
                return capabilities.call(coreFunction, function, words);
            case STATE_QUERY:
                stateQueries.call(coreFunction, function, words);
                return 0;
            default:
                throw new IllegalStateException(function + " has an interception the layer does not route");
        }
    }

    public static final class Builder {
        private CallCatalog catalog;
        private ContextProfile profile;
        private CallTranslator translator;
        private ObjectNames names;
        private DisplayListNames listNames;
        private DisplayListCalls listCalls;
        private QueryCalls queries;
        private VirtualFramebufferCalls framebuffers;
        private BindingQueries bindings;
        private ExposedExtensions exposed;
        private ErrorLedger errors;
        private OperationReporter reporter;
        private ProviderCalls provider;
        private NativeMemory memory;

        private Builder() {
        }

        public Builder context(CallCatalog describedCalls, ContextProfile contextProfile) {
            this.catalog = Objects.requireNonNull(describedCalls, "describedCalls");
            this.profile = Objects.requireNonNull(contextProfile, "contextProfile");
            return this;
        }

        public Builder translation(CallTranslator callTranslator, ObjectNames objectNames) {
            this.translator = Objects.requireNonNull(callTranslator, "callTranslator");
            this.names = Objects.requireNonNull(objectNames, "objectNames");
            return this;
        }

        public Builder displayLists(DisplayListNames displayListNames, DisplayListCalls displayListCalls) {
            this.listNames = Objects.requireNonNull(displayListNames, "displayListNames");
            this.listCalls = Objects.requireNonNull(displayListCalls, "displayListCalls");
            return this;
        }

        public Builder queries(QueryCalls queryCalls, BindingQueries bindingQueries) {
            this.queries = Objects.requireNonNull(queryCalls, "queryCalls");
            this.bindings = Objects.requireNonNull(bindingQueries, "bindingQueries");
            return this;
        }

        public Builder framebuffers(VirtualFramebufferCalls framebufferCalls) {
            this.framebuffers = Objects.requireNonNull(framebufferCalls, "framebufferCalls");
            return this;
        }

        public Builder capabilities(ExposedExtensions exposedExtensions) {
            this.exposed = Objects.requireNonNull(exposedExtensions, "exposedExtensions");
            return this;
        }

        public Builder diagnostics(ErrorLedger errorLedger, OperationReporter operationReporter) {
            this.errors = Objects.requireNonNull(errorLedger, "errorLedger");
            this.reporter = Objects.requireNonNull(operationReporter, "operationReporter");
            return this;
        }

        public Builder provider(ProviderCalls providerCalls, NativeMemory nativeMemory) {
            this.provider = Objects.requireNonNull(providerCalls, "providerCalls");
            this.memory = Objects.requireNonNull(nativeMemory, "nativeMemory");
            return this;
        }

        public InterceptedCalls build() {
            Objects.requireNonNull(catalog, "context");
            Objects.requireNonNull(translator, "translation");
            Objects.requireNonNull(listNames, "displayLists");
            Objects.requireNonNull(queries, "queries");
            Objects.requireNonNull(framebuffers, "framebuffers");
            Objects.requireNonNull(exposed, "capabilities");
            Objects.requireNonNull(errors, "diagnostics");
            Objects.requireNonNull(provider, "provider");
            return new InterceptedCalls(this);
        }
    }
}
