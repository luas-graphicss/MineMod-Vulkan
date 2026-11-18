package io.github.luasgraphicss.minemod.verification.scenario.simulation;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallDescription;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.Checkpoint;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.NameArgument;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.state.error.ProviderErrors;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentOwner;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.ProviderLists;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderObjects;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class SimulatedProvider implements ProviderCalls, ProviderObjects, ProviderLists, ProviderErrors {
    public static final int NO_ERROR = 0;
    public static final long INVALID_CAPABILITY = 0xFFFF;

    private static final int NO_OBJECT = 0;
    private static final int GL_INVALID_ENUM = 0x0500;
    private static final int GL_INVALID_VALUE = 0x0501;
    private static final int GL_INVALID_OPERATION = 0x0502;
    private static final int GL_COMPILE = 0x1300;
    private static final String ENABLE = "glEnable";
    private static final String DISABLE = "glDisable";
    private static final String NEW_LIST = "glNewList";
    private static final String END_LIST = "glEndList";
    private static final String CALL_LISTS = "glCallLists";
    private static final String READ_PIXELS = "glReadPixels";
    private static final String NO_OBJECT_LABEL = "none";
    private static final String DEFAULT_FRAMEBUFFER_LABEL = "default";
    private static final Set<ObjectKind> CREATED_BY_FIRST_USE = EnumSet.of(
            ObjectKind.TEXTURE,
            ObjectKind.BUFFER,
            ObjectKind.RENDERBUFFER,
            ObjectKind.FRAMEBUFFER,
            ObjectKind.DISPLAY_LIST);

    private final CallCatalog catalog = CallCatalog.standard();
    private final ContextProfile profile;
    private final Timeline timeline;
    private final Supplier<AttachmentOwner> attachmentOwner;
    private final Map<ObjectKind, Map<Integer, String>> liveObjects = new EnumMap<>(ObjectKind.class);
    private final Map<ObjectKind, Integer> createdCounts = new EnumMap<>(ObjectKind.class);
    private final Set<Long> enabledCapabilities = new HashSet<>();
    private final List<String> ownershipViolations = new ArrayList<>();
    private final ScriptedAnswers answers = new ScriptedAnswers();
    private final PixelPacking packing = new PixelPacking();
    private int sceneFramebuffer = NO_OBJECT;
    private int pendingError = NO_ERROR;
    private boolean recordingList;

    public SimulatedProvider(ContextProfile profile, Timeline timeline, Supplier<AttachmentOwner> attachmentOwner) {
        this.profile = profile;
        this.timeline = timeline;
        this.attachmentOwner = attachmentOwner;
    }

    public void answer(String function, long... values) {
        answers.script(function, values);
    }

    @Override
    public long invoke(String function, long[] arguments) {
        boolean capabilityChange = function.equals(ENABLE) || function.equals(DISABLE);
        if (capabilityChange && arguments[0] == INVALID_CAPABILITY) {
            raise(GL_INVALID_ENUM);
            return 0;
        }
        Optional<CallDescription> description = catalog.describe(function);
        Optional<String[]> shown = describeArguments(function, description, arguments);
        if (shown.isEmpty()) {
            return 0;
        }
        followState(function, arguments);
        packing.follow(function, arguments);
        if (touchesAttachments(description) && attachmentOwner.get() != AttachmentOwner.PROVIDER) {
            ownershipViolations.add(function + " ran while the renderer owned the attachments");
        }
        timeline.add(Timeline.Source.PROVIDER, function + "(" + String.join(", ", shown.get()) + ")");
        return function.equals(READ_PIXELS) ? packing.bytesWritten(arguments) : answers.next(function);
    }

    @Override
    public int create(ObjectKind kind, int parameter) {
        int object = 1;
        while (isTaken(kind, object)) {
            object++;
        }
        register(kind, object);
        return object;
    }

    @Override
    public void dispose(ObjectKind kind, int backing) {
        objectsOf(kind).remove(backing);
    }

    public void deleteNamed(ObjectKind kind, int object) {
        if (isLive(kind, object)) {
            dispose(kind, object);
        } else if (object != NO_OBJECT && kind == ObjectKind.SHADER_OR_PROGRAM) {
            raise(GL_INVALID_VALUE);
        }
    }

    @Override
    public int generate(int range) {
        int first = 1;
        int free = 0;
        while (free < range) {
            if (isTaken(ObjectKind.DISPLAY_LIST, first + free)) {
                first += free + 1;
                free = 0;
            } else {
                free++;
            }
        }
        for (int offset = 0; offset < range; offset++) {
            register(ObjectKind.DISPLAY_LIST, first + offset);
        }
        return first;
    }

    @Override
    public void delete(int firstBacking, int range) {
        for (int offset = 0; offset < range; offset++) {
            dispose(ObjectKind.DISPLAY_LIST, firstBacking + offset);
        }
    }

    @Override
    public int takeError() {
        int error = pendingError;
        pendingError = NO_ERROR;
        return error;
    }

    public int adoptSceneFramebuffer() {
        sceneFramebuffer = 1;
        while (objectsOf(ObjectKind.FRAMEBUFFER).containsKey(sceneFramebuffer)) {
            sceneFramebuffer++;
        }
        return sceneFramebuffer;
    }

    public boolean isLive(ObjectKind kind, int object) {
        return objectsOf(kind).containsKey(object);
    }

    public boolean isEnabled(long capability) {
        return enabledCapabilities.contains(capability);
    }

    public List<String> ownershipViolations() {
        return List.copyOf(ownershipViolations);
    }

    private Optional<String[]> describeArguments(
            String function, Optional<CallDescription> description, long[] arguments) {
        String[] shown = new String[arguments.length];
        for (int index = 0; index < arguments.length; index++) {
            shown[index] = function.equals(CALL_LISTS)
                    ? objectsOf(ObjectKind.DISPLAY_LIST).getOrDefault((int) arguments[index], NO_OBJECT_LABEL)
                    : Long.toString(arguments[index]);
        }
        for (NameArgument argument : description.map(CallDescription::nameArguments).orElse(List.of())) {
            Optional<String> label = labelForUse(argument.kind(), (int) arguments[argument.index()]);
            if (label.isEmpty()) {
                raise(argument.kind() == ObjectKind.SHADER_OR_PROGRAM ? GL_INVALID_VALUE : GL_INVALID_OPERATION);
                return Optional.empty();
            }
            shown[argument.index()] = label.get();
        }
        return Optional.of(shown);
    }

    private Optional<String> labelForUse(ObjectKind kind, int object) {
        if (kind == ObjectKind.FRAMEBUFFER && (object == NO_OBJECT || object == sceneFramebuffer)) {
            return Optional.of(DEFAULT_FRAMEBUFFER_LABEL);
        }
        if (object == NO_OBJECT) {
            return Optional.of(NO_OBJECT_LABEL);
        }
        String label = objectsOf(kind).get(object);
        if (label != null) {
            return Optional.of(label);
        }
        if (profile == ContextProfile.COMPATIBILITY && CREATED_BY_FIRST_USE.contains(kind)) {
            return Optional.of(register(kind, object));
        }
        return Optional.empty();
    }

    private boolean isTaken(ObjectKind kind, int object) {
        return objectsOf(kind).containsKey(object) || (kind == ObjectKind.FRAMEBUFFER && object == sceneFramebuffer);
    }

    private String register(ObjectKind kind, int object) {
        String label = kind + "#" + createdCounts.merge(kind, 1, Integer::sum);
        objectsOf(kind).put(object, label);
        return label;
    }

    private Map<Integer, String> objectsOf(ObjectKind kind) {
        return liveObjects.computeIfAbsent(kind, unused -> new HashMap<>());
    }

    private void raise(int error) {
        if (pendingError == NO_ERROR) {
            pendingError = error;
        }
    }

    private void followState(String function, long[] arguments) {
        switch (function) {
            case ENABLE -> enabledCapabilities.add(arguments[0]);
            case DISABLE -> enabledCapabilities.remove(arguments[0]);
            case NEW_LIST -> recordingList = arguments[1] == GL_COMPILE;
            case END_LIST -> recordingList = false;
            default -> { }
        }
    }

    private boolean touchesAttachments(Optional<CallDescription> description) {
        return !recordingList
                && description.map(CallDescription::checkpoint).orElse(Checkpoint.NONE) != Checkpoint.NONE;
    }
}
