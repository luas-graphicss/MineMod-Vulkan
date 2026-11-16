package io.github.luasgraphicss.minemod.compatibility.dispatch.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class CatalogResource {
    private CatalogResource() {
    }

    static List<String> lines(String name) {
        InputStream resource = CatalogResource.class.getResourceAsStream(name);
        if (resource == null) {
            throw new IllegalStateException("the catalog resource is not packaged: " + name);
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return lines;
    }
}
