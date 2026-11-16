package io.github.luasgraphicss.minemod.compatibility.contexts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ContextLifecycleCyclesTest {
    private static final long SEED = 20261116L;
    private static final int CYCLES = 1_000;
    private static final int MAX_SHARING_CONTEXTS = 3;

    private final RecordingBackend backend = new RecordingBackend();
    private final ContextRegistry registry = new ContextRegistry(backend, 32);
    private final Random random = new Random(SEED);
    private int disposedCount;
    private int nextBacking = 1;

    @Test
    void repeatedLifecyclesLeaveNoContextsGroupsOrObjects() {
        for (int cycle = 0; cycle < CYCLES; cycle++) {
            List<CompatibilityContext> contexts = createShareGroup();
            populate(contexts);
            reloadSharedTexture(contexts.get(0), cycle);
            Collections.shuffle(contexts, random);
            for (CompatibilityContext context : contexts) {
                registry.destroy(context);
            }

            String where = "seed " + SEED + ", cycle " + cycle;
            assertEquals(0, registry.liveContextCount(), where);
            assertEquals(0, registry.liveShareGroupCount(), where);
            assertEquals(0, registry.liveObjectCount(), where);
            assertEquals(0, backend.liveCount(), where);
            assertEquals(cycle + 1, disposedCount, where);
        }
    }

    private List<CompatibilityContext> createShareGroup() {
        List<CompatibilityContext> contexts = new ArrayList<>();
        contexts.add(registry.create());
        int sharingContexts = random.nextInt(MAX_SHARING_CONTEXTS + 1);
        for (int index = 0; index < sharingContexts; index++) {
            contexts.add(registry.createSharingWith(contexts.get(random.nextInt(contexts.size()))));
        }
        return contexts;
    }

    private void populate(List<CompatibilityContext> contexts) {
        for (CompatibilityContext context : contexts) {
            registry.makeCurrent(context);
            context.names().table(ObjectKind.BUFFER).allocate(nextBacking++);
            context.names().table(ObjectKind.FRAMEBUFFER).allocate(nextBacking++);
        }
    }

    private void reloadSharedTexture(CompatibilityContext context, long frame) {
        NameTable textures = context.names().table(ObjectKind.TEXTURE);
        int texture = textures.allocate(nextBacking++);
        textures.markUsed(texture, frame);
        textures.rebind(texture, nextBacking++, (kind, backing) -> disposedCount++);
        textures.collect(frame, (kind, backing) -> disposedCount++);
        assertEquals(0, textures.retiringCount());
    }
}
