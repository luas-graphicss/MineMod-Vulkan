package io.github.luasgraphicss.minemod.adapters.events.frame;

import io.github.luasgraphicss.minemod.adapters.events.contract.LoaderEventContract;
import io.github.luasgraphicss.minemod.adapters.events.dispatch.ElementPhase;
import io.github.luasgraphicss.minemod.adapters.events.dispatch.EventOutcome;
import io.github.luasgraphicss.minemod.adapters.events.dispatch.LoaderEvents;
import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint.Scope;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class FrameEvents {
    private final LoaderEventContract contract;
    private final LoaderEvents loader;
    private final Set<Scope> renderedScopes = EnumSet.noneOf(Scope.class);
    private final Set<FramePoint> reached = EnumSet.noneOf(FramePoint.class);
    private final Set<FramePoint> canceledPoints = EnumSet.noneOf(FramePoint.class);
    private final Set<String> canceledElements = new HashSet<>();
    private FramePoint latest;
    private boolean closed;

    public FrameEvents(LoaderEventContract contract, LoaderEvents loader, Collection<Scope> renderedScopes) {
        this.contract = Objects.requireNonNull(contract, "contract");
        this.loader = Objects.requireNonNull(loader, "loader");
        this.renderedScopes.addAll(renderedScopes);
    }

    public void announce(FramePoint point) {
        if (contract.cancelable(point)) {
            throw new IllegalStateException(
                    point + " can cancel a native draw on " + contract.loader() + ", ask whether it permits the draw");
        }
        advanceTo(point);
        if (contract.notifies(point)) {
            loader.post(point);
        }
    }

    public boolean permits(FramePoint point) {
        advanceTo(point);
        if (!contract.notifies(point)) {
            return true;
        }
        if (loader.post(point) == EventOutcome.CANCELED && contract.cancelable(point)) {
            canceledPoints.add(point);
            return false;
        }
        return true;
    }

    public boolean permitsElement(FramePoint point, String element) {
        requireLatest(point);
        if (!contract.notifiesElementsAt(point)) {
            return true;
        }
        if (loader.postElement(point, element, ElementPhase.BEFORE) == EventOutcome.CANCELED) {
            canceledElements.add(element);
            return false;
        }
        return true;
    }

    public void completeElement(FramePoint point, String element) {
        requireLatest(point);
        if (contract.notifiesElementsAt(point) && !canceledElements.contains(element)) {
            loader.postElement(point, element, ElementPhase.AFTER);
        }
    }

    public List<FramePoint> close() {
        requireOpen();
        closed = true;
        List<FramePoint> missed = new ArrayList<>();
        for (FramePoint point : contract.notified()) {
            if (renderedScopes.contains(point.scope()) && !reached.contains(point) && !withheld(point)) {
                missed.add(point);
            }
        }
        return missed;
    }

    private void advanceTo(FramePoint point) {
        requireOpen();
        if (!renderedScopes.contains(point.scope())) {
            throw new IllegalStateException("a frame without " + point.scope() + " reached " + point);
        }
        if (withheld(point)) {
            throw new IllegalStateException(point + " belongs to what " + point.guard().get() + " canceled");
        }
        if (latest != null && point.compareTo(latest) <= 0) {
            throw new IllegalStateException(point + " was reached after " + latest);
        }
        latest = point;
        reached.add(point);
        canceledElements.clear();
    }

    private boolean withheld(FramePoint point) {
        return point.guard().filter(canceledPoints::contains).isPresent();
    }

    private void requireLatest(FramePoint point) {
        requireOpen();
        if (point != latest) {
            throw new IllegalStateException("elements of " + point + " are drawn while the frame is at " + latest);
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("the frame was closed and announces nothing more");
        }
    }
}
