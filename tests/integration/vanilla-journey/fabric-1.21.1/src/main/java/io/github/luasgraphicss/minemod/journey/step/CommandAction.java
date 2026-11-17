package io.github.luasgraphicss.minemod.journey.step;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandSourceStack;

final class CommandAction implements StepAction {
    private final String command;
    private final boolean mustSucceed;
    private final CompletableFuture<Boolean> succeeded = new CompletableFuture<>();

    private CommandAction(String command, boolean mustSucceed) {
        this.command = command;
        this.mustSucceed = mustSucceed;
    }

    static CommandAction required(String command) {
        return new CommandAction(command, true);
    }

    static CommandAction attempted(String command) {
        return new CommandAction(command, false);
    }

    @Override
    public void begin(Minecraft minecraft) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            throw new IllegalStateException("a command runs on the integrated server of an open world");
        }
        server.execute(() -> run(server));
    }

    @Override
    public boolean isDone(Minecraft minecraft) {
        if (!succeeded.isDone()) {
            return false;
        }
        if (!succeeded.join() && mustSucceed) {
            throw new IllegalStateException("the server refused the command");
        }
        return true;
    }

    private void run(IntegratedServer server) {
        AtomicBoolean accepted = new AtomicBoolean();
        CommandSourceStack source = server.createCommandSourceStack()
                .withSuppressedOutput()
                .withCallback((success, result) -> accepted.compareAndSet(false, success));
        try {
            server.getCommands().performPrefixedCommand(source, command);
            succeeded.complete(accepted.get());
        } catch (RuntimeException failure) {
            succeeded.completeExceptionally(failure);
        }
    }
}
