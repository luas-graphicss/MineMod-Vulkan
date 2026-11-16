package io.github.luasgraphicss.minemod.compatibility.layer;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.CallSiteLocator;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.ReportLog;
import io.github.luasgraphicss.minemod.compatibility.layer.capability.ExposedExtensions;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.layer.provider.ListCallMarshalling;
import io.github.luasgraphicss.minemod.compatibility.layer.provider.TypedProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.layer.route.InterceptedCalls;
import io.github.luasgraphicss.minemod.compatibility.layer.state.WrapperState;
import io.github.luasgraphicss.minemod.compatibility.layer.texture.BoundTextureRedirection;
import io.github.luasgraphicss.minemod.compatibility.resources.alias.VanillaAliases;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.DefaultFramebufferBuffers;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.FramebufferBindings;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ContextNames;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import io.github.luasgraphicss.minemod.compatibility.resources.redefine.TextureRedefinition;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushLog;
import io.github.luasgraphicss.minemod.compatibility.state.submission.SubmissionCheckpoints;
import io.github.luasgraphicss.minemod.compatibility.translation.call.CallObserver;
import io.github.luasgraphicss.minemod.compatibility.translation.call.CallTranslator;
import io.github.luasgraphicss.minemod.compatibility.translation.framebuffer.VirtualFramebufferCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.DisplayListCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.DisplayListNames;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.OpenConstructs;
import io.github.luasgraphicss.minemod.compatibility.translation.names.NameArguments;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.query.BindingQueries;
import io.github.luasgraphicss.minemod.compatibility.translation.query.QueryCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.query.QuerySpans;
import io.github.luasgraphicss.minemod.compatibility.translation.report.OperationReporter;
import io.github.luasgraphicss.minemod.compatibility.translation.section.MixedSections;
import io.github.luasgraphicss.minemod.compatibility.translation.texture.AliasStorageGuard;
import io.github.luasgraphicss.minemod.compatibility.translation.texture.TextureBindings;
import java.util.Arrays;
import java.util.List;

public final class CompatibilityLayer implements AutoCloseable {
    private static final int REPORT_CAPACITY = 256;
    private static final int FLUSH_HISTORY = 256;
    private static final int SCRATCH_VALUES = 4;
    private static final List<String> INFRASTRUCTURE_PACKAGES =
            Arrays.asList("io.github.luasgraphicss.minemod.", "org.lwjgl.", "java.", "jdk.", "sun.");
    private final ReportLog reports = new ReportLog(REPORT_CAPACITY);
    private final FlushLog flushes = new FlushLog(FLUSH_HISTORY);
    private final ContextNames contextNames;
    private final ScratchInts scratch;
    private final TypedProviderCalls typedProvider;
    private final ExposedExtensions exposed;
    private final SubmissionCheckpoints checkpoints;
    private final WrapperState wrapperState;
    private final VanillaAliases aliases;
    private final MixedSections sections;
    private final InterceptedCalls intercepted;
    private long frame;

    public CompatibilityLayer(LayerPorts ports, LayerSettings settings) {
        contextNames = ports.names();
        scratch = new ScratchInts(ports.memory(), SCRATCH_VALUES);
        ProviderCalls provider = new ListCallMarshalling(ports.provider(), ports.memory());
        typedProvider = new TypedProviderCalls(provider, scratch);
        exposed = new ExposedExtensions(ports.memory(), settings.exposedExtensions());
        checkpoints = new SubmissionCheckpoints(ports.handoff(), flushes);

        ErrorLedger errors = new ErrorLedger(typedProvider);
        OperationReporter reporter =
                new OperationReporter(reports, new CallSiteLocator(INFRASTRUCTURE_PACKAGES, ports.origins()));
        OpenConstructs constructs = new OpenConstructs();
        ObjectNames names =
                new ObjectNames(() -> contextNames, typedProvider, errors, () -> frame, settings.profile());
        DisplayListNames listNames = new DisplayListNames(() -> contextNames, typedProvider, errors, () -> frame);
        DisplayListCalls listCalls = new DisplayListCalls(listNames, constructs, checkpoints, provider);
        QuerySpans spans = new QuerySpans(reporter, errors);
        QueryCalls queries = new QueryCalls(spans, names, checkpoints, provider);
        wrapperState = new WrapperState(settings.wrapperState(), provider, reporter);

        FramebufferBindings framebufferBindings =
                new FramebufferBindings(contextNames.table(ObjectKind.FRAMEBUFFER), settings.sceneFramebuffer());
        DefaultFramebufferBuffers defaultBuffers = new DefaultFramebufferBuffers();
        VirtualFramebufferCalls framebuffers = new VirtualFramebufferCalls(
                framebufferBindings, defaultBuffers, names, settings.catalog(), provider, errors, reporter);

        NameTable textures = contextNames.table(ObjectKind.TEXTURE);
        TextureBindings textureBindings = new TextureBindings();
        aliases = new VanillaAliases(textures);
        TextureRedefinition redefinition = new TextureRedefinition(
                textures, aliases, new BoundTextureRedirection(textureBindings, names, typedProvider, provider), typedProvider);
        List<CallObserver> observers = Arrays.asList(
                textureBindings,
                new AliasStorageGuard(textureBindings, aliases, redefinition, provider, reporter),
                wrapperState);

        CallTranslator translator = new CallTranslator(
                settings.catalog(), new NameArguments(names), checkpoints, constructs, observers, provider);
        sections = new MixedSections(wrapperState.reconciler(), checkpoints, spans, constructs, provider, reporter);
        intercepted = InterceptedCalls.builder()
                .context(settings.catalog(), settings.profile())
                .translation(translator, names)
                .displayLists(listNames, listCalls)
                .queries(queries, new BindingQueries(names, framebufferBindings, defaultBuffers, listNames))
                .framebuffers(framebuffers)
                .capabilities(exposed)
                .diagnostics(errors, reporter)
                .provider(provider, ports.memory())
                .build();
    }

    public long call(String function, long[] words) {
        return intercepted.call(function, words);
    }

    public void beginFrame() {
        frame++;
        checkpoints.beginFrame(frame);
    }

    public void beforeNativePass() {
        sections.beforeNativePass();
    }

    public int enterCallback() {
        return sections.enterCallback();
    }

    public List<String> exitCallback() {
        return sections.exitCallback();
    }

    public void present() {
        sections.present();
        for (ObjectKind kind : ObjectKind.values()) {
            contextNames.table(kind).collect(frame, typedProvider);
        }
    }

    public int publishVanillaTexture(long nativeResource, int importedBacking) {
        return aliases.publish(nativeResource, importedBacking, typedProvider);
    }

    public boolean withdrawVanillaTexture(long nativeResource) {
        return aliases.withdraw(nativeResource, typedProvider);
    }

    public WrapperState wrapperState() {
        return wrapperState;
    }

    public ReportLog reports() {
        return reports;
    }

    public FlushLog flushes() {
        return flushes;
    }

    @Override
    public void close() {
        exposed.close();
        scratch.close();
    }
}
