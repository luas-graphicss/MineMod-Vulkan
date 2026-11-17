package io.github.luasgraphicss.minemod.journey.mixin;

import java.util.List;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DebugScreenOverlay.class)
public interface DebugScreenOverlayInvoker {
    @Invoker("getSystemInformation")
    List<String> journey$systemInformation();
}
