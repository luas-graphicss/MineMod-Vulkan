package io.github.luasgraphicss.minemod.verification.differential;

import java.util.List;
import java.util.Optional;

public final class Divergence {
    private Divergence() {}

    public static Optional<String> first(String subject, List<String> reference, List<String> layer) {
        int shared = Math.min(reference.size(), layer.size());
        for (int index = 0; index < shared; index++) {
            if (!reference.get(index).equals(layer.get(index))) {
                return Optional.of(subject + " " + index + ": the reference has " + reference.get(index)
                        + ", the layer has " + layer.get(index));
            }
        }
        if (reference.size() != layer.size()) {
            return Optional.of(subject + " count: the reference has " + reference.size() + ", the layer has "
                    + layer.size());
        }
        return Optional.empty();
    }
}
