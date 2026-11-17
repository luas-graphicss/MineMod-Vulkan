package io.github.luasgraphicss.minemod.adapters.minecraft.terrain;

import io.github.luasgraphicss.minemod.codec.scene.FaceConnections;
import io.github.luasgraphicss.minemod.codec.scene.SectionFace;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.Direction;

public final class FaceCapture {
    private FaceCapture() {}

    public static FaceConnections connectionsOf(VisibilitySet visibility) {
        FaceConnections connections = FaceConnections.none();
        for (Direction first : Direction.values()) {
            for (Direction second : Direction.values()) {
                if (visibility.visibilityBetween(first, second)) {
                    connections = connections.connecting(faceOf(first), faceOf(second));
                }
            }
        }
        return connections;
    }

    private static SectionFace faceOf(Direction direction) {
        switch (direction) {
            case WEST:
                return SectionFace.WEST;
            case EAST:
                return SectionFace.EAST;
            case DOWN:
                return SectionFace.DOWN;
            case UP:
                return SectionFace.UP;
            case NORTH:
                return SectionFace.NORTH;
            default:
                return SectionFace.SOUTH;
        }
    }
}
