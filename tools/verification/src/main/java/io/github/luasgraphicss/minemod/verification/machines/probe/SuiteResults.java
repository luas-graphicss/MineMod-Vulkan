package io.github.luasgraphicss.minemod.verification.machines.probe;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public final class SuiteResults {
    public record SuiteOutcome(int tests, int skipped, int failures, int errors) {
        public boolean ranEveryTest() {
            return tests > 0 && skipped == 0 && failures == 0 && errors == 0;
        }

        SuiteOutcome plus(SuiteOutcome other) {
            return new SuiteOutcome(
                    tests + other.tests, skipped + other.skipped, failures + other.failures, errors + other.errors);
        }
    }

    private static final String FILE_PREFIX = "TEST-";
    private static final String FILE_SUFFIX = ".xml";
    private static final String SUITE_ELEMENT = "testsuite";

    private SuiteResults() {}

    public static Map<String, SuiteOutcome> readFrom(Path directory) throws IOException {
        Map<String, SuiteOutcome> outcomes = new TreeMap<>();
        for (Path report : reportsUnder(directory)) {
            readSuite(report, outcomes);
        }
        return outcomes;
    }

    private static List<Path> reportsUnder(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(SuiteResults::isSuiteReport).sorted().toList();
        }
    }

    private static boolean isSuiteReport(Path file) {
        String name = file.getFileName().toString();
        return name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX) && Files.isRegularFile(file);
    }

    private static void readSuite(Path report, Map<String, SuiteOutcome> outcomes) throws IOException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try (InputStream content = Files.newInputStream(report)) {
            XMLStreamReader reader = factory.createXMLStreamReader(content);
            try {
                reader.nextTag();
                if (!reader.getLocalName().equals(SUITE_ELEMENT)) {
                    throw new IOException(report + " is not a JUnit suite report");
                }
                SuiteOutcome outcome = new SuiteOutcome(
                        count(reader, report, "tests"),
                        count(reader, report, "skipped"),
                        count(reader, report, "failures"),
                        count(reader, report, "errors"));
                outcomes.merge(attribute(reader, report, "name"), outcome, SuiteOutcome::plus);
            } finally {
                reader.close();
            }
        } catch (XMLStreamException malformed) {
            throw new IOException(report + " is not a readable suite report: " + malformed.getMessage(), malformed);
        }
    }

    private static int count(XMLStreamReader reader, Path report, String name) throws IOException {
        String value = attribute(reader, report, name);
        try {
            return Integer.parseUnsignedInt(value);
        } catch (NumberFormatException malformed) {
            throw new IOException(report + " has " + name + "=\"" + value + "\", expected a count", malformed);
        }
    }

    private static String attribute(XMLStreamReader reader, Path report, String name) throws IOException {
        String value = reader.getAttributeValue(null, name);
        if (value == null) {
            throw new IOException(report + " has a suite without the attribute " + name);
        }
        return value;
    }
}
