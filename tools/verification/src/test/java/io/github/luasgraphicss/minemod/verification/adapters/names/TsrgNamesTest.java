package io.github.luasgraphicss.minemod.verification.adapters.names;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.verification.adapters.names.TsrgNames.SrgNames;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class TsrgNamesTest {
    private static final String FIRST_FORMAT = String.join(
            "\n",
            "dez net/minecraft/client/MainWindow",
            "\ta field_198119_f",
            "\ta (Z)V func_216523_b",
            "dfa net/minecraft/client/renderer/IWindowEventListener",
            "");
    private static final String SECOND_FORMAT = String.join(
            "\n",
            "tsrg2 obf srg id",
            "ehn net/minecraft/src/C_3161_ 3161",
            "\ta f_85349_ 85349",
            "\ta (Z)V m_85409_ 85409",
            "\t\t0 o p_85410_ 85410",
            "");

    @Test
    void namesClassesAndMethodsInTheFirstFormat() throws IOException {
        SrgNames names = TsrgNames.read(readerOf(FIRST_FORMAT));

        assertFalse(names.leavesClassNamesToMojang());
        assertEquals(
                MethodTarget.parse("net/minecraft/client/MainWindow.func_216523_b(Z)V"),
                names.table().translate(MethodTarget.parse("dez.a(Z)V")));
        assertEquals(
                MethodTarget.parse("net/minecraft/client/MainWindow.<init>"
                        + "(Lnet/minecraft/client/renderer/IWindowEventListener;)V"),
                names.table().translate(MethodTarget.parse("dez.<init>(Ldfa;)V")));
    }

    @Test
    void namesOnlyMethodsInTheSecondFormat() throws IOException {
        SrgNames names = TsrgNames.read(readerOf(SECOND_FORMAT));

        assertTrue(names.leavesClassNamesToMojang());
        assertEquals("m_85409_", names.table().methodNameOf(MethodTarget.parse("ehn.a(Z)V")));
        assertEquals(MethodTarget.parse("ehn.m_85409_(Z)V"), names.table().translate(MethodTarget.parse("ehn.a(Z)V")));
    }

    @Test
    void refusesAFileThatNamesNoClass() {
        assertThrows(IOException.class, () -> TsrgNames.read(readerOf("tsrg2 obf srg id\n")));
    }

    private static BufferedReader readerOf(String text) {
        return new BufferedReader(new StringReader(text));
    }
}
