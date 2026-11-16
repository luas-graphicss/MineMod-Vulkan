package io.github.luasgraphicss.minemod.client.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.client.support.RecordedStream;
import io.github.luasgraphicss.minemod.codec.scene.FaceConnections;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import io.github.luasgraphicss.minemod.codec.scene.SectionFace;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class TerrainPublisherTest {
    private static final SectionPosition SECTION = new SectionPosition(3, -4, 5);
    private static final int WORLD_OFFSET = 12;
    private static final SectionPosition NEIGHBOUR = new SectionPosition(4, -4, 5);
    private static final SectionPosition FARTHER_SECTION = new SectionPosition(5, -4, 5);
    private static final long MAX_SECTION_BYTES = 4096;
    private static final int MAX_DESCRIBED_SECTIONS = 2;
    private static final FaceConnections WALLED = FaceConnections.none().connecting(SectionFace.WEST, SectionFace.EAST);
    private static final BooleanSupplier CURRENT = () -> false;
    private static final BooleanSupplier WITHDRAWN = () -> true;

    private final TerrainPublisher terrain = new TerrainPublisher(MAX_SECTION_BYTES, MAX_DESCRIBED_SECTIONS);

    @Test
    void publishesACapturedSectionAtItsPosition() {
        terrain.capture(SECTION, CURRENT, TerrainFixtures.solidQuads(1));
        RecordedStream stream = new RecordedStream();

        terrain.publish(stream.scene(), roomyBudget());

        assertEquals(Collections.singletonList(SceneOpcode.PUBLISH_SECTION), stream.opcodes());
        assertEquals(3, stream.payload(0).getInt(0));
        assertEquals(-4, stream.payload(0).getInt(4));
        assertEquals(5, stream.payload(0).getInt(8));
        assertEquals(0, stream.payload(0).getInt(WORLD_OFFSET));
    }

    @Test
    void announcesAWorldChangeBeforeTheSectionsOfTheNewWorld() {
        terrain.worldChanged();
        terrain.capture(SECTION, CURRENT, TerrainFixtures.solidQuads(1));
        RecordedStream stream = new RecordedStream();

        terrain.publish(stream.scene(), roomyBudget());

        assertEquals(Arrays.asList(SceneOpcode.CHANGE_WORLD, SceneOpcode.PUBLISH_SECTION), stream.opcodes());
        assertEquals(1, stream.payload(1).getInt(WORLD_OFFSET));
    }

    @Test
    void dropsASectionWithdrawnBeforeItWasPublished() {
        terrain.capture(SECTION, WITHDRAWN, TerrainFixtures.solidQuads(1));
        StreamBudget budget = roomyBudget();

        terrain.publish(new RecordedStream().scene(), budget);

        assertFalse(budget.hasTaken());
        assertEquals(1, terrain.withdrawnSections());
    }

    @Test
    void unloadsAPublishedSectionOnceItIsReset() {
        publishSolidSection();
        terrain.sectionReset(SECTION);

        assertEquals(Collections.singletonList(SceneOpcode.UNLOAD_SECTION), publishNext());
    }

    @Test
    void ignoresTheResetOfASectionThatWasNeverPublished() {
        terrain.sectionReset(SECTION);
        StreamBudget budget = roomyBudget();

        terrain.publish(new RecordedStream().scene(), budget);

        assertFalse(budget.hasTaken());
    }

    @Test
    void unloadsAPublishedSectionWhoseNewGeometryIsEmpty() {
        publishSolidSection();
        terrain.capture(SECTION, CURRENT, SectionGeometry.empty());

        assertEquals(Collections.singletonList(SceneOpcode.UNLOAD_SECTION), publishNext());
    }

    @Test
    void unloadsAPublishedSectionWhoseNewGeometryIsTooLargeToPublish() {
        publishSolidSection();
        terrain.capture(SECTION, CURRENT, TerrainFixtures.solidQuads(64));

        assertEquals(Collections.singletonList(SceneOpcode.UNLOAD_SECTION), publishNext());
        assertEquals(1, terrain.oversizedSections());
    }

    @Test
    void keepsASectionThatDidNotFitForTheNextStream() {
        terrain.capture(SECTION, CURRENT, TerrainFixtures.solidQuads(1));
        terrain.publish(new RecordedStream().scene(), new StreamBudget(64, 16));

        assertEquals(Collections.singletonList(SceneOpcode.PUBLISH_SECTION), publishNext());
    }

    @Test
    void dropsADeferredSectionThatWasWithdrawnMeanwhile() {
        boolean[] isWithdrawn = {false};
        terrain.capture(SECTION, () -> isWithdrawn[0], TerrainFixtures.solidQuads(1));
        terrain.publish(new RecordedStream().scene(), new StreamBudget(64, 16));
        isWithdrawn[0] = true;

        assertEquals(Collections.emptyList(), publishNext());
        assertEquals(1, terrain.withdrawnSections());
    }

    @Test
    void forgetsTheSectionsOfTheWorldThatWasLeft() {
        publishSolidSection();
        terrain.worldChanged();
        terrain.sectionReset(SECTION);

        assertEquals(Collections.singletonList(SceneOpcode.CHANGE_WORLD), publishNext());
    }

    @Test
    void tellsWhichFacesOfASectionSeeEachOther() {
        terrain.describe(SECTION, CURRENT, WALLED);
        RecordedStream stream = new RecordedStream();
        terrain.publish(stream.scene(), roomyBudget());

        assertEquals(Collections.singletonList(SceneOpcode.CONNECT_SECTION_FACES), stream.opcodes());
        assertEquals(3, stream.payload(0).getInt(0));
        assertEquals(-4, stream.payload(0).getInt(4));
        assertEquals(5, stream.payload(0).getInt(8));
        assertEquals(0, stream.payload(0).getInt(WORLD_OFFSET));
    }

    @Test
    void saysNothingAboutASectionWhoseFacesAllSeeEachOther() {
        terrain.describe(SECTION, CURRENT, FaceConnections.open());

        assertEquals(Collections.emptyList(), publishNext());
    }

    @Test
    void opensTheFacesOfASectionThatWasDescribedBefore() {
        terrain.describe(SECTION, CURRENT, WALLED);
        publishNext();
        terrain.describe(SECTION, CURRENT, FaceConnections.open());

        assertEquals(Collections.singletonList(SceneOpcode.CONNECT_SECTION_FACES), publishNext());
    }

    @Test
    void unloadsADescribedSectionOnceItIsReset() {
        terrain.describe(SECTION, CURRENT, WALLED);
        publishNext();
        terrain.sectionReset(SECTION);

        assertEquals(Collections.singletonList(SceneOpcode.UNLOAD_SECTION), publishNext());
    }

    @Test
    void dropsADescriptionWithdrawnBeforeItWasPublished() {
        terrain.describe(SECTION, WITHDRAWN, WALLED);

        assertEquals(Collections.emptyList(), publishNext());
    }

    @Test
    void keepsADescriptionThatDidNotFitForTheNextStream() {
        terrain.describe(SECTION, CURRENT, WALLED);
        terrain.publish(new RecordedStream().scene(), new StreamBudget(0, 0));

        assertEquals(Collections.singletonList(SceneOpcode.CONNECT_SECTION_FACES), publishNext());
    }

    @Test
    void leavesASectionOpenOnceTheSceneDescribesAsManyAsItHolds() {
        terrain.describe(SECTION, CURRENT, WALLED);
        terrain.describe(NEIGHBOUR, CURRENT, WALLED);
        terrain.describe(FARTHER_SECTION, CURRENT, WALLED);

        assertEquals(
                Arrays.asList(SceneOpcode.CONNECT_SECTION_FACES, SceneOpcode.CONNECT_SECTION_FACES), publishNext());
        assertEquals(1, terrain.undescribedSections());
    }

    private void publishSolidSection() {
        terrain.capture(SECTION, CURRENT, TerrainFixtures.solidQuads(1));
        terrain.publish(new RecordedStream().scene(), roomyBudget());
    }

    private List<SceneOpcode> publishNext() {
        RecordedStream stream = new RecordedStream();
        terrain.publish(stream.scene(), roomyBudget());
        return stream.opcodes();
    }

    private static StreamBudget roomyBudget() {
        return new StreamBudget(32 * 1024, 64);
    }
}
