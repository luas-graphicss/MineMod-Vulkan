package io.github.luasgraphicss.minemod.adapters.shaderpacks.directives;

final class GlslComments {
    private static final String LINE_COMMENT = "//";
    private static final String BLOCK_OPEN = "/*";
    private static final String BLOCK_CLOSE = "*/";

    private GlslComments() {}

    static String strip(String source) {
        StringBuilder code = new StringBuilder(source.length());
        int index = 0;
        while (index < source.length()) {
            if (source.startsWith(LINE_COMMENT, index)) {
                int lineEnd = source.indexOf('\n', index);
                index = lineEnd < 0 ? source.length() : lineEnd;
            } else if (source.startsWith(BLOCK_OPEN, index)) {
                int close = source.indexOf(BLOCK_CLOSE, index + BLOCK_OPEN.length());
                index = close < 0 ? source.length() : close + BLOCK_CLOSE.length();
                code.append(' ');
            } else {
                code.append(source.charAt(index++));
            }
        }
        return code.toString();
    }
}
