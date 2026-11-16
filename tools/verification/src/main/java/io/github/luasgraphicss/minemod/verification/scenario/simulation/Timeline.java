package io.github.luasgraphicss.minemod.verification.scenario.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Timeline {
    private final List<Event> events = new ArrayList<>();

    public void add(Source source, String text) {
        events.add(new Event(source, text));
    }

    public List<String> all() {
        return events.stream().map(Event::toString).toList();
    }

    public List<String> from(Source source) {
        return events.stream().filter(event -> event.source() == source).map(Event::text).toList();
    }

    public int size() {
        return events.size();
    }

    public enum Source {
        NATIVE,
        PROVIDER,
        HANDOFF
    }

    private record Event(Source source, String text) {
        @Override
        public String toString() {
            return source.name().toLowerCase(Locale.ROOT) + ": " + text;
        }
    }
}
