package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import java.util.ArrayList;
import java.util.List;

final class DisplayList {
    private static final int QUADS = 7;

    private final List<ListedDraw> draws = new ArrayList<>();

    void keep(DrawCapture.Geometry geometry) {
        if (RetainedMesh.isPublishedAsASection(geometry.format())) {
            return;
        }
        int last = draws.size() - 1;
        if (last >= 0 && draws.get(last).continuesWith(geometry)) {
            draws.set(last, draws.get(last).joinedWith(geometry));
            return;
        }
        draws.add(ListedDraw.of(geometry));
    }

    void record() {
        for (ListedDraw draw : draws) {
            draw.record();
        }
    }

    private static final class ListedDraw {
        private final int mode;
        private final RetainedMesh mesh;

        private ListedDraw(int mode, RetainedMesh mesh) {
            this.mode = mode;
            this.mesh = mesh;
        }

        static ListedDraw of(DrawCapture.Geometry geometry) {
            return new ListedDraw(geometry.mode(), RetainedMesh.copyOf(geometry.format(), geometry.vertices()));
        }

        boolean continuesWith(DrawCapture.Geometry geometry) {
            return mode == QUADS && geometry.mode() == QUADS && mesh.isStoredAs(geometry.format());
        }

        ListedDraw joinedWith(DrawCapture.Geometry geometry) {
            return new ListedDraw(mode, mesh.joinedWith(geometry.vertices()));
        }

        void record() {
            mesh.record(mode);
        }
    }
}
