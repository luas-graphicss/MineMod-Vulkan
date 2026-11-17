package io.github.luasgraphicss.minemod.client.compatibility.sharing;

import io.github.luasgraphicss.minemod.bridge.sharing.HandoffSynchronization;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachments;
import io.github.luasgraphicss.minemod.compatibility.interop.device.DeviceIdentity;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingNegotiator;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingOutcome;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;
import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class SceneNegotiation {
    private SceneNegotiation() {}

    public static SharingPlan negotiate(
            SharedAttachments shared,
            DeviceIdentity provider,
            Collection<String> providerExtensions,
            HandlePlatform platform) {
        DeviceIdentity renderer = new DeviceIdentity(shared.deviceUuid(), shared.driverUuid());
        List<SharingRequest> requests = Arrays.asList(
                AttachmentRequests.requestFor(SharedAttachment.COLOR, shared.attachment(SharedAttachment.COLOR)),
                AttachmentRequests.requestFor(
                        SharedAttachment.DEPTH_STENCIL, shared.attachment(SharedAttachment.DEPTH_STENCIL)));
        SessionSupport support = new SessionSupport(shared);
        SharingOutcome signaled =
                SharingNegotiator.negotiate(renderer, provider, providerExtensions, platform, requests, support);
        if (signaled.isAccepted()) {
            return planOf(signaled.shares(), HandoffSynchronization.SEMAPHORES, platform);
        }
        SharingOutcome finished =
                SharingNegotiator.negotiateMemory(renderer, provider, providerExtensions, platform, requests, support);
        if (finished.isAccepted()) {
            return planOf(finished.shares(), HandoffSynchronization.BLOCKING, platform);
        }
        throw new SharingRefused(finished.rejections());
    }

    private static SharingPlan planOf(
            List<PlannedShare> shares, HandoffSynchronization synchronization, HandlePlatform platform) {
        return new SharingPlan(
                shares.get(SharedAttachment.COLOR.code()),
                shares.get(SharedAttachment.DEPTH_STENCIL.code()),
                synchronization,
                platform);
    }
}
