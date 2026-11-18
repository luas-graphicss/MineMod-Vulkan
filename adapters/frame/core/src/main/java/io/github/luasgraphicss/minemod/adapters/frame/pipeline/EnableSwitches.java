package io.github.luasgraphicss.minemod.adapters.frame.pipeline;

final class EnableSwitches {
    private static final int ALPHA_TEST = 0x0BC0;
    private static final int BLEND = 0x0BE2;
    private static final int DEPTH_TEST = 0x0B71;
    private static final int CULL_FACE = 0x0B44;
    private static final int FOG = 0x0B60;
    private static final int LIGHTING = 0x0B50;
    private static final int TEXTURE_2D = 0x0DE1;
    private static final int POLYGON_OFFSET_FILL = 0x8037;
    private static final int COLOR_LOGIC_OPERATION = 0x0BF2;
    private static final int GENERATED_HORIZONTAL_COORDINATE = 0x0C60;

    private EnableSwitches() {}

    static void set(FixedPipeline pipeline, int capability, boolean isEnabled) {
        switch (capability) {
            case ALPHA_TEST:
                pipeline.alphaTest().enable(isEnabled);
                break;
            case BLEND:
                pipeline.surface().blend(isEnabled);
                break;
            case DEPTH_TEST:
                pipeline.surface().depthTest(isEnabled);
                break;
            case CULL_FACE:
                pipeline.surface().cullBackFaces(isEnabled);
                break;
            case FOG:
                pipeline.fog().enable(isEnabled);
                break;
            case LIGHTING:
                pipeline.lights().enable(isEnabled);
                break;
            case TEXTURE_2D:
                pipeline.units().enable(isEnabled);
                break;
            case POLYGON_OFFSET_FILL:
                pipeline.surface().depthOffset(isEnabled);
                break;
            case COLOR_LOGIC_OPERATION:
                pipeline.surface().colorLogic(isEnabled);
                break;
            case GENERATED_HORIZONTAL_COORDINATE:
                pipeline.units().generateCoordinates(isEnabled);
                break;
            default:
                break;
        }
    }
}
