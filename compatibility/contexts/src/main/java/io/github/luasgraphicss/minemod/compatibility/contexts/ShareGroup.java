package io.github.luasgraphicss.minemod.compatibility.contexts;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectScope;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameSpace;

public final class ShareGroup {
    private final int id;
    private final NameSpace sharedNames;
    private int memberCount;

    ShareGroup(int id, int nameCapacityPerKind) {
        this.id = id;
        this.sharedNames = new NameSpace(ObjectScope.SHARE_GROUP, nameCapacityPerKind);
    }

    public int id() {
        return id;
    }

    public NameSpace sharedNames() {
        return sharedNames;
    }

    void join() {
        memberCount++;
    }

    boolean leave() {
        memberCount--;
        if (memberCount > 0) {
            return false;
        }
        sharedNames.discardAll();
        return true;
    }

    @Override
    public String toString() {
        return "share group " + id;
    }
}
