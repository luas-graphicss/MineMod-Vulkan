package io.github.luasgraphicss.minemod.verification.hooks.catalog;

public record LibraryCall(String hook, String owner, String name, String descriptor) {
    public String signature() {
        return owner + "." + name + descriptor;
    }

    public boolean isCalledBy(String calledOwner, String calledName, String calledDescriptor) {
        return owner.equals(calledOwner) && name.equals(calledName) && descriptor.equals(calledDescriptor);
    }
}
