package io.github.luasgraphicss.minemod.client.terrain;

import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;

public final class TerrainPublisher {
    private static final long WORLD_CHANGE_BYTES = SceneOpcode.CHANGE_WORLD.fixedBytes();
    private static final long UNLOAD_BYTES = SceneOpcode.UNLOAD_SECTION.fixedBytes();
    private static final long PUBLICATION_BYTES = SceneOpcode.PUBLISH_SECTION.fixedBytes();

    private final Queue<CapturedSection> captured = new ConcurrentLinkedQueue<>();
    private final Deque<SectionPosition> unloads = new ArrayDeque<>();
    private final Set<SectionPosition> liveSections = new HashSet<>();
    private final long maxSectionBytes;
    private CapturedSection deferred;
    private int world;
    private int unpublishedWorldChanges;
    private long withdrawnSections;
    private long oversizedSections;

    public TerrainPublisher(long maxSectionBytes) {
        if (maxSectionBytes < 1) {
            throw new IllegalArgumentException("a section needs room for its geometry: " + maxSectionBytes);
        }
        this.maxSectionBytes = maxSectionBytes;
    }

    public void capture(SectionPosition position, BooleanSupplier isWithdrawn, SectionGeometry geometry) {
        captured.add(new CapturedSection(position, isWithdrawn, geometry));
    }

    public void sectionReset(SectionPosition position) {
        if (liveSections.remove(position)) {
            unloads.add(position);
        }
    }

    public void worldChanged() {
        liveSections.clear();
        unloads.clear();
        world++;
        unpublishedWorldChanges++;
    }

    public void publish(SceneCommandWriter scene, StreamBudget budget) {
        while (unpublishedWorldChanges > 0) {
            if (!budget.take(WORLD_CHANGE_BYTES)) {
                return;
            }
            scene.changeWorld();
            unpublishedWorldChanges--;
        }
        while (!unloads.isEmpty()) {
            if (!budget.take(UNLOAD_BYTES)) {
                return;
            }
            SectionPosition unloaded = unloads.remove();
            scene.unloadSection(unloaded.x(), unloaded.y(), unloaded.z());
        }
        for (CapturedSection section = nextCaptured(); section != null; section = nextCaptured()) {
            if (section.isWithdrawn.getAsBoolean()) {
                withdrawnSections++;
            } else if (!write(section, scene, budget)) {
                deferred = section;
                return;
            }
        }
    }

    public long withdrawnSections() {
        return withdrawnSections;
    }

    public long oversizedSections() {
        return oversizedSections;
    }

    private CapturedSection nextCaptured() {
        if (deferred == null) {
            return captured.poll();
        }
        CapturedSection waiting = deferred;
        deferred = null;
        return waiting;
    }

    private boolean write(CapturedSection section, SceneCommandWriter scene, StreamBudget budget) {
        SectionPosition position = section.position;
        SectionGeometry geometry = section.geometry;
        if (!geometry.isEmpty() && geometry.bytes() <= maxSectionBytes) {
            if (!budget.take(PUBLICATION_BYTES + geometry.bytes())) {
                return false;
            }
            scene.publishSection(position.x(), position.y(), position.z(), world, geometry.mesh());
            liveSections.add(position);
            return true;
        }
        boolean isLive = liveSections.contains(position);
        if (isLive && !budget.take(UNLOAD_BYTES)) {
            return false;
        }
        if (isLive) {
            scene.unloadSection(position.x(), position.y(), position.z());
            liveSections.remove(position);
        }
        if (!geometry.isEmpty()) {
            oversizedSections++;
        }
        return true;
    }

    private static final class CapturedSection {
        private final SectionPosition position;
        private final BooleanSupplier isWithdrawn;
        private final SectionGeometry geometry;

        private CapturedSection(SectionPosition position, BooleanSupplier isWithdrawn, SectionGeometry geometry) {
            this.position = position;
            this.isWithdrawn = isWithdrawn;
            this.geometry = geometry;
        }
    }
}
