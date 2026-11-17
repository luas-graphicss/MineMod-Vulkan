package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.renderer;

import io.github.luasgraphicss.minemod.compatibility.interop.device.DeviceIdentity;
import io.github.luasgraphicss.minemod.compatibility.interop.handle.HandleCloser;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ExternalSupport;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ExternalSupportQuery;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ShareableResource;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;
import java.nio.file.Paths;
import java.util.Arrays;

public final class RendererStandIn implements ExternalSupportQuery, AutoCloseable {
    public static final String LIBRARY = "minemod.sceneHost";
    private static final int SINGLE_SAMPLE = 1;
    private static final int EXPORTABLE = 0;
    private static final int MAX_WIDTH = 1;
    private static final int MAX_HEIGHT = 2;
    private static final int MAX_MIP_LEVELS = 3;

    private final long host;

    private RendererStandIn(long host) {
        this.host = host;
    }

    public static RendererStandIn open() {
        StandInNatives.load(Paths.get(System.getProperty(LIBRARY)));
        return new RendererStandIn(StandInNatives.open());
    }

    public static HandleCloser descriptors() {
        return descriptor -> StandInNatives.closeDescriptor((int) descriptor);
    }

    public DeviceIdentity identity() {
        byte[] uuids = StandInNatives.deviceIdentity(host);
        return new DeviceIdentity(
                Arrays.copyOfRange(uuids, 0, DeviceIdentity.UUID_BYTES),
                Arrays.copyOfRange(uuids, DeviceIdentity.UUID_BYTES, 2 * DeviceIdentity.UUID_BYTES));
    }

    public StandInAttachment createAttachment(SharingRequest request) {
        if (request.mipLevels() != 1 || request.samples() != SINGLE_SAMPLE) {
            throw new IllegalArgumentException("the stand-in shares attachments of one level and one sample");
        }
        int attachment = StandInNatives.createAttachment(
                host, isColor(request), request.format(), (int) request.width(), request.height());
        return new StandInAttachment(host, attachment, request);
    }

    public String validationText() {
        return StandInNatives.validationText(host);
    }

    @Override
    public ExternalSupport supportFor(SharingRequest request) {
        long[] support = StandInNatives.attachmentSupport(host, isColor(request), request.format());
        if (support[EXPORTABLE] == 0) {
            return ExternalSupport.unsupported();
        }
        return ExternalSupport.shareable(
                true, support[MAX_WIDTH], (int) support[MAX_HEIGHT], (int) support[MAX_MIP_LEVELS], SINGLE_SAMPLE);
    }

    @Override
    public boolean binarySemaphoresShareable() {
        return StandInNatives.semaphoresShareable(host);
    }

    @Override
    public void close() {
        StandInNatives.close(host);
    }

    private static boolean isColor(SharingRequest request) {
        if (request.resource() == ShareableResource.COLOR_ATTACHMENT) {
            return true;
        }
        if (request.resource() == ShareableResource.DEPTH_STENCIL_ATTACHMENT) {
            return false;
        }
        throw new IllegalArgumentException("the stand-in shares scene attachments, not " + request);
    }
}
