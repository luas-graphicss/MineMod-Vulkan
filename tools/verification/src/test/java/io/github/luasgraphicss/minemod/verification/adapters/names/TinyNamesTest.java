package io.github.luasgraphicss.minemod.verification.adapters.names;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class TinyNamesTest {
    private static final String NAMES = String.join(
            "\n",
            "tiny\t2\t0\tofficial\tintermediary",
            "c\tcfs\tnet/minecraft/class_1041",
            "\tm\t(Z)V\ta\tmethod_15998",
            "\t\tp\t0\tfullscreen",
            "\tf\tJ\tb\tfield_5187",
            "c\tcfi\tnet/minecraft/class_3678",
            "");

    @Test
    void translatesTheClassesAndTheMethodsOfAnIntermediaryFile() throws IOException {
        NameTable names = TinyNames.read(readerOf(NAMES));

        assertEquals(
                MethodTarget.parse("net/minecraft/class_1041.method_15998(Z)V"),
                names.translate(MethodTarget.parse("cfs.a(Z)V")));
        assertEquals(
                MethodTarget.parse("net/minecraft/class_1041.<init>(Lnet/minecraft/class_3678;)V"),
                names.translate(MethodTarget.parse("cfs.<init>(Lcfi;)V")));
    }

    @Test
    void refusesAFileThatIsNotOfficialToIntermediary() {
        IOException refusal = assertThrows(
                IOException.class, () -> TinyNames.read(readerOf("tiny\t2\t0\tintermediary\tnamed\n")));

        assertTrue(refusal.getMessage().contains("official to intermediary"), refusal.getMessage());
    }

    private static BufferedReader readerOf(String text) {
        return new BufferedReader(new StringReader(text));
    }
}
