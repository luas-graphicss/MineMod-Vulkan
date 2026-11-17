package io.github.luasgraphicss.minemod.client.draw.frame;

import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import io.github.luasgraphicss.minemod.codec.scene.ViewState;
import java.util.ArrayList;
import java.util.List;

final class FrameViews {
    static final int NO_VIEW = -1;

    private static final long VIEW_BYTES = SceneOpcode.SET_VIEW.fixedBytes();

    private final List<GameView> views = new ArrayList<>();

    int indexOf(GameView view, StreamBudget budget) {
        int known = views.indexOf(view);
        if (known >= 0) {
            return known;
        }
        if (views.size() == ViewState.MAX_VIEWS || !budget.take(VIEW_BYTES)) {
            return NO_VIEW;
        }
        views.add(view);
        return views.size() - 1;
    }

    int writeFrom(int firstUnwritten, SceneCommandWriter frame) {
        for (int index = firstUnwritten; index < views.size(); index++) {
            frame.setView(views.get(index).toScene(index));
        }
        return views.size();
    }

    void clear() {
        views.clear();
    }
}
