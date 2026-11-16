package io.github.luasgraphicss.minemod.adapters.transformers.point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MethodTargetTest {
    private static final String OWNER = "game/render/WorldRenderer";

    @Test
    void readsAsOwnerNameAndDescriptor() {
        MethodTarget target = new MethodTarget(OWNER, "renderWorld", "(FJ[Lgame/Entity;)V");

        assertEquals("game/render/WorldRenderer.renderWorld(FJ[Lgame/Entity;)V", target.toString());
    }

    @Test
    void acceptsConstructorsAndStaticInitializers() {
        assertEquals("<init>", new MethodTarget(OWNER, "<init>", "()V").name());
        assertEquals("<clinit>", new MethodTarget(OWNER, "<clinit>", "()V").name());
    }

    @Test
    void tellsOverloadsApartByDescriptor() {
        MethodTarget byFloat = new MethodTarget(OWNER, "renderWorld", "(F)V");
        MethodTarget byLong = new MethodTarget(OWNER, "renderWorld", "(J)V");

        assertNotEquals(byFloat, byLong);
        assertEquals(byFloat, new MethodTarget(OWNER, "renderWorld", "(F)V"));
    }

    @Test
    void rejectsASourceClassNameAsOwner() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MethodTarget("game.render.WorldRenderer", "renderWorld", "(F)V"));
    }

    @Test
    void rejectsAWildcardMethodName() {
        assertThrows(IllegalArgumentException.class, () -> new MethodTarget(OWNER, "render*", "(F)V"));
    }

    @Test
    void rejectsAMissingOrPartialDescriptor() {
        assertThrows(IllegalArgumentException.class, () -> new MethodTarget(OWNER, "renderWorld", ""));
        assertThrows(IllegalArgumentException.class, () -> new MethodTarget(OWNER, "renderWorld", "(F)"));
        assertThrows(IllegalArgumentException.class, () -> new MethodTarget(OWNER, "renderWorld", "(Lgame/Entity)V"));
    }
}
