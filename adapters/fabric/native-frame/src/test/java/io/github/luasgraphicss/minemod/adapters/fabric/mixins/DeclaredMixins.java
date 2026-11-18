package io.github.luasgraphicss.minemod.adapters.fabric.mixins;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

final class DeclaredMixins {
    private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String CLASS_TARGETS = "value";
    private static final String NAMED_TARGETS = "targets";
    private static final String CLASS_FILE_SUFFIX = ".class";

    private DeclaredMixins() {}

    static List<DeclaredMixin> listedIn(String configName, ClassLoader loader) throws IOException {
        JsonObject config = read(configName, loader);
        String mixinPackage = config.get("package").getAsString();
        List<DeclaredMixin> mixins = new ArrayList<>();
        for (JsonElement listed : config.getAsJsonArray("client")) {
            String name = mixinPackage + "." + listed.getAsString();
            mixins.add(new DeclaredMixin(name, targetsOf(name, loader)));
        }
        return mixins;
    }

    private static JsonObject read(String configName, ClassLoader loader) throws IOException {
        try (Reader config = new InputStreamReader(open(configName, loader), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(config, JsonObject.class);
        }
    }

    private static List<String> targetsOf(String mixinName, ClassLoader loader) throws IOException {
        ClassNode mixin = new ClassNode();
        try (InputStream classFile = open(mixinName.replace('.', '/') + CLASS_FILE_SUFFIX, loader)) {
            new ClassReader(classFile).accept(mixin, ClassReader.SKIP_CODE);
        }
        List<String> targets = new ArrayList<>();
        for (AnnotationNode annotation : mixin.invisibleAnnotations) {
            if (annotation.desc.equals(MIXIN_ANNOTATION)) {
                collectTargets(annotation, targets);
            }
        }
        return targets;
    }

    private static void collectTargets(AnnotationNode mixin, List<String> targets) {
        for (int index = 0; index < mixin.values.size(); index += 2) {
            Object name = mixin.values.get(index);
            List<?> listed = (List<?>) mixin.values.get(index + 1);
            if (CLASS_TARGETS.equals(name)) {
                listed.forEach(target -> targets.add(((Type) target).getClassName()));
            } else if (NAMED_TARGETS.equals(name)) {
                listed.forEach(target -> targets.add(((String) target).replace('/', '.')));
            }
        }
    }

    private static InputStream open(String resource, ClassLoader loader) throws IOException {
        InputStream stream = loader.getResourceAsStream(resource);
        if (stream == null) {
            throw new IOException("the game adapter does not carry " + resource);
        }
        return stream;
    }
}
