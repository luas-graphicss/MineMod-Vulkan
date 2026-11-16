package io.github.luasgraphicss.minemod.compatibility.contracts.report;

public final class OriginLabel {
    public static final String UNKNOWN = "unknown";

    private static final char ARCHIVE_ENTRY_MARK = '!';

    private OriginLabel() {
    }

    public static String of(String location) {
        int archiveEnd = location.indexOf(ARCHIVE_ENTRY_MARK);
        String archive = archiveEnd < 0 ? location : location.substring(0, archiveEnd);
        int end = archive.length();
        while (end > 0 && isSeparator(archive.charAt(end - 1))) {
            end--;
        }
        int start = end;
        while (start > 0 && !isSeparator(archive.charAt(start - 1))) {
            start--;
        }
        return start == end ? UNKNOWN : archive.substring(start, end);
    }

    private static boolean isSeparator(char character) {
        return character == '/' || character == '\\';
    }
}
