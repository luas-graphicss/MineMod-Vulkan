package io.github.luasgraphicss.minemod.journey.driver;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;

final class ClientThread {
    private final Minecraft minecraft;

    ClientThread(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    void run(Consumer<Minecraft> task) {
        ask(client -> {
            task.accept(client);
            return true;
        });
    }

    <T> T ask(Function<Minecraft, T> question) {
        Supplier<T> answer = () -> question.apply(minecraft);
        return minecraft.submit(answer).join();
    }

    void stopTheGame() {
        minecraft.execute(minecraft::stop);
    }
}
