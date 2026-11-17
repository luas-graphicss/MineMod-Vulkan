package io.github.luasgraphicss.minemod.adapters.ownership.catalog;

import io.github.luasgraphicss.minemod.adapters.ownership.claim.RendererIdentity;
import io.github.luasgraphicss.minemod.adapters.ownership.claim.StageClaim;
import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

public final class RendererCatalog {
    private static final String BUNDLED_FAMILIES = "renderer-families.properties";
    private static final String STAGE_SEPARATOR = ",";

    private final Map<String, Set<PipelineStage>> stagesByModId;

    public RendererCatalog(Map<String, ? extends Collection<PipelineStage>> stagesByModId) {
        Map<String, Set<PipelineStage>> families = new TreeMap<>();
        for (Map.Entry<String, ? extends Collection<PipelineStage>> family : stagesByModId.entrySet()) {
            if (family.getValue().isEmpty()) {
                throw new IllegalArgumentException("renderer " + family.getKey() + " replaces no stage");
            }
            families.put(family.getKey(), Collections.unmodifiableSet(EnumSet.copyOf(family.getValue())));
        }
        this.stagesByModId = Collections.unmodifiableMap(families);
    }

    public static RendererCatalog bundled() {
        Properties families = new Properties();
        try (InputStream resource = RendererCatalog.class.getResourceAsStream(BUNDLED_FAMILIES)) {
            if (resource == null) {
                throw new IllegalStateException("the artifact carries no " + BUNDLED_FAMILIES);
            }
            families.load(resource);
        } catch (IOException exception) {
            throw new UncheckedIOException("cannot read " + BUNDLED_FAMILIES, exception);
        }
        Map<String, Set<PipelineStage>> stagesByModId = new TreeMap<>();
        for (String modId : families.stringPropertyNames()) {
            stagesByModId.put(modId, stagesNamedBy(families.getProperty(modId)));
        }
        return new RendererCatalog(stagesByModId);
    }

    public RendererCatalog with(String modId, Collection<PipelineStage> stages) {
        Map<String, Collection<PipelineStage>> families = new TreeMap<String, Collection<PipelineStage>>(stagesByModId);
        families.put(modId, stages);
        return new RendererCatalog(families);
    }

    public Set<String> rendererIds() {
        return stagesByModId.keySet();
    }

    public List<StageClaim> claimsOf(Map<String, String> installedModVersions) {
        List<StageClaim> claims = new ArrayList<>();
        for (Map.Entry<String, Set<PipelineStage>> family : stagesByModId.entrySet()) {
            String version = installedModVersions.get(family.getKey());
            if (version == null) {
                continue;
            }
            RendererIdentity renderer = new RendererIdentity(family.getKey(), version);
            for (PipelineStage stage : family.getValue()) {
                claims.add(new StageClaim(renderer, stage));
            }
        }
        return claims;
    }

    private static Set<PipelineStage> stagesNamedBy(String list) {
        Set<PipelineStage> stages = EnumSet.noneOf(PipelineStage.class);
        for (String name : list.split(STAGE_SEPARATOR)) {
            stages.add(PipelineStage.valueOf(name.trim()));
        }
        return stages;
    }
}
