package io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction;

final class EnabledSteps {
    private final boolean testsAlpha;
    private final boolean lights;
    private final boolean fogs;
    private final boolean[] sampledUnits;

    private EnabledSteps(FixedPipeline pipeline) {
        this.testsAlpha = pipeline.alphaTest().isEnabled();
        this.lights = pipeline.lights().isEnabled();
        this.fogs = pipeline.fog().isEnabled();
        this.sampledUnits = pipeline.units().enabledUnits();
    }

    static EnabledSteps of(FixedPipeline pipeline) {
        return new EnabledSteps(pipeline);
    }

    void restoreTo(FixedPipeline pipeline) {
        pipeline.alphaTest().enable(testsAlpha);
        pipeline.lights().enable(lights);
        pipeline.fog().enable(fogs);
        pipeline.units().restore(sampledUnits);
    }
}
