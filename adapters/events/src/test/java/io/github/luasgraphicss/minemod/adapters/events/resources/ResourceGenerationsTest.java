package io.github.luasgraphicss.minemod.adapters.events.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class ResourceGenerationsTest {
    private final ResourceGenerations generations = new ResourceGenerations(
            EnumSet.of(ResourceExtensionPoint.MODEL_BAKING, ResourceExtensionPoint.ATLAS_STITCHING));

    @Test
    void publishesASnapshotOnlyAfterModsExtendedTheResources() {
        generations.beginReload();
        generations.complete(ResourceExtensionPoint.ATLAS_STITCHING);

        IllegalStateException refusal = assertThrows(IllegalStateException.class, generations::publish);
        generations.complete(ResourceExtensionPoint.MODEL_BAKING);

        assertEquals(
                "the native snapshot would miss what mods add at [MODEL_BAKING] of generation 1", refusal.getMessage());
        assertEquals(1, generations.publish());
        assertTrue(generations.isCurrent(1));
    }

    @Test
    void aReloadRetiresTheSnapshotOfThePreviousGeneration() {
        long first = publishReload();

        generations.beginReload();

        assertFalse(generations.isCurrent(first));
        assertEquals(first + 1, publishReloadInProgress());
        assertFalse(generations.isCurrent(first));
    }

    @Test
    void refusesExtensionsAndSnapshotsOutsideAReload() {
        assertThrows(
                IllegalStateException.class, () -> generations.complete(ResourceExtensionPoint.MODEL_BAKING));
        assertThrows(IllegalStateException.class, generations::publish);
        assertFalse(generations.isCurrent(0));
    }

    private long publishReload() {
        generations.beginReload();
        return publishReloadInProgress();
    }

    private long publishReloadInProgress() {
        generations.complete(ResourceExtensionPoint.MODEL_BAKING);
        generations.complete(ResourceExtensionPoint.ATLAS_STITCHING);
        return generations.publish();
    }
}
