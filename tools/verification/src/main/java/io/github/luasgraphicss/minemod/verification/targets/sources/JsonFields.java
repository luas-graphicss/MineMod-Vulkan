package io.github.luasgraphicss.minemod.verification.targets.sources;

import com.fasterxml.jackson.databind.JsonNode;

final class JsonFields {
    private JsonFields() {}

    static JsonNode require(JsonNode parent, String field) throws MetadataFormatException {
        JsonNode value = parent.get(field);
        if (value == null || value.isNull()) {
            throw new MetadataFormatException("metadata is missing the field '" + field + "'");
        }
        return value;
    }

    static String text(JsonNode parent, String field) throws MetadataFormatException {
        JsonNode value = require(parent, field);
        if (!value.isTextual()) {
            throw new MetadataFormatException("metadata field '" + field + "' is not text");
        }
        return value.asText();
    }
}
