package io.github.luasgraphicss.minemod.verification.ecosystem.pack;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record Modpack(
        int schemaVersion, String id, String release, LoaderTarget.Kind loader, List<PackMod> mods, Expectation expectation) {
    public static final int SCHEMA_VERSION = 1;

    public enum Expectation {
        @JsonProperty("runs")
        RUNS,
        @JsonProperty("refused-before-world")
        REFUSED_BEFORE_WORLD
    }

    public record PackMod(String id, String version, ModRole role) {}

    public Modpack {
        mods = List.copyOf(mods);
    }

    public Map<String, String> installedVersions() {
        Map<String, String> versions = new LinkedHashMap<>();
        mods.forEach(mod -> versions.put(mod.id(), mod.version()));
        return versions;
    }

    public Set<ModRole> roles() {
        Set<ModRole> roles = EnumSet.noneOf(ModRole.class);
        mods.forEach(mod -> roles.add(mod.role()));
        return roles;
    }

    public Modpack only(PackMod mod) {
        return new Modpack(schemaVersion, id + " / " + mod.id() + " alone", release, loader, List.of(mod), expectation);
    }
}
