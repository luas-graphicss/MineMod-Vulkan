package io.github.luasgraphicss.minemod.journey.step;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

final class ResourceActions {
    private static final String PACK_SEPARATOR = ",";
    private static final String ROLLED_BACK = "the game rolled the reload back and unselected its resource packs";

    private ResourceActions() {}

    static StepAction select(String packs) {
        List<String> selected = packs.isEmpty() ? List.of() : List.of(packs.split(PACK_SEPARATOR));
        return StepAction.immediate(minecraft -> {
            PackRepository repository = minecraft.getResourcePackRepository();
            repository.reload();
            for (String pack : selected) {
                if (!repository.isAvailable(pack)) {
                    throw new IllegalStateException("pack " + pack + " is not installed in the game directory");
                }
            }
            repository.setSelected(selected);
        });
    }

    static StepAction reload() {
        return new Reload();
    }

    private static final class Reload implements StepAction {
        private CompletableFuture<Void> reloaded;

        @Override
        public void begin(Minecraft minecraft) {
            reloaded = minecraft.reloadResourcePacks();
        }

        @Override
        public boolean isDone(Minecraft minecraft) {
            if (minecraft.getOverlay() != null) {
                return false;
            }
            if (!reloaded.isDone()) {
                throw new IllegalStateException(ROLLED_BACK);
            }
            reloaded.join();
            return true;
        }
    }
}
