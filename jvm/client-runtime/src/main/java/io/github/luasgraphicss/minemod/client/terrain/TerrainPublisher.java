package io.github.luasgraphicss.minemod.client.terrain;

import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.codec.scene.FaceConnections;
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
    private static final long DESCRIPTION_BYTES = SceneOpcode.CONNECT_SECTION_FACES.fixedBytes();

    private final Queue<CapturedSection> captured = new ConcurrentLinkedQueue<>();
    private final Queue<DescribedSection> described = new ConcurrentLinkedQueue<>();
    private final Deque<SectionPosition> unloads = new ArrayDeque<>();
    private final Set<SectionPosition> liveSections = new HashSet<>();
    private final Set<SectionPosition> describedSections = new HashSet<>();
    private final long maxSectionBytes;
    private final int maxDescribedSections;
    private CapturedSection deferred;
    private int world;
    private int unpublishedWorldChanges;
    private long withdrawnSections;
    private long oversizedSections;
    private long undescribedSections;

    public TerrainPublisher(long maxSectionBytes, int maxDescribedSections) {
        if (maxSectionBytes < 1) {
            throw new IllegalArgumentException("a section needs room for its geometry: " + maxSectionBytes);
        }
        if (maxDescribedSections < 0) {
            throw new IllegalArgumentException(
                    "the scene cannot describe a negative number of sections: " + maxDescribedSections);
        }
        this.maxSectionBytes = maxSectionBytes;
        this.maxDescribedSections = maxDescribedSections;
    }

    public void capture(SectionPosition position, BooleanSupplier isWithdrawn, SectionGeometry geometry) {
        captured.add(new CapturedSection(position, isWithdrawn, geometry));
    }

    public void describe(SectionPosition position, BooleanSupplier isWithdrawn, FaceConnections connections) {
        described.add(new DescribedSection(position, isWithdrawn, connections));
    }

    public void sectionReset(SectionPosition position) {
        boolean wasLive = liveSections.remove(position);
        boolean wasDescribed = describedSections.remove(position);
        if (wasLive || wasDescribed) {
            unloads.add(position);
        }
    }

    public void worldChanged() {
        liveSections.clear();
        describedSections.clear();
        unloads.clear();
        world++;
        unpublishedWorldChanges++;
    }

    public void publish(SceneCommandWriter scene, StreamBudget budget) {
        if (leaveWorlds(scene, budget) && unloadSections(scene, budget) && connectFaces(scene, budget)) {
            publishGeometry(scene, budget);
        }
    }

    public long withdrawnSections() {
        return withdrawnSections;
    }

    public long oversizedSections() {
        return oversizedSections;
    }

    public long undescribedSections() {
        return undescribedSections;
    }

    private boolean leaveWorlds(SceneCommandWriter scene, StreamBudget budget) {
        while (unpublishedWorldChanges > 0) {
            if (!budget.take(WORLD_CHANGE_BYTES)) {
                return false;
            }
            scene.changeWorld();
            unpublishedWorldChanges--;
        }
        return true;
    }

    private boolean unloadSections(SceneCommandWriter scene, StreamBudget budget) {
        while (!unloads.isEmpty()) {
            if (!budget.take(UNLOAD_BYTES)) {
                return false;
            }
            SectionPosition unloaded = unloads.remove();
            scene.unloadSection(unloaded.x(), unloaded.y(), unloaded.z());
        }
        return true;
    }

    private boolean connectFaces(SceneCommandWriter scene, StreamBudget budget) {
        for (DescribedSection section = described.peek(); section != null; section = described.peek()) {
            if (!section.isWithdrawn.getAsBoolean() && !connect(section, scene, budget)) {
                return false;
            }
            described.remove();
        }
        return true;
    }

    private void publishGeometry(SceneCommandWriter scene, StreamBudget budget) {
        for (CapturedSection section = nextCaptured(); section != null; section = nextCaptured()) {
            if (section.isWithdrawn.getAsBoolean()) {
                withdrawnSections++;
            } else if (!write(section, scene, budget)) {
                deferred = section;
                return;
            }
        }
    }

    private CapturedSection nextCaptured() {
        if (deferred == null) {
            return captured.poll();
        }
        CapturedSection waiting = deferred;
        deferred = null;
        return waiting;
    }

    private boolean connect(DescribedSection section, SceneCommandWriter scene, StreamBudget budget) {
        SectionPosition position = section.position;
        boolean isDescribed = describedSections.contains(position);
        if (!isDescribed && section.connections.equals(FaceConnections.open())) {
            return true;
        }
        if (!isDescribed && describedSections.size() >= maxDescribedSections) {
            undescribedSections++;
            return true;
        }
        if (!budget.take(DESCRIPTION_BYTES)) {
            return false;
        }
        scene.connectSectionFaces(position.x(), position.y(), position.z(), world, section.connections);
        describedSections.add(position);
        return true;
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

    private static final class DescribedSection {
        private final SectionPosition position;
        private final BooleanSupplier isWithdrawn;
        private final FaceConnections connections;

        private DescribedSection(SectionPosition position, BooleanSupplier isWithdrawn, FaceConnections connections) {
            this.position = position;
            this.isWithdrawn = isWithdrawn;
            this.connections = connections;
        }
    }
}
