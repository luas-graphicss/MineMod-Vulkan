package io.github.luasgraphicss.minemod.client.session;

import io.github.luasgraphicss.minemod.bridge.NativeBridge;
import io.github.luasgraphicss.minemod.bridge.NativeSceneHost;
import io.github.luasgraphicss.minemod.bridge.NativeSceneSession;
import io.github.luasgraphicss.minemod.bridge.PresentPacing;
import io.github.luasgraphicss.minemod.bridge.SceneSessionSetup;
import io.github.luasgraphicss.minemod.client.diagnostics.RendererDiagnostics;
import io.github.luasgraphicss.minemod.client.failure.RendererFailure;
import io.github.luasgraphicss.minemod.client.frame.FrameContent;
import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.client.lightmap.LightmapPublisher;
import io.github.luasgraphicss.minemod.client.presentation.PresentationControl;
import io.github.luasgraphicss.minemod.client.reload.ResourceReload;
import io.github.luasgraphicss.minemod.client.reload.SessionProgramStaging;
import io.github.luasgraphicss.minemod.client.shaders.NativeShaderCompiler;
import io.github.luasgraphicss.minemod.client.shaders.ShaderCompiler;
import io.github.luasgraphicss.minemod.client.terrain.TerrainPublisher;
import io.github.luasgraphicss.minemod.client.window.FramebufferSize;
import io.github.luasgraphicss.minemod.client.window.WindowSurface;
import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.contract.abi.AbiNegotiation;
import io.github.luasgraphicss.minemod.contract.capability.Capability;
import io.github.luasgraphicss.minemod.contract.handle.HandleAllocator;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class ClientRenderer implements AutoCloseable {
    private static final Set<Capability> REQUIRED_CAPABILITIES =
            Collections.unmodifiableSet(EnumSet.of(Capability.COMMAND_STREAM));
    private static final int MAX_TEXTURES = 4096;
    private static final int MAX_SECTIONS = 65536;
    private static final int RESOURCE_SHARE_OF_STREAM = 2;
    private static final int SECTION_SHARE_OF_STREAM = 4;
    private static final String SHADER_SCRATCH_DIRECTORY = "shader-scratch";

    private final NativeSceneHost host;
    private final NativeSceneSession session;
    private final PresentationControl presentation;
    private final ByteBuffer streamStorage;
    private final long resourceBytesPerStream;
    private final long resourceRecordsPerStream;
    private final HandleAllocator textures = new HandleAllocator(ResourceKind.TEXTURE, MAX_TEXTURES);
    private final LightmapPublisher lightmap = new LightmapPublisher(textures.allocate());
    private final TerrainPublisher terrain;
    private final ResourceReload reload;
    private final RendererDiagnostics diagnostics;

    private ClientRenderer(
            AbiNegotiation negotiation,
            NativeSceneHost host,
            NativeSceneSession session,
            PresentationControl presentation,
            ShaderCompiler shaders) {
        long streamBytes = Math.min(negotiation.maxStreamBytes(), Integer.MAX_VALUE);
        this.host = host;
        this.session = session;
        this.presentation = presentation;
        this.streamStorage = ByteBuffer.allocateDirect((int) streamBytes);
        this.resourceBytesPerStream = streamBytes / RESOURCE_SHARE_OF_STREAM;
        this.resourceRecordsPerStream = negotiation.maxStreamCommands() / RESOURCE_SHARE_OF_STREAM;
        this.terrain = new TerrainPublisher(streamBytes / SECTION_SHARE_OF_STREAM, MAX_SECTIONS);
        this.reload = new ResourceReload(new SessionProgramStaging(session.reload()), shaders);
        this.diagnostics = new RendererDiagnostics(session.diagnostics(), reload);
    }

    public static ClientRenderer start(Path nativeLibrary, WindowSurface window, DeviceValidation validation) {
        NativeBridge bridge = NativeBridge.load(nativeLibrary, REQUIRED_CAPABILITIES);
        NativeSceneHost host =
                bridge.openSceneHost(validation == DeviceValidation.ENABLED, window.requiredInstanceExtensions());
        try {
            FramebufferSize windowSize = window.framebufferSize();
            FramebufferSize openedSize =
                    new FramebufferSize(Math.max(1, windowSize.width()), Math.max(1, windowSize.height()));
            NativeSceneSession session =
                    host.openSession(SceneSessionSetup.offscreen(openedSize.width(), openedSize.height())
                            .presentingOn(window.createSurface(host.vulkanInstance()))
                            .withLimits(MAX_TEXTURES, MAX_SECTIONS));
            PresentationControl presentation =
                    new PresentationControl(new SessionPresentation(session), window, openedSize);
            ShaderCompiler shaders =
                    NativeShaderCompiler.open(nativeLibrary.toAbsolutePath().resolveSibling(SHADER_SCRATCH_DIRECTORY));
            return new ClientRenderer(bridge.negotiation(), host, session, presentation, shaders);
        } catch (RuntimeException failure) {
            host.close();
            throw failure;
        }
    }

    public LightmapPublisher lightmap() {
        return lightmap;
    }

    public TerrainPublisher terrain() {
        return terrain;
    }

    public ResourceReload reload() {
        return reload;
    }

    public RendererDiagnostics diagnostics() {
        return diagnostics;
    }

    public void pace(PresentPacing pacing) {
        presentation.pace(pacing);
    }

    public void presentFrame(FrameContent content) {
        boolean isDrawable = presentation.beginFrame();
        CommandStreamWriter stream = new CommandStreamWriter(streamStorage);
        SceneCommandWriter scene = new SceneCommandWriter(stream);
        StreamBudget resources = new StreamBudget(resourceBytesPerStream, resourceRecordsPerStream);
        lightmap.publish(scene, resources);
        terrain.publish(scene, resources);
        if (!isDrawable && !resources.hasTaken()) {
            return;
        }
        if (isDrawable) {
            content.write(scene);
        }
        StreamReport report = session.submit(stream.finish());
        if (!report.isAccepted()) {
            throw new RendererFailure("the native renderer refused the frame stream: " + report);
        }
        if (isDrawable) {
            presentation.endFrame();
        }
    }

    @Override
    public void close() {
        reload.close();
        session.close();
        host.close();
    }
}
