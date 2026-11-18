package io.github.luasgraphicss.minemod.verification.machines.probe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.machines.probe.SuiteResults.SuiteOutcome;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SuiteResultsTest {
    @TempDir
    Path results;

    @Test
    void readsTheCountsOfEverySuiteUnderTheDirectory() throws IOException {
        writeReport("glfw/test/TEST-example.WindowTest.xml", suite("example.WindowTest", 2, 0, 0, 0));
        writeReport("runtime/test/TEST-example.SceneTest.xml", suite("example.SceneTest", 5, 1, 0, 0));

        Map<String, SuiteOutcome> outcomes = SuiteResults.readFrom(results);

        assertEquals(
                Map.of(
                        "example.WindowTest", new SuiteOutcome(2, 0, 0, 0),
                        "example.SceneTest", new SuiteOutcome(5, 1, 0, 0)),
                outcomes);
        assertTrue(outcomes.get("example.WindowTest").ranEveryTest());
        assertFalse(outcomes.get("example.SceneTest").ranEveryTest());
    }

    @Test
    void addsUpTheRunsOfOneSuiteOnSeveralRuntimes() throws IOException {
        writeReport("test/TEST-example.WindowTest.xml", suite("example.WindowTest", 1, 0, 0, 0));
        writeReport("testOnJava21/TEST-example.WindowTest.xml", suite("example.WindowTest", 1, 1, 0, 0));

        assertEquals(
                Map.of("example.WindowTest", new SuiteOutcome(2, 1, 0, 0)), SuiteResults.readFrom(results));
    }

    @Test
    void aSuiteWithoutTestsDidNotRunEveryTest() {
        assertFalse(new SuiteOutcome(0, 0, 0, 0).ranEveryTest());
    }

    @Test
    void ignoresFilesThatAreNotSuiteReports() throws IOException {
        writeReport("test/binary/output.bin", "not a report");
        writeReport("test/notes.xml", "<notes/>");

        assertEquals(Map.of(), SuiteResults.readFrom(results));
    }

    @Test
    void rejectsAReportThatDeclaresADocumentType() throws IOException {
        writeReport("test/TEST-example.WindowTest.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE testsuite [<!ENTITY secret SYSTEM "file:///etc/hostname">]>
                <testsuite name="&secret;" tests="1" skipped="0" failures="0" errors="0"/>
                """);

        assertThrows(IOException.class, () -> SuiteResults.readFrom(results));
    }

    @Test
    void rejectsASuiteWithoutItsCounts() throws IOException {
        writeReport("test/TEST-example.WindowTest.xml", "<testsuite name=\"example.WindowTest\" tests=\"1\"/>");

        IOException refused = assertThrows(IOException.class, () -> SuiteResults.readFrom(results));

        assertTrue(refused.getMessage().endsWith("has a suite without the attribute skipped"), refused.getMessage());
    }

    @Test
    void rejectsAReportOfSomethingOtherThanASuite() throws IOException {
        writeReport("test/TEST-example.WindowTest.xml", "<testsuites/>");

        IOException refused = assertThrows(IOException.class, () -> SuiteResults.readFrom(results));

        assertTrue(refused.getMessage().endsWith("is not a JUnit suite report"), refused.getMessage());
    }

    private void writeReport(String relativePath, String content) throws IOException {
        Path report = results.resolve(relativePath);
        Files.createDirectories(report.getParent());
        Files.writeString(report, content);
    }

    private static String suite(String name, int tests, int skipped, int failures, int errors) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="%s" tests="%d" skipped="%d" failures="%d" errors="%d" time="0.5">
                  <properties/>
                  <testcase name="opens" classname="%s" time="0.5"/>
                </testsuite>
                """.formatted(name, tests, skipped, failures, errors, name);
    }
}
