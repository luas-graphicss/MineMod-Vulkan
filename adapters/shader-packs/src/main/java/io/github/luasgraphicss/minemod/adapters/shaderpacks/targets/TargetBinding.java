package io.github.luasgraphicss.minemod.adapters.shaderpacks.targets;

import java.util.Objects;

public final class TargetBinding {
    private final PackTarget target;
    private final BufferSide side;

    public TargetBinding(PackTarget target, BufferSide side) {
        this.target = Objects.requireNonNull(target, "target");
        this.side = Objects.requireNonNull(side, "side");
    }

    public PackTarget target() {
        return target;
    }

    public BufferSide side() {
        return side;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TargetBinding)) {
            return false;
        }
        TargetBinding binding = (TargetBinding) other;
        return target.equals(binding.target) && side == binding.side;
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, side);
    }

    @Override
    public String toString() {
        return target.flips() ? target + "." + side : target.toString();
    }
}
