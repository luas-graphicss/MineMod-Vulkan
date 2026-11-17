package io.github.luasgraphicss.minemod.adapters.events.contract;

import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class LoaderEventContract {
    private final String loader;
    private final Set<FramePoint> notified;
    private final Set<FramePoint> cancelable;
    private final Set<FramePoint> elementPoints;

    private LoaderEventContract(Builder builder) {
        this.loader = builder.loader;
        this.notified = Collections.unmodifiableSet(EnumSet.copyOf(builder.notified));
        this.cancelable = Collections.unmodifiableSet(EnumSet.copyOf(builder.cancelable));
        this.elementPoints = Collections.unmodifiableSet(EnumSet.copyOf(builder.elementPoints));
    }

    public static Builder of(String loader) {
        if (loader.trim().isEmpty()) {
            throw new IllegalArgumentException("an event contract names its loader");
        }
        return new Builder(loader);
    }

    public String loader() {
        return loader;
    }

    public Set<FramePoint> notified() {
        return notified;
    }

    public boolean notifies(FramePoint point) {
        return notified.contains(point);
    }

    public boolean cancelable(FramePoint point) {
        return cancelable.contains(point);
    }

    public boolean notifiesElementsAt(FramePoint point) {
        return elementPoints.contains(point);
    }

    public static final class Builder {
        private final String loader;
        private final Set<FramePoint> notified = EnumSet.noneOf(FramePoint.class);
        private final Set<FramePoint> cancelable = EnumSet.noneOf(FramePoint.class);
        private final Set<FramePoint> elementPoints = EnumSet.noneOf(FramePoint.class);

        private Builder(String loader) {
            this.loader = loader;
        }

        public Builder notifies(FramePoint... points) {
            notified.addAll(Arrays.asList(points));
            return this;
        }

        public Builder cancels(FramePoint... points) {
            notified.addAll(Arrays.asList(points));
            cancelable.addAll(Arrays.asList(points));
            return this;
        }

        public Builder notifiesElementsAt(FramePoint... points) {
            notified.addAll(Arrays.asList(points));
            elementPoints.addAll(Arrays.asList(points));
            return this;
        }

        public LoaderEventContract build() {
            return new LoaderEventContract(this);
        }
    }
}
