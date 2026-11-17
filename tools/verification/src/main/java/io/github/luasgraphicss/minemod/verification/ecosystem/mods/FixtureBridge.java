package io.github.luasgraphicss.minemod.verification.ecosystem.mods;

import io.github.luasgraphicss.minemod.compatibility.bridges.activation.BridgeCandidate;
import io.github.luasgraphicss.minemod.compatibility.bridges.contract.BridgeContract;
import io.github.luasgraphicss.minemod.compatibility.bridges.contract.ContractSymbol;
import io.github.luasgraphicss.minemod.compatibility.spi.BridgeContext;
import io.github.luasgraphicss.minemod.compatibility.spi.BridgeDescriptor;
import io.github.luasgraphicss.minemod.compatibility.spi.RenderBridge;
import io.github.luasgraphicss.minemod.compatibility.spi.SpiVersion;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureRenderers;
import io.github.luasgraphicss.minemod.verification.ecosystem.session.ModContext;
import java.nio.ByteBuffer;
import java.util.List;

public final class FixtureBridge implements RenderBridge {
    public static final String TERRAIN_MESHES = "chunk meshes of the terrain pass";
    public static final String FLUID_SURFACES = "fluid surfaces of the translucent pass";
    public static final String CABLE_MESHES = "cable meshes of the translucent pass";
    public static final String SHADER_PASSES = "passes of the shader pack";

    private final BridgeDescriptor descriptor;
    private final ModContext mod;
    private BridgeContext context;

    private FixtureBridge(String operation, ModContext mod) {
        this.descriptor = new BridgeDescriptor(mod.modId() + ": " + operation, SpiVersion.CURRENT, List.of(operation));
        this.mod = mod;
    }

    public static BridgeCandidate candidate(String operation, ModContext mod) {
        BridgeContract contract =
                new BridgeContract(mod.modId(), List.of(FixtureRenderers.BRIDGED_VERSION), List.of(symbolOf(operation)));
        return new BridgeCandidate(new FixtureBridge(operation, mod), contract);
    }

    public static ContractSymbol symbolOf(String operation) {
        return new ContractSymbol(
                "fixture." + operation.replace(' ', '_'), "commands", "()Ljava/nio/ByteBuffer;");
    }

    @Override
    public BridgeDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public void attach(BridgeContext attached) {
        context = attached;
        mod.note("attaches a bridge");
    }

    @Override
    public long serve(String operation, long[] arguments) {
        ByteBuffer commands = ByteBuffer.allocate(Long.BYTES * arguments.length);
        for (long argument : arguments) {
            commands.putLong(argument);
        }
        commands.flip();
        context.submit(commands);
        mod.note("submits " + operation);
        return arguments.length;
    }

    @Override
    public void detach() {
        mod.note("detaches a bridge");
    }
}
