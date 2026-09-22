package eu.pro.dbeaver.indentfolding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Combines indentation-based and explicit-region folding.
 *
 * Explicit regions have priority when an indentation fold would cross a
 * region boundary. Indentation folds anchored on a #region or #endregion
 * marker line are suppressed so an indentation fold cannot compete with the
 * explicit region fold or accidentally swallow the closing marker/blank lines.
 */
final class FoldingRegionCalculator {
    private record RegionKey(int offset, int length) {
    }

    private FoldingRegionCalculator() {
    }

    static List<IndentationFoldParser.FoldRegion> parse(
        String text,
        int tabWidth,
        boolean indentationEnabled,
        boolean regionsEnabled
    ) {
        RegionFoldParser.ParseResult parsedRegions = regionsEnabled
            ? RegionFoldParser.parse(text)
            : new RegionFoldParser.ParseResult(List.of(), Set.of());

        List<IndentationFoldParser.FoldRegion> explicitRegions = parsedRegions.regions();
        Set<Integer> markerLineOffsets = parsedRegions.markerLineOffsets();

        List<IndentationFoldParser.FoldRegion> indentationRegions = indentationEnabled
            ? IndentationFoldParser.parse(text, tabWidth)
            : List.of();

        Map<RegionKey, IndentationFoldParser.FoldRegion> merged = new LinkedHashMap<>();

        for (IndentationFoldParser.FoldRegion region : explicitRegions) {
            merged.put(new RegionKey(region.offset(), region.length()), region);
        }

        for (IndentationFoldParser.FoldRegion region : indentationRegions) {
            // A marker line is owned by explicit region folding. In particular,
            // when #endregion is indented deeper than #region, indentation
            // folding otherwise creates a second region starting at #region
            // and extending to the next dedent. Collapsing that competing fold
            // hides #endregion and the blank lines after it.
            if (regionsEnabled && markerLineOffsets.contains(region.offset())) {
                continue;
            }

            boolean crossesExplicitRegion = false;
            for (IndentationFoldParser.FoldRegion explicit : explicitRegions) {
                if (crosses(region, explicit)) {
                    crossesExplicitRegion = true;
                    break;
                }
            }
            if (!crossesExplicitRegion) {
                merged.putIfAbsent(new RegionKey(region.offset(), region.length()), region);
            }
        }

        List<IndentationFoldParser.FoldRegion> result = new ArrayList<>(merged.values());
        result.sort(
            Comparator.comparingInt(IndentationFoldParser.FoldRegion::offset)
                .thenComparing(
                    Comparator.comparingInt(IndentationFoldParser.FoldRegion::length).reversed()
                )
        );
        return result;
    }

    private static boolean crosses(
        IndentationFoldParser.FoldRegion a,
        IndentationFoldParser.FoldRegion b
    ) {
        int aStart = a.offset();
        int aEnd = aStart + a.length();
        int bStart = b.offset();
        int bEnd = bStart + b.length();

        return (aStart < bStart && bStart < aEnd && aEnd < bEnd)
            || (bStart < aStart && aStart < bEnd && bEnd < aEnd);
    }
}
