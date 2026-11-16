package io.github.luasgraphicss.minemod.compatibility.interop.negotiation;

public interface ExternalSupportQuery {
    ExternalSupport supportFor(SharingRequest request);

    boolean binarySemaphoresShareable();
}
