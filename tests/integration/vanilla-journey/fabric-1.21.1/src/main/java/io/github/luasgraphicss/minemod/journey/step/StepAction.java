package io.github.luasgraphicss.minemod.journey.step;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;

public interface StepAction {
    void begin(Minecraft minecraft);

    boolean isDone(Minecraft minecraft);

    static StepAction immediate(Consumer<Minecraft> action) {
        return until(action, minecraft -> true);
    }

    static StepAction until(Consumer<Minecraft> action, Predicate<Minecraft> done) {
        return new StepAction() {
            @Override
            public void begin(Minecraft minecraft) {
                action.accept(minecraft);
            }

            @Override
            public boolean isDone(Minecraft minecraft) {
                return done.test(minecraft);
            }
        };
    }
}
