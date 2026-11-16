package io.github.luasgraphicss.minemod.bridge;

public final class SceneMetrics {
    public static final int FIELD_COUNT = 8;

    private static final int FRAMES_MEASURED = 0;
    private static final int MEAN_FRAME_NANOSECONDS = 1;
    private static final int WORST_FRAME_NANOSECONDS = 2;
    private static final int PEAK_TEXTURE_BYTES = 3;
    private static final int PEAK_STAGING_BYTES = 4;
    private static final int PEAK_PIPELINES = 5;
    private static final int RETIRED_RESOURCES = 6;
    private static final int RESOURCE_GENERATION = 7;

    private final long[] fields;

    public SceneMetrics(long[] fields) {
        if (fields.length != FIELD_COUNT) {
            throw new IllegalArgumentException("scene metrics have " + FIELD_COUNT + " fields, found " + fields.length);
        }
        this.fields = fields.clone();
    }

    public long framesMeasured() {
        return fields[FRAMES_MEASURED];
    }

    public long meanFrameNanoseconds() {
        return fields[MEAN_FRAME_NANOSECONDS];
    }

    public long worstFrameNanoseconds() {
        return fields[WORST_FRAME_NANOSECONDS];
    }

    public long peakTextureBytes() {
        return fields[PEAK_TEXTURE_BYTES];
    }

    public long peakStagingBytes() {
        return fields[PEAK_STAGING_BYTES];
    }

    public long peakPipelines() {
        return fields[PEAK_PIPELINES];
    }

    public long retiredResources() {
        return fields[RETIRED_RESOURCES];
    }

    public long resourceGeneration() {
        return fields[RESOURCE_GENERATION];
    }
}
