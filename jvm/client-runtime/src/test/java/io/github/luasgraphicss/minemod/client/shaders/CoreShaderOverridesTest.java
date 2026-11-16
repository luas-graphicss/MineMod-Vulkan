package io.github.luasgraphicss.minemod.client.shaders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.bridge.CoreShaderSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoreShaderOverridesTest {
    private static final String VERTEX = "#version 150\n#moj_import <fog.glsl>\nvoid main() {}\n";
    private static final String FRAGMENT = "#version 150\nout vec4 fragColor;\nvoid main() {}\n";
    private static final String FOG = "#moj_import <light.glsl>\nfloat fog() { return 1.0; }\n";
    private static final String LIGHT = "float light() { return 1.0; }\n";
    private static final ProgramStages SOLID =
            new ProgramStages("rendertype_solid", "rendertype_solid", "rendertype_solid", false);

    private final PackFiles files = new PackFiles()
            .builtIn("shaders/core/rendertype_solid.vsh", VERTEX)
            .builtIn("shaders/core/rendertype_solid.fsh", FRAGMENT)
            .builtIn("shaders/include/fog.glsl", FOG)
            .builtIn("shaders/include/light.glsl", LIGHT);

    @Test
    void leavesAProgramMadeOfBuiltInFilesToTheRenderer() {
        assertEquals(Collections.emptyList(), namesOf(collect(SOLID)));
    }

    @Test
    void collectsAProgramWhoseStageAPackReplaces() {
        files.replaced("shaders/core/rendertype_solid.fsh", "#version 150\nvoid main() { discard; }\n");

        List<CoreShaderSource> overrides = collect(SOLID);

        assertEquals(Collections.singletonList("rendertype_solid"), namesOf(overrides));
        assertEquals(VERTEX, overrides.get(0).vertex());
        assertEquals("#version 150\nvoid main() { discard; }\n", overrides.get(0).fragment());
    }

    @Test
    void collectsAProgramWhoseNestedImportAPackReplaces() {
        files.replaced("shaders/include/light.glsl", "float light() { return 0.5; }\n");

        List<CoreShaderSource> overrides = collect(SOLID);

        assertEquals(Collections.singletonList("rendertype_solid"), namesOf(overrides));
        assertEquals(
                Arrays.asList("<fog.glsl>", "<light.glsl>"),
                new ArrayList<>(overrides.get(0).includes().keySet()));
        assertEquals("float light() { return 0.5; }\n", overrides.get(0).includes().get("<light.glsl>"));
    }

    @Test
    void collectsAProgramAPackRedefinesWithTheStagesItNames() {
        files.builtIn("shaders/core/position.vsh", "#version 150\nvoid main() {}\n");
        ProgramStages redefined = new ProgramStages("rendertype_solid", "position", "rendertype_solid", true);

        List<CoreShaderSource> overrides = collect(redefined);

        assertEquals(Collections.singletonList("rendertype_solid"), namesOf(overrides));
        assertEquals("#version 150\nvoid main() {}\n", overrides.get(0).vertex());
        assertEquals(Collections.emptySet(), overrides.get(0).includes().keySet());
    }

    @Test
    void leavesAProgramWithAMissingStageToTheRenderer() {
        ProgramStages broken = new ProgramStages("rendertype_solid", "rendertype_solid", "absent", true);

        assertEquals(Collections.emptyList(), namesOf(collect(broken)));
    }

    @Test
    void stopsFollowingImportsThatImportEachOther() {
        files.replaced("shaders/include/fog.glsl", "#moj_import <light.glsl>\n")
                .builtIn("shaders/include/light.glsl", "#moj_import <fog.glsl>\n");

        List<CoreShaderSource> overrides = collect(SOLID);

        assertEquals(
                Arrays.asList("<fog.glsl>", "<light.glsl>"),
                new ArrayList<>(overrides.get(0).includes().keySet()));
    }

    private List<CoreShaderSource> collect(ProgramStages program) {
        return CoreShaderOverrides.collect(files, Collections.singletonList(program));
    }

    private static List<String> namesOf(List<CoreShaderSource> overrides) {
        List<String> names = new ArrayList<>();
        for (CoreShaderSource override : overrides) {
            names.add(override.name());
        }
        return names;
    }
}
