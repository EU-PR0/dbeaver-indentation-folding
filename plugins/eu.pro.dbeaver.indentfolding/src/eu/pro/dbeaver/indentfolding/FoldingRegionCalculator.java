package eu.pro.dbeaver.indentfolding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines indentation-based and explicit-region folding.
 *
 * Explicit regions have priority when an indentation fold would cross a
 * region boundary. Properly nested ranges from both mechanisms are retained.
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
        List<IndentationFoldParser.FoldRegion> explicitRegions = regionsEnabled
            ? RegionFoldParser.parse(text)
            : List.of();

        List<IndentationFoldParser.FoldRegion> indentationRegions = indentationEnabled
            ? IndentationFoldParser.parse(text, tabWidth)
            : List.of();

        Map<RegionKey, IndentationFoldParser.FoldRegion> merged = new LinkedHashMap<>();

        for (IndentationFoldParser.FoldRegion region : explicitRegions) {
            merged.put(new RegionKey(region.offset(), region.length()), region);
        }

        for (IndentationFoldParser.FoldRegion region : indentationRegions) {
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
