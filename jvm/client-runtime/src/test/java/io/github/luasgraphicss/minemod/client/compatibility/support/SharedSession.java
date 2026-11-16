package io.github.luasgraphicss.minemod.client.compatibility.support;

import io.github.luasgraphicss.minemod.bridge.NativeBridge;
import io.github.luasgraphicss.minemod.bridge.NativeSceneHost;
import io.github.luasgraphicss.minemod.bridge.NativeSceneSession;
import io.github.luasgraphicss.minemod.bridge.SceneSessionSetup;
import io.github.luasgraphicss.minemod.bridge.SceneSharing;
import io.github.luasgraphicss.minemod.client.frame.FrameContent;
import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.contract.capability.Capability;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;

public final class SharedSession implements AutoCloseable {
    public static final String LIBRARY = "minemod.sceneLibrary";
    private static final int STREAM_BYTES = 256;
    private static final int CONTENT_BYTES = 64 * 1024;
    private static final int RGBA_BYTES = 4;
    private static final int CHANNEL_MASK = 0xFF;
    private static final float CHANNEL_MAXIMUM = 255.0F;

    private final NativeSceneHost host;
    private final NativeSceneSession session;
    private int width;
    private int height;
    private int[] backdrop;
    private boolean hasBegun;

    private SharedSession(NativeSceneHost host, NativeSceneSession session, int width, int height) {
        this.host = host;
        this.session = session;
        this.width = width;
        this.height = height;
    }

    public static SharedSession open(int width, int height) {
        NativeBridge bridge =
                NativeBridge.load(Paths.get(System.getProperty(LIBRARY)), EnumSet.of(Capability.COMMAND_STREAM));
        NativeSceneHost host = bridge.openSceneHost(true, Collections.<String>emptyList());
        try {
            NativeSceneSession session =
                    host.openSession(SceneSessionSetup.offscreen(width, height).sharingAttachments());
            return new SharedSession(host, session, width, height);
        } catch (RuntimeException failure) {
            host.close();
            throw failure;
        }
    }

    public SceneSharing sharing() {
        return session.sharing();
    }

    public void recordFrameOver(int[] backdropColor) {
        backdrop = backdropColor.clone();
        hasBegun = false;
    }

    public boolean submitRecordedPart() {
        if (backdrop == null) {
            return false;
        }
        if (!hasBegun) {
            CommandStreamWriter stream = new CommandStreamWriter(ByteBuffer.allocateDirect(STREAM_BYTES));
            new SceneCommandWriter(stream)
                    .beginInterfaceFrame(
                            backdrop[0] / CHANNEL_MAXIMUM,
                            backdrop[1] / CHANNEL_MAXIMUM,
                            backdrop[2] / CHANNEL_MAXIMUM,
                            backdrop[3] / CHANNEL_MAXIMUM);
            submit(stream);
            hasBegun = true;
        }
        return true;
    }

    public void endFrame() {
        submitRecordedPart();
        CommandStreamWriter stream = new CommandStreamWriter(ByteBuffer.allocateDirect(STREAM_BYTES));
        new SceneCommandWriter(stream).endFrame();
        submit(stream);
        backdrop = null;
    }

    public boolean submitPart(FrameContent content) {
        CommandStreamWriter stream = new CommandStreamWriter(ByteBuffer.allocateDirect(CONTENT_BYTES));
        content.writePart(new SceneCommandWriter(stream));
        submit(stream);
        return true;
    }

    public void present(FrameContent content) {
        CommandStreamWriter stream = new CommandStreamWriter(ByteBuffer.allocateDirect(CONTENT_BYTES));
        content.write(new SceneCommandWriter(stream));
        submit(stream);
    }

    public int width() {
        return width;
    }

    public void resize(int newWidth, int newHeight) {
        session.resize(newWidth, newHeight);
        width = newWidth;
        height = newHeight;
    }

    public int[] pixelAt(int x, int rowFromTheTop) {
        ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * RGBA_BYTES);
        session.readFrame(pixels);
        int first = (rowFromTheTop * width + x) * RGBA_BYTES;
        return new int[] {
            pixels.get(first) & CHANNEL_MASK,
            pixels.get(first + 1) & CHANNEL_MASK,
            pixels.get(first + 2) & CHANNEL_MASK,
            pixels.get(first + 3) & CHANNEL_MASK
        };
    }

    public long framesDrawn() {
        return session.counters().framesDrawn();
    }

    public String validationText() {
        return host.validationText();
    }

    @Override
    public void close() {
        session.close();
        host.close();
    }

    private void submit(CommandStreamWriter stream) {
        StreamReport report = session.submit(stream.finish());
        if (!report.isAccepted()) {
            throw new IllegalStateException("the session refused a stream of the test: " + report);
        }
    }
}
