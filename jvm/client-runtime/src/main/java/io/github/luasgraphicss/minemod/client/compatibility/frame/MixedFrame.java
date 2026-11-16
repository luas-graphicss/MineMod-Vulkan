package io.github.luasgraphicss.minemod.client.compatibility.frame;

import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import java.util.Objects;

public final class MixedFrame {
    private final CompatibilityLayer layer;
    private boolean isInCallback;

    public MixedFrame(CompatibilityLayer layer) {
        this.layer = Objects.requireNonNull(layer, "layer");
    }

    public void begin() {
        layer.beginFrame();
    }

    public void beforeProviderCall() {
        if (!isInCallback) {
            layer.enterCallback();
            isInCallback = true;
        }
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
