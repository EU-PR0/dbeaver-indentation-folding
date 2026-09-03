package eu.pro.dbeaver.indentfolding;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Computes folding regions purely from visual indentation.
 * Blank lines do not open or close blocks.
 */
final class IndentationFoldParser {
    record FoldRegion(int offset, int length) {}
    private record Line(int offset, int indent, boolean blank) {}
    private record OpenBlock(int offset, int indent) {}

    private IndentationFoldParser() {
    }

    static List<FoldRegion> parse(String text, int tabWidth) {
        int effectiveTabWidth = Math.max(1, Math.min(16, tabWidth));
        List<Line> lines = scanLines(text, effectiveTabWidth);
        List<FoldRegion> regions = new ArrayList<>();
        Deque<OpenBlock> stack = new ArrayDeque<>();
        Line previousNonBlank = null;

        for (Line line : lines) {
            if (line.blank()) {
                continue;
            }

            while (!stack.isEmpty() && line.indent() <= stack.peek().indent()) {
                OpenBlock block = stack.pop();
                addRegion(regions, block.offset(), line.offset());
            }

            if (previousNonBlank != null && line.indent() > previousNonBlank.indent()) {
                stack.push(new OpenBlock(previousNonBlank.offset(), previousNonBlank.indent()));
            }

            previousNonBlank = line;
        }

        while (!stack.isEmpty()) {
            OpenBlock block = stack.pop();
            addRegion(regions, block.offset(), text.length());
        }

        return regions;
    }

    private static void addRegion(List<FoldRegion> regions, int start, int end) {
        if (end > start) {
            regions.add(new FoldRegion(start, end - start));
        }
    }

    private static List<Line> scanLines(String text, int tabWidth) {
        List<Line> lines = new ArrayList<>();
        int length = text.length();
        int offset = 0;

        while (offset < length) {
            int lineStart = offset;
            int indent = 0;
            boolean blank = true;
            boolean inIndent = true;

            while (offset < length) {
                char ch = text.charAt(offset);
                if (ch == '\r' || ch == '\n') {
                    break;
                }

                if (inIndent) {
                    if (ch == ' ') {
                        indent++;
                    } else if (ch == '\t') {
                        indent += tabWidth - (indent % tabWidth);
                    } else {
                        inIndent = false;
                        if (!Character.isWhitespace(ch)) {
                            blank = false;
                        }
                    }
                } else if (!Character.isWhitespace(ch)) {
                    blank = false;
                }
                offset++;
            }

            lines.add(new Line(lineStart, indent, blank));

            if (offset < length) {
                if (text.charAt(offset) == '\r' && offset + 1 < length && text.charAt(offset + 1) == '\n') {
                    offset += 2;
                } else {
                    offset++;
                }
            }
        }

        // Preserve a final empty line for correct end offsets only when needed. It is blank and
        // therefore cannot open/close a block, so no extra entry is necessary when text ends in EOL.
        return lines;
    }
}
