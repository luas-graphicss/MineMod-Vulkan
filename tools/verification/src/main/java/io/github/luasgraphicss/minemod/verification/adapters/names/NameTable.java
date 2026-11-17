package io.github.luasgraphicss.minemod.verification.adapters.names;

import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NameTable {
    private static final Pattern OBJECT_TYPE = Pattern.compile("L([^;]+);");

    private final Map<String, String> classNames;
    private final Map<String, String> methodNames;

    public NameTable(Map<String, String> classNames, Map<String, String> methodNames) {
        this.classNames = Map.copyOf(classNames);
        this.methodNames = Map.copyOf(methodNames);
    }

    public static String methodKey(String owner, String name, String descriptor) {
        return owner + "." + name + descriptor;
    }

    public MethodTarget translate(MethodTarget official) {
        return new MethodTarget(
                classOf(official.owner()), methodNameOf(official), descriptorOf(official.descriptor()));
    }

    public String methodNameOf(MethodTarget official) {
        String key = methodKey(official.owner(), official.name(), official.descriptor());
        return methodNames.getOrDefault(key, official.name());
    }

    private String classOf(String officialClass) {
        return classNames.getOrDefault(officialClass, officialClass);
    }

    private String descriptorOf(String officialDescriptor) {
        Matcher objectType = OBJECT_TYPE.matcher(officialDescriptor);
        StringBuilder translated = new StringBuilder();
        while (objectType.find()) {
            objectType.appendReplacement(translated, Matcher.quoteReplacement("L" + classOf(objectType.group(1)) + ";"));
        }
        objectType.appendTail(translated);
        return translated.toString();
    }
}
