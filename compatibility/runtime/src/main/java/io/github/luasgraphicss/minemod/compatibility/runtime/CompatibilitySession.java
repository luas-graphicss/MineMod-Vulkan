package io.github.luasgraphicss.minemod.compatibility.runtime;

import io.github.luasgraphicss.minemod.compatibility.contexts.CompatibilityContext;
import io.github.luasgraphicss.minemod.compatibility.contexts.ContextRegistry;
import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.exposure.ExtensionExposure;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.DispatchInstaller;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.DispatchTable;
import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.layer.LayerPorts;
import io.github.luasgraphicss.minemod.compatibility.layer.LayerSettings;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.layer.provider.ProviderExtensions;
import io.github.luasgraphicss.minemod.compatibility.layer.scene.SceneFramebuffer;
import io.github.luasgraphicss.minemod.compatibility.natives.gate.Thunks;
import io.github.luasgraphicss.minemod.compatibility.natives.memory.JniMemory;
import io.github.luasgraphicss.minemod.compatibility.natives.provider.BundleOpener;
import io.github.luasgraphicss.minemod.compatibility.natives.provider.NativeProvider;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.CapabilityRequirements;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ProviderCapabilities;
import io.github.luasgraphicss.minemod.compatibility.provider.diagnosis.ProviderDiagnosis;
import io.github.luasgraphicss.minemod.compatibility.provider.diagnosis.ProviderInspector;
import io.github.luasgraphicss.minemod.compatibility.provider.identity.ProviderIdentityParser;
import io.github.luasgraphicss.minemod.compatibility.provider.load.IsolatedProviderLoader;
import io.github.luasgraphicss.minemod.compatibility.provider.load.LoadedProvider;
import io.github.luasgraphicss.minemod.compatibility.provider.load.ProviderLoadException;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderBundle;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderRejectedException;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneTextures;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.io.IOException;
import java.util.Set;

public final class CompatibilitySession implements AutoCloseable {
    private static final int NAMES_PER_KIND = 1 << 16;
    private final LoadedProvider loaded;
    private final ProviderCalls calls;
    private final JniMemory memory;
    private final ContextRegistry contexts;
    private final ContextProfile profile;
    private final DispatchTable dispatch;
    private final Set<String> exposedExtensions;
    private final ProviderDiagnosis diagnosis;

    private CompatibilitySession(
            LoadedProvider loaded,
            ProviderCalls calls,
            JniMemory memory,
            ContextRegistry contexts,
            ContextProfile profile,
            DispatchTable dispatch,
            Set<String> exposedExtensions,
            ProviderDiagnosis diagnosis) {
        this.loaded = loaded;
        this.calls = calls;
        this.memory = memory;
        this.contexts = contexts;
        this.profile = profile;
        this.dispatch = dispatch;
        this.exposedExtensions = exposedExtensions;
        this.diagnosis = diagnosis;
    }

    public static CompatibilitySession open(
            ProviderBundle bundle, CapabilityRequirements requirements, int majorVersion, int minorVersion)
            throws IOException, ProviderLoadException, ProviderRejectedException {
        IsolatedProviderLoader loader = new IsolatedProviderLoader(
                bundle.directory(), bundle.pin(), new BundleOpener(bundle.dependencies(), bundle.driverSettings()));
        LoadedProvider loaded = loader.load(bundle.entryLibrary());
        try {
            NativeProvider provider = new NativeProvider(loaded);
            ProviderCalls calls = provider.calls();
            JniMemory memory = new JniMemory();
            ContextRegistry contexts = new ContextRegistry(
                    provider.contexts(requirements.profile(), majorVersion, minorVersion), NAMES_PER_KIND);
            ProviderCapabilities capabilities = probe(contexts, calls, memory);
            ProviderDiagnosis diagnosis = ProviderInspector.inspect(capabilities, bundle.pin(), requirements);
            if (!diagnosis.isAccepted()) {
                throw new ProviderRejectedException(diagnosis);
            }
            CallCatalog catalog = CallCatalog.standard();
            DispatchTable dispatch = new DispatchTable(
                    catalog, requirements.profile(), provider.functions(), provider.image(), new Thunks());
            Set<String> exposed = ExtensionExposure.exposed(capabilities.extensions(), catalog.extensions(), dispatch);
            return new CompatibilitySession(
                    loaded, calls, memory, contexts, requirements.profile(), dispatch, exposed, diagnosis);
        } catch (RuntimeException | ProviderRejectedException failure) {
            loaded.close();
            throw failure;
        }
    }

    public SessionContext createContext(ContextPlan plan) {
        return enter(contexts.create(), plan);
    }

    public SessionContext createContextSharingWith(SessionContext existing, ContextPlan plan) {
        return enter(contexts.createSharingWith(existing.context()), plan);
    }

    public void install(DispatchInstaller installer, BindingGeneration generation) {
        installer.install(generation, dispatch);
    }

    public DispatchTable dispatch() {
        return dispatch;
    }

    public Set<String> exposedExtensions() {
        return exposedExtensions;
    }

    public ProviderDiagnosis diagnosis() {
        return diagnosis;
    }

    public ContextRegistry contexts() {
        return contexts;
    }

    @Override
    public void close() {
        contexts.shutdown();
        loaded.close();
    }

    private SessionContext enter(CompatibilityContext context, ContextPlan plan) {
        contexts.makeCurrent(context);
        try {
            SceneTarget scene;
            try (ScratchInts scratch = new ScratchInts(memory, 1)) {
                SceneTextures textures = plan.scene().attach(calls, scratch);
                scene = new SceneTarget(
                        calls,
                        memory,
                        SceneFramebuffer.assemble(calls, scratch, textures.color(), textures.depthStencil()));
            }
            CompatibilityLayer layer = new CompatibilityLayer(
                    new LayerPorts(calls, memory, context.names(), plan.handoff().apply(calls), plan.origins()),
                    new LayerSettings(
                            CallCatalog.standard(), profile, exposedExtensions, plan.wrapperState(), scene.framebuffer()));
            SessionContext entered = new SessionContext(contexts, context, layer, scene);
            entered.makeCurrent();
            return entered;
        } catch (RuntimeException failure) {
            contexts.destroy(context);
            throw failure;
        }
    }

    private static ProviderCapabilities probe(ContextRegistry contexts, ProviderCalls calls, JniMemory memory) {
        CompatibilityContext context = contexts.create();
        contexts.makeCurrent(context);
        try {
            return new ProviderCapabilities(
                    ProviderIdentityParser.parse(ProviderExtensions.strings(calls, memory)),
                    ProviderExtensions.list(calls, memory));
        } finally {
            contexts.destroy(context);
        }
    }
}
