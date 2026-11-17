package io.github.luasgraphicss.minemod.verification.ecosystem.fixture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.adapters.events.dispatch.ElementPhase;
import io.github.luasgraphicss.minemod.adapters.events.dispatch.EventOutcome;
import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus.Dispatch;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus.Priority;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FixtureBusTest {
    private final List<String> calls = new ArrayList<>();

    @Test
    void aPriorityBusCallsListenersFromHighestToLowest() {
        FixtureBus bus = new FixtureBus(Dispatch.PRIORITY_BUS);
        listen(bus, "low", Priority.LOW, false, false);
        listen(bus, "highest", Priority.HIGHEST, false, false);
        listen(bus, "normal", Priority.NORMAL, false, false);
        listen(bus, "second normal", Priority.NORMAL, false, false);

        assertEquals(EventOutcome.PROCEEDED, bus.post(FramePoint.WORLD_LAST));
        assertEquals(List.of("highest", "normal", "second normal", "low"), calls);
    }

    @Test
    void aCanceledEventReachesOnlyTheListenersThatReceiveCanceledEvents() {
        FixtureBus bus = new FixtureBus(Dispatch.PRIORITY_BUS);
        listen(bus, "plain", Priority.LOWEST, false, false);
        listen(bus, "canceling", Priority.HIGH, false, true);
        listen(bus, "receiving", Priority.LOW, true, false);

        assertEquals(EventOutcome.CANCELED, bus.post(FramePoint.WORLD_LAST));
        assertEquals(List.of("canceling", "receiving saw it canceled"), calls);
    }

    @Test
    void aCallbackArrayCallsEveryListenerInRegistrationOrder() {
        FixtureBus bus = new FixtureBus(Dispatch.CALLBACK_ARRAY);
        listen(bus, "first", Priority.LOWEST, false, true);
        listen(bus, "second", Priority.HIGHEST, false, false);

        assertEquals(EventOutcome.CANCELED, bus.post(FramePoint.WORLD_LAST));
        assertEquals(List.of("first", "second saw it canceled"), calls);
    }

    @Test
    void anElementEventReachesOnlyTheListenersOfThatElementAndPhase() {
        FixtureBus bus = new FixtureBus(Dispatch.PRIORITY_BUS);
        bus.onElement(FramePoint.HUD_ELEMENTS, "hotbar", ElementPhase.BEFORE, Priority.NORMAL, false, canceled -> {
            calls.add("hotbar before");
            return true;
        });

        assertEquals(
                EventOutcome.PROCEEDED, bus.postElement(FramePoint.HUD_ELEMENTS, "hotbar", ElementPhase.AFTER));
        assertEquals(
                EventOutcome.PROCEEDED, bus.postElement(FramePoint.HUD_ELEMENTS, "health", ElementPhase.BEFORE));
        assertEquals(
                EventOutcome.CANCELED, bus.postElement(FramePoint.HUD_ELEMENTS, "hotbar", ElementPhase.BEFORE));
        assertEquals(List.of("hotbar before"), calls);
    }

    private void listen(FixtureBus bus, String name, Priority priority, boolean receivesCanceled, boolean cancels) {
        bus.on(FramePoint.WORLD_LAST, priority, receivesCanceled, canceled -> {
            calls.add(canceled ? name + " saw it canceled" : name);
            return cancels;
        });
    }
}
