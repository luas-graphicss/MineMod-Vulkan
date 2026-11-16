package io.github.luasgraphicss.minemod.compatibility.interop.negotiation;

import io.github.luasgraphicss.minemod.compatibility.interop.device.DeviceIdentity;
import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SharingNegotiator {
    private SharingNegotiator() {
    }

    public static SharingOutcome negotiate(
            DeviceIdentity renderer,
            DeviceIdentity provider,
            Collection<String> providerExtensions,
            HandlePlatform platform,
            List<SharingRequest> requests,
            ExternalSupportQuery query) {
        List<String> rejections = new ArrayList<>();
        if (!renderer.isSameGpuAs(provider)) {
            rejections.add("the provider runs on a different GPU or driver than the renderer");
        }
        for (String extension : platform.requiredExtensions()) {
            if (!providerExtensions.contains(extension)) {
                rejections.add("the provider lacks " + extension);
            }
        }
        if (!query.binarySemaphoresShareable()) {
            rejections.add("binary semaphores cannot be shared on this device");
        }
        List<PlannedShare> shares = new ArrayList<>();
        for (SharingRequest request : requests) {
            ExternalSupport support = query.supportFor(request);
            String rejection = rejectionOf(request, support);
            if (rejection != null) {
                rejections.add(rejection);
            } else {
                shares.add(new PlannedShare(request, support.dedicatedAllocationRequired()));
            }
        }
        return rejections.isEmpty() ? SharingOutcome.accepted(shares) : SharingOutcome.rejected(rejections);
    }

    private static String rejectionOf(SharingRequest request, ExternalSupport support) {
        if (!support.isShareable()) {
            return request + " cannot be exported and imported";
        }
        if (!support.fits(request)) {
            return request + " exceeds the shareable size";
        }
        if (!support.supportsSamples(request.samples())) {
            return request + " cannot be shared with that sample count";
        }
        return null;
    }
}
