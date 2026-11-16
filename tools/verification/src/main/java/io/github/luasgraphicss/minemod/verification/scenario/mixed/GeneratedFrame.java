package io.github.luasgraphicss.minemod.verification.scenario.mixed;

import io.github.luasgraphicss.minemod.compatibility.state.reconcile.LeftoverPolicy;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentOwner;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedLayer;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.TrackedState;
import java.util.List;
import java.util.Optional;
import java.util.Random;

final class GeneratedFrame {
    private static final int STEP_KINDS = 6;
    private static final long GL_TRIANGLES = 0x0004;
    private static final long GL_COLOR_BUFFER_BIT = 0x4000;
    private static final long GL_RGBA = 0x1908;
    private static final long GL_UNSIGNED_BYTE = 0x1401;
    private static final TrackedState[] STATES = TrackedState.values();

    private final SimulatedLayer layer;
    private final Random random;
    private boolean inCallback;
    private String failure;

    GeneratedFrame(SimulatedLayer layer, Random random) {
        this.layer = layer;
        this.random = random;
    }

    Optional<String> play(int steps) {
        layer.beginFrame();
        for (int step = 0; step < steps && failure == null; step++) {
            playStep();
            if (failure == null && !layer.ownershipViolations().isEmpty()) {
                failure = layer.ownershipViolations().get(0);
            }
            if (failure != null) {
                failure = "step " + step + ": " + failure;
            }
        }
        if (failure == null) {
            finishFrame();
        }
        return Optional.ofNullable(failure);
    }

    private void playStep() {
        switch (random.nextInt(STEP_KINDS)) {
            case 0 -> toggleCallback();
            case 1 -> changeThroughWrapper();
            case 2 -> {
                if (inCallback) {
                    changeDirectly();
                } else {
                    changeThroughWrapper();
                }
            }
            case 3 -> drawOrRunNativePass("glDrawArrays", GL_TRIANGLES, 0, 3);
            case 4 -> drawOrRunNativePass("glReadPixels", 0, 0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, 0);
            default -> drawOrRunNativePass("glClear", GL_COLOR_BUFFER_BIT);
        }
    }

    private void toggleCallback() {
        if (inCallback) {
            leaveCallback();
            return;
        }
        layer.enterCallback();
        inCallback = true;
        for (TrackedState state : STATES) {
            if (providerValue(state) != layer.wrapperValue(state)) {
                failure = state + " was not materialised when the callback began";
            }
        }
    }

    private void leaveCallback() {
        long[] expected = new long[STATES.length];
        for (TrackedState state : STATES) {
            expected[state.ordinal()] =
                    state.policy() == LeftoverPolicy.ADOPT ? providerValue(state) : layer.wrapperValue(state);
        }
        List<String> leftOpen = layer.exitCallback();
        inCallback = false;
        if (!leftOpen.isEmpty()) {
            failure = "a balanced callback was reported as leaving " + leftOpen;
        }
        for (TrackedState state : STATES) {
            if (layer.wrapperValue(state) != expected[state.ordinal()]) {
                failure = state + " did not follow its leftover policy when the callback ended";
            }
        }
    }

    private void changeThroughWrapper() {
        TrackedState state = STATES[random.nextInt(STATES.length)];
        long value = random.nextBoolean() ? TrackedState.ENABLED : TrackedState.DISABLED;
        layer.setThroughWrapper(state, value);
        if (inCallback && providerValue(state) != value) {
            failure = state + " set through the wrapper did not reach the provider inside the callback";
        }
    }

    private void changeDirectly() {
        TrackedState state = STATES[random.nextInt(STATES.length)];
        layer.call(random.nextBoolean() ? "glEnable" : "glDisable", state.capability());
    }

    private void drawOrRunNativePass(String function, long... arguments) {
        if (inCallback) {
            layer.call(function, arguments);
        } else {
            layer.nativePass("native pass");
        }
    }

    private void finishFrame() {
        if (inCallback) {
            leaveCallback();
        }
        layer.present();
        if (failure != null) {
            return;
        }
        if (layer.attachmentOwner() != AttachmentOwner.RENDERER) {
            failure = "the frame was presented while the provider owned the attachments";
        } else if (layer.handoff().hasPendingSignal()) {
            failure = "a semaphore signal was left pending after the frame";
        } else if (!layer.ownershipViolations().isEmpty()) {
            failure = layer.ownershipViolations().get(0);
        }
    }

    private long providerValue(TrackedState state) {
        return layer.provider().isEnabled(state.capability()) ? TrackedState.ENABLED : TrackedState.DISABLED;
    }
}
