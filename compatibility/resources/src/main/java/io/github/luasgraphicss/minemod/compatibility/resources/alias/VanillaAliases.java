package io.github.luasgraphicss.minemod.compatibility.resources.alias;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ObjectDisposer;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;

public final class VanillaAliases {
    private final NameTable textures;
    private final Map<Long, Integer> namesByResource = new HashMap<>();
    private final Map<Integer, Published> publishedByName = new HashMap<>();

    public VanillaAliases(NameTable textures) {
        if (textures.kind() != ObjectKind.TEXTURE) {
            throw new IllegalArgumentException("vanilla resources are published as textures, not as " + textures.kind());
        }
        this.textures = textures;
    }

    public synchronized int publish(long nativeResource, int importedBacking, ObjectDisposer disposer) {
        OptionalInt published = nameOf(nativeResource);
        int name;
        if (published.isPresent()) {
            name = published.getAsInt();
            textures.rebind(name, importedBacking, disposer);
        } else {
            name = textures.allocate(importedBacking);
            namesByResource.put(nativeResource, name);
        }
        publishedByName.put(name, new Published(nativeResource, importedBacking));
        return name;
    }

    public synchronized boolean withdraw(long nativeResource, ObjectDisposer disposer) {
        OptionalInt published = nameOf(nativeResource);
        if (!published.isPresent()) {
            return false;
        }
        forget(published.getAsInt());
        textures.delete(published.getAsInt(), disposer);
        return true;
    }

    public synchronized boolean detach(int name) {
        if (current(name) == null) {
            return false;
        }
        forget(name);
        return true;
    }

    public synchronized boolean isAlias(int name) {
        return current(name) != null;
    }

    public synchronized OptionalInt nameOf(long nativeResource) {
        Integer name = namesByResource.get(nativeResource);
        return name == null || current(name) == null ? OptionalInt.empty() : OptionalInt.of(name);
    }

    public synchronized OptionalLong resourceOf(int name) {
        Published published = current(name);
        return published == null ? OptionalLong.empty() : OptionalLong.of(published.nativeResource);
    }

    public synchronized int aliasCount() {
        int count = 0;
        for (Integer name : publishedByName.keySet().toArray(new Integer[0])) {
            if (current(name) != null) {
                count++;
            }
        }
        return count;
    }

    private Published current(int name) {
        Published published = publishedByName.get(name);
        if (published == null) {
            return null;
        }
        OptionalInt holder = textures.nameOf(published.backing);
        if (holder.isPresent() && holder.getAsInt() == name) {
            return published;
        }
        forget(name);
        return null;
    }

    private void forget(int name) {
        Published published = publishedByName.remove(name);
        if (published != null) {
            namesByResource.remove(published.nativeResource);
        }
    }

    private static final class Published {
        private final long nativeResource;
        private final int backing;

        private Published(long nativeResource, int backing) {
            this.nativeResource = nativeResource;
            this.backing = backing;
        }
    }
}
