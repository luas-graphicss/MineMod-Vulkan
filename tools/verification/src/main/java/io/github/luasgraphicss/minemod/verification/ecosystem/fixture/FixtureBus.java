package io.github.luasgraphicss.minemod.verification.ecosystem.fixture;

import io.github.luasgraphicss.minemod.adapters.events.dispatch.ElementPhase;
import io.github.luasgraphicss.minemod.adapters.events.dispatch.EventOutcome;
import io.github.luasgraphicss.minemod.adapters.events.dispatch.LoaderEvents;
import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FixtureBus implements LoaderEvents {
    public enum Dispatch {
        PRIORITY_BUS,
        CALLBACK_ARRAY
    }

    public enum Priority {
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
    }

    @FunctionalInterface
    public interface Listener {
        boolean cancels(boolean alreadyCanceled);
    }

    private record Registration(String event, Priority priority, boolean receivesCanceled, Listener listener) {}

    private final Dispatch dispatch;
    private final List<Registration> registrations = new ArrayList<>();

    public FixtureBus(Dispatch dispatch) {
        this.dispatch = dispatch;
    }

    public void on(FramePoint point, Priority priority, boolean receivesCanceled, Listener listener) {
        registrations.add(new Registration(point.name(), priority, receivesCanceled, listener));
    }

    public void onElement(
            FramePoint point,
            String element,
            ElementPhase phase,
            Priority priority,
            boolean receivesCanceled,
            Listener listener) {
        registrations.add(new Registration(eventOf(point, element, phase), priority, receivesCanceled, listener));
    }

    @Override
    public EventOutcome post(FramePoint point) {
        return dispatch(point.name());
    }

    @Override
    public EventOutcome postElement(FramePoint point, String element, ElementPhase phase) {
        return dispatch(eventOf(point, element, phase));
    }

    private EventOutcome dispatch(String event) {
        List<Registration> listeners = new ArrayList<>(
                registrations.stream().filter(registration -> registration.event().equals(event)).toList());
        if (dispatch == Dispatch.PRIORITY_BUS) {
            listeners.sort(Comparator.comparing(Registration::priority));
        }
        boolean canceled = false;
        for (Registration registration : listeners) {
            if (dispatch == Dispatch.PRIORITY_BUS && canceled && !registration.receivesCanceled()) {
                continue;
            }
            canceled |= registration.listener().cancels(canceled);
        }
        return canceled ? EventOutcome.CANCELED : EventOutcome.PROCEEDED;
    }

    private static String eventOf(FramePoint point, String element, ElementPhase phase) {
        return point + " " + element + " " + phase;
    }
}
