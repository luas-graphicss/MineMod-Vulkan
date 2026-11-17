package io.github.luasgraphicss.minemod.client.compatibility.frame;

import io.github.luasgraphicss.minemod.client.compatibility.state.WrapperCalls;
import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import java.util.Objects;

public final class MixedFrame {
    private final CompatibilityLayer layer;
    private final WrapperCalls wrapperCalls;
    private boolean isInCallback;

    public MixedFrame(CompatibilityLayer layer, WrapperCalls wrapperCalls) {
        this.layer = Objects.requireNonNull(layer, "layer");
        this.wrapperCalls = Objects.requireNonNull(wrapperCalls, "wrapperCalls");
    }

    public void begin() {
        layer.beginFrame();
    }

    public void beforeProviderCall() {
        if (!isInCallback) {
            layer.enterCallback();
            isInCallback = true;
        }
        wrapperCalls.replayInto(layer::call);
    }

    public void beforeNativeWork() {
        if (isInCallback) {
            leaveCallback();
            layer.beforeNativePass();
        }
    }

    public void present() {
        if (isInCallback) {
            leaveCallback();
        }
        layer.present();
    }

    private void leaveCallback() {
        layer.exitCallback();
        isInCallback = false;
    }
}
