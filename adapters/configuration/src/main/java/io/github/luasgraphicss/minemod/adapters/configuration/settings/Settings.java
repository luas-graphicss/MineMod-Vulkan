package io.github.luasgraphicss.minemod.adapters.configuration.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Settings {
    private final Map<String, String> stored;
    private final List<String> rejected;

    private Settings(Map<String, String> stored, List<String> rejected) {
        this.stored = Collections.unmodifiableMap(stored);
        this.rejected = Collections.unmodifiableList(rejected);
    }

    public static Settings resolve(Map<String, String> stored, Collection<Setting<?>> schema) {
        List<String> rejected = new ArrayList<>();
        for (Setting<?> setting : schema) {
            String text = stored.get(setting.key());
            if (text != null && !setting.parse(text).isPresent()) {
                rejected.add(setting.key() + " is \"" + text + "\", expected " + setting.accepted() + "; using "
                        + setting.fallback());
            }
        }
        return new Settings(new TreeMap<>(stored), rejected);
    }

    public <T> T get(Setting<T> setting) {
        String text = stored.get(setting.key());
        return text == null ? setting.fallback() : setting.parse(text).orElse(setting.fallback());
    }

    public <T> Settings with(Setting<T> setting, T value) {
        Map<String, String> changed = new TreeMap<>(stored);
        changed.put(setting.key(), String.valueOf(value));
        return new Settings(changed, new ArrayList<>(rejected));
    }

    public Map<String, String> stored() {
        return stored;
    }

    public List<String> rejected() {
        return rejected;
    }

    public List<String> describe(Collection<Setting<?>> schema) {
        List<String> entries = new ArrayList<>();
        for (Setting<?> setting : schema) {
            entries.add(setting.key() + "=" + get(setting));
        }
        return entries;
    }
}
