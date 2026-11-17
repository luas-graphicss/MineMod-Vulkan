package io.github.luasgraphicss.minemod.adapters.fabric.mixins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class MixinApplicationTest {
    private static final String CONFIG_PROPERTY = "minemod.mixinConfig";

    @Test
    void everyMixinOfTheGameAdapterIsAppliedToItsTargets() throws IOException, ClassNotFoundException {
        ClassLoader game = MixinApplicationTest.class.getClassLoader();
        List<DeclaredMixin> mixins = DeclaredMixins.listedIn(System.getProperty(CONFIG_PROPERTY), game);
        assertFalse(mixins.isEmpty(), "the game adapter lists no mixin");

        List<String> missing = new ArrayList<>();
        for (DeclaredMixin mixin : mixins) {
            for (String target : mixin.targets()) {
                Class<?> transformed = Class.forName(target, false, game);
                if (!AppliedMixins.on(transformed).contains(mixin.name())) {
                    missing.add(mixin.name() + " on " + target);
                }
            }
        }

        assertEquals(List.of(), missing, "mixins that left nothing in their target");
    }
}
