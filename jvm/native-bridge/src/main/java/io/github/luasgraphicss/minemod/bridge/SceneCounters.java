package io.github.luasgraphicss.minemod.bridge;

public final class SceneCounters {
    static final int FIELD_COUNT = 10;

    private static final int FRAMES_DRAWN = 0;
    private static final int FRAMES_PRESENTED = 1;
    private static final int BATCHES_DRAWN = 2;
    private static final int SECTIONS_PUBLISHED = 3;
    private static final int SECTIONS_DROPPED = 4;
    private static final int LIVE_SECTIONS = 5;
    private static final int LIVE_TEXTURES = 6;
    private static final int LIVE_TEXTURE_BYTES = 7;
    private static final int PEAK_UPLOAD_BYTES = 8;
    private static final int PEAK_GEOMETRY_BYTES = 9;

    private final long[] fields;

    SceneCounters(long[] fields) {
        if (fields.length != FIELD_COUNT) {
            throw new IllegalArgumentException("scene counters have " + FIELD_COUNT + " fields, found " + fields.length);
        }
        this.fields = fields.clone();
    }

    public long framesDrawn() {
        return fields[FRAMES_DRAWN];
    }

    public long framesPresented() {
        return fields[FRAMES_PRESENTED];
    }

    public long batchesDrawn() {
        return fields[BATCHES_DRAWN];
    }

    public long sectionsPublished() {
        return fields[SECTIONS_PUBLISHED];
    }

    public long sectionsDropped() {
        return fields[SECTIONS_DROPPED];
    }

    public long liveSections() {
        return fields[LIVE_SECTIONS];
    }

    public long liveTextures() {
        return fields[LIVE_TEXTURES];
    }

    public long liveTextureBytes() {
        return fields[LIVE_TEXTURE_BYTES];
    }

    public long peakUploadBytes() {
        return fields[PEAK_UPLOAD_BYTES];
    }

    public long peakGeometryBytes() {
        return fields[PEAK_GEOMETRY_BYTES];
    }
}
