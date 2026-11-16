package io.github.luasgraphicss.minemod.compatibility.dispatch.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CallCatalogParserTest {
    @Test
    void alternativesExpandToEveryFunctionTheyName() {
        List<CallDescription> descriptions =
                CallCatalogParser.parse(Collections.singletonList("glUniform{1,2}{f,i}{,v} UNIFORM FORWARD NONE"));

        assertEquals(
                Arrays.asList(
                        "glUniform1f", "glUniform1fv", "glUniform1i", "glUniform1iv",
                        "glUniform2f", "glUniform2fv", "glUniform2i", "glUniform2iv"),
                functionsOf(descriptions));
    }

    @Test
    void nameArgumentsKeepTheirPositionAndKind() {
        CallDescription description = CallCatalogParser.parse(Collections.singletonList(
                        "glAttachShader SHADER TRANSLATE NONE 0:SHADER_OR_PROGRAM 1:SHADER_OR_PROGRAM"))
                .get(0);

        assertEquals(2, description.nameArguments().size());
        assertEquals(1, description.nameArguments().get(1).index());
        assertEquals(ObjectKind.SHADER_OR_PROGRAM, description.nameArguments().get(1).kind());
        assertEquals(Interception.TRANSLATE, description.interception());
    }

    @Test
    void aMalformedLineIsReportedWithItsNumber() {
        IllegalArgumentException missingFields = assertThrows(
                IllegalArgumentException.class,
                () -> CallCatalogParser.parse(Arrays.asList("", "glClear DRAW TRANSLATE")));
        assertEquals("line 2", missingFields.getMessage().substring(0, 6));

        assertThrows(
                IllegalArgumentException.class,
                () -> CallCatalogParser.parse(Collections.singletonList("glClear PAINT TRANSLATE DRAW")));
        assertThrows(
                IllegalArgumentException.class,
                () -> CallCatalogParser.parse(Collections.singletonList("glBindTexture TEXTURE TRANSLATE NONE 1")));
        assertThrows(
                IllegalArgumentException.class,
                () -> CallCatalogParser.parse(Collections.singletonList("glVertex{2,3 IMMEDIATE_MODE FORWARD NONE")));
    }

    @Test
    void aForwardedCallCannotAskForTranslation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CallCatalogParser.parse(Collections.singletonList("glBindTexture TEXTURE FORWARD NONE 1:TEXTURE")));
        assertThrows(
                IllegalArgumentException.class,
                () -> CallCatalogParser.parse(Collections.singletonList("glDrawArrays DRAW FORWARD DRAW")));
    }

    private static List<String> functionsOf(List<CallDescription> descriptions) {
        List<String> functions = new ArrayList<>();
        for (CallDescription description : descriptions) {
            functions.add(description.function());
        }
        return functions;
    }
}
