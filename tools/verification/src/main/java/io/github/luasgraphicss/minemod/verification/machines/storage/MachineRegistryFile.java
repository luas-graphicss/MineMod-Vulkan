package io.github.luasgraphicss.minemod.verification.machines.storage;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.luasgraphicss.minemod.verification.json.RepositoryJson;
import io.github.luasgraphicss.minemod.verification.machines.model.MachineRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MachineRegistryFile {
    private static final String REPOSITORY_PATH = "manifests/reference-machines.json";
    private static final long MAX_BYTES = 1024L * 1024;

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.FAIL))
            .build();

    private MachineRegistryFile() {}

    public static MachineRegistry readFrom(Path repositoryRoot) throws IOException {
        Path path = repositoryRoot.resolve(REPOSITORY_PATH);
        if (Files.size(path) > MAX_BYTES) {
            throw new IOException(path + " exceeds " + MAX_BYTES + " bytes");
        }
        return MAPPER.readValue(path.toFile(), MachineRegistry.class);
    }

    public static void writeTo(Path repositoryRoot, MachineRegistry registry) throws IOException {
        RepositoryJson.write(repositoryRoot.resolve(REPOSITORY_PATH), registry);
    }
}
