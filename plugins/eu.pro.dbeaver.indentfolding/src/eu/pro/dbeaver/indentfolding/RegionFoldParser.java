package eu.pro.dbeaver.indentfolding;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Parses explicit MySQL-style region comments:
 *
 *   #region Optional name
 *   ...
 *   #endregion
 *
 * Markers are case-insensitive, may be indented, and may be nested.
 * Only matched pairs produce a folding region.
 */
final class RegionFoldParser {
    private static final String REGION_START = "#region";
    private static final String REGION_END = "#endregion";

    private record OpenRegion(int offset) {
    }

    private RegionFoldParser() {
    }

    static List<IndentationFoldParser.FoldRegion> parse(String text) {
        List<IndentationFoldParser.FoldRegion> regions = new ArrayList<>();
        Deque<OpenRegion> stack = new ArrayDeque<>();

        int offset = 0;
        while (offset < text.length()) {
            int lineStart = offset;

            while (offset < text.length()) {
                char ch = text.charAt(offset);
                if (ch == '\r' || ch == '\n') {
                    break;
                }
                offset++;
            }

            int contentEnd = offset;
            if (offset < text.length()) {
                if (text.charAt(offset) == '\r'
                    && offset + 1 < text.length()
                    && text.charAt(offset + 1) == '\n') {
                    offset += 2;
                } else {
                    offset++;
                }
            }

            int lineEnd = offset;
            String line = text.substring(lineStart, contentEnd).strip();

            if (isMarker(line, REGION_END)) {
                OpenRegion open = stack.pollFirst();
                if (open != null && lineEnd > open.offset()) {
                    regions.add(new IndentationFoldParser.FoldRegion(
                        open.offset(),
                        lineEnd - open.offset()
                    ));
                }
            } else if (isMarker(line, REGION_START)) {
                stack.addFirst(new OpenRegion(lineStart));
            }
        }

        return regions;
    }

    private static boolean isMarker(String line, String marker) {
        if (line.length() < marker.length()
            || !line.regionMatches(true, 0, marker, 0, marker.length())) {
            return false;
        }

        if (line.length() == marker.length()) {
            return true;
        }

        return Character.isWhitespace(line.charAt(marker.length()));
    }
}
