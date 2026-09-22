package eu.pro.dbeaver.indentfolding;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses explicit MySQL-style region comments:
 *
 *   #region Optional name
 *   ...
 *   #endregion
 *
 * Markers are case-insensitive, may be indented, and may be nested.
 * Only matched pairs produce a folding region.
 *
 * The folding range deliberately ends at the beginning of the #endregion
 * line. This keeps the closing marker visible after the region is collapsed.
 */
final class RegionFoldParser {
    private static final String REGION_START = "#region";
    private static final String REGION_END = "#endregion";

    record ParseResult(
        List<IndentationFoldParser.FoldRegion> regions,
        Set<Integer> markerLineOffsets
    ) {
        ParseResult {
            regions = List.copyOf(regions);
            markerLineOffsets = Set.copyOf(markerLineOffsets);
        }
    }

    private record OpenRegion(int offset) {
    }

    private RegionFoldParser() {
    }

    static ParseResult parse(String text) {
        List<IndentationFoldParser.FoldRegion> regions = new ArrayList<>();
        Set<Integer> markerLineOffsets = new LinkedHashSet<>();
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

            String line = text.substring(lineStart, contentEnd).strip();

            if (isMarker(line, REGION_END)) {
                markerLineOffsets.add(lineStart);

                OpenRegion open = stack.pollFirst();
                if (open != null && lineStart > open.offset()) {
                    regions.add(new IndentationFoldParser.FoldRegion(
                        open.offset(),
                        lineStart - open.offset()
                    ));
                }
            } else if (isMarker(line, REGION_START)) {
                markerLineOffsets.add(lineStart);
                stack.addFirst(new OpenRegion(lineStart));
            }
        }

        return new ParseResult(regions, markerLineOffsets);
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
