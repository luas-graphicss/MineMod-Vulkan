package io.github.luasgraphicss.minemod.adapters.events.resources;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public final class ResourceGenerations {
    private final Set<ResourceExtensionPoint> required = EnumSet.noneOf(ResourceExtensionPoint.class);
    private final Set<ResourceExtensionPoint> completed = EnumSet.noneOf(ResourceExtensionPoint.class);
    private long generation;
    private long publishedGeneration;
    private boolean reloading;

    public ResourceGenerations(Collection<ResourceExtensionPoint> required) {
        this.required.addAll(required);
    }

    public synchronized void beginReload() {
        generation++;
        publishedGeneration = 0;
        completed.clear();
        reloading = true;
    }

    public synchronized void complete(ResourceExtensionPoint point) {
        if (!reloading) {
            throw new IllegalStateException(point + " completed outside a resource reload");
        }
        completed.add(point);
    }

    public synchronized long publish() {
        if (!reloading) {
            throw new IllegalStateException("no resource reload is waiting to be published");
        }
        Set<ResourceExtensionPoint> pending = EnumSet.copyOf(required);
        pending.removeAll(completed);
        if (!pending.isEmpty()) {
            throw new IllegalStateException(
                    "the native snapshot would miss what mods add at " + pending + " of generation " + generation);
        }
        reloading = false;
        publishedGeneration = generation;
        return generation;
    }

    public synchronized boolean isCurrent(long snapshotGeneration) {
        return snapshotGeneration != 0 && snapshotGeneration == publishedGeneration;
    }
}
