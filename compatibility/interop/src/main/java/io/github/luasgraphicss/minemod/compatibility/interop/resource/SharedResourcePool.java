package io.github.luasgraphicss.minemod.compatibility.interop.resource;

import io.github.luasgraphicss.minemod.compatibility.interop.handle.HandleLedger;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SharedResourcePool {
    private final SharedResourceBackend backend;
    private final HandleLedger handles;
    private final Set<SharedResource> live = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Retiring> retiring = new ArrayList<>();

    public SharedResourcePool(SharedResourceBackend backend, HandleLedger handles) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.handles = Objects.requireNonNull(handles, "handles");
    }

    public SharedResource create(PlannedShare share) {
        long rendererResource = backend.createInRenderer(share);
        int providerObject;
        try {
            providerObject = importFrom(rendererResource, share);
        } catch (RuntimeException failure) {
            backend.destroyInRenderer(rendererResource);
            throw failure;
        }
        SharedResource resource = new SharedResource(share, rendererResource, providerObject);
        live.add(resource);
        return resource;
    }

    public void retire(SharedResource resource, long lastUseFrame) {
        if (!live.remove(resource)) {
            throw new IllegalArgumentException("the shared resource is not live in this pool");
        }
        retiring.add(new Retiring(resource, lastUseFrame));
    }

    public int collect(long completedFrame) {
        int destroyed = 0;
        Iterator<Retiring> candidates = retiring.iterator();
        while (candidates.hasNext()) {
            Retiring candidate = candidates.next();
            if (candidate.lastUseFrame <= completedFrame) {
                candidates.remove();
                backend.destroyInProvider(candidate.resource.providerObject());
                backend.destroyInRenderer(candidate.resource.rendererResource());
                destroyed++;
            }
        }
        return destroyed;
    }

    public int liveCount() {
        return live.size();
    }

    public int retiringCount() {
        return retiring.size();
    }

    private int importFrom(long rendererResource, PlannedShare share) {
        long handle = backend.exportHandle(rendererResource);
        handles.exported(handle);
        int providerObject;
        try {
            providerObject = backend.importIntoProvider(share, handle);
        } catch (RuntimeException failure) {
            handles.abandoned(handle);
            throw failure;
        }
        handles.imported(handle);
        return providerObject;
    }

    private static final class Retiring {
        private final SharedResource resource;
        private final long lastUseFrame;

        private Retiring(SharedResource resource, long lastUseFrame) {
            this.resource = resource;
            this.lastUseFrame = lastUseFrame;
        }
    }
}
