package io.github.luasgraphicss.minemod.adapters.events.support;

import io.github.luasgraphicss.minemod.adapters.events.dispatch.ElementPhase;
import io.github.luasgraphicss.minemod.adapters.events.dispatch.EventOutcome;
import io.github.luasgraphicss.minemod.adapters.events.dispatch.LoaderEvents;
import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ScriptedLoader implements LoaderEvents {
    private final List<String> posted = new ArrayList<>();
    private final Set<FramePoint> canceledPoints = EnumSet.noneOf(FramePoint.class);
    private final Set<String> canceledElements = new HashSet<>();

    public ScriptedLoader cancel(FramePoint point) {
        canceledPoints.add(point);
        return this;
    }

    public ScriptedLoader cancelElement(String element) {
        canceledElements.add(element);
        return this;
    }

    public List<String> posted() {
        return posted;
    }

    @Override
    public EventOutcome post(FramePoint point) {
        posted.add(point.name());
        return canceledPoints.contains(point) ? EventOutcome.CANCELED : EventOutcome.PROCEEDED;
    }

    @Override
    public EventOutcome postElement(FramePoint point, String element, ElementPhase phase) {
        posted.add(point + " " + element + " " + phase);
        boolean canceled = phase == ElementPhase.BEFORE && canceledElements.contains(element);
        return canceled ? EventOutcome.CANCELED : EventOutcome.PROCEEDED;
    }
}
