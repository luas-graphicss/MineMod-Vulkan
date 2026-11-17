package io.github.luasgraphicss.minemod.adapters.events.dispatch;

import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;

public interface LoaderEvents {
    EventOutcome post(FramePoint point);

    EventOutcome postElement(FramePoint point, String element, ElementPhase phase);
}
