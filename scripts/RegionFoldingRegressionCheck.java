package eu.pro.dbeaver.indentfolding;

import java.util.List;

/**
 * Dependency-free regression checks for the region folding parser/calculator.
 *
 * CI compiles this together with the three pure-Java folding parser classes
 * before the Tycho build. It intentionally has no JUnit or DBeaver dependency.
 */
public final class RegionFoldingRegressionCheck {
    private RegionFoldingRegressionCheck() {
    }

    public static void main(String[] args) {
        closingMarkerMustRemainOutsideFold();
        deeperClosingMarkerMustNotCreateCompetingIndentFold();
        nestedRegionsMustKeepBothClosingMarkersOutsideTheirOwnFolds();
        markerMatchingMustRemainCaseInsensitive();
        System.out.println("Region folding regression checks passed.");
    }

    private static void closingMarkerMustRemainOutsideFold() {
        String sql = """
            #region VARIABLES
                DECLARE a INT;
            #endregion

            SELECT 1;
            """;

        int start = sql.indexOf("#region VARIABLES");
        int endMarker = sql.indexOf("#endregion");

        List<IndentationFoldParser.FoldRegion> regions =
            FoldingRegionCalculator.parse(sql, 4, true, true);

        IndentationFoldParser.FoldRegion region = onlyRegionStartingAt(regions, start);
        assertEquals(endMarker - start, region.length(), "explicit region must end at #endregion line start");

        String foldedRange = sql.substring(region.offset(), region.offset() + region.length());
        assertFalse(foldedRange.contains("#endregion"), "#endregion must not be inside the collapsed range");
    }

    private static void deeperClosingMarkerMustNotCreateCompetingIndentFold() {
        String sql = """
            #region PREPARE VARIABLES
                SET a = 1;
                    #endregion

            #region NEXT BLOCK
                SELECT 1;
            #endregion
            """;

        int firstStart = sql.indexOf("#region PREPARE VARIABLES");
        int firstEndMarker = sql.indexOf("#endregion");
        int firstEndLineStart = lineStart(sql, firstEndMarker);
        int nextStart = sql.indexOf("#region NEXT BLOCK");

        List<IndentationFoldParser.FoldRegion> regions =
            FoldingRegionCalculator.parse(sql, 4, true, true);

        List<IndentationFoldParser.FoldRegion> anchored = regions.stream()
            .filter(region -> region.offset() == firstStart)
            .toList();

        assertEquals(1, anchored.size(), "marker line must have exactly one fold annotation");

        IndentationFoldParser.FoldRegion region = anchored.getFirst();
        assertEquals(
            firstEndLineStart - firstStart,
            region.length(),
            "deeper #endregion must not extend the fold to the next dedent"
        );
        assertTrue(
            region.offset() + region.length() < nextStart,
            "blank lines after #endregion must remain outside the collapsed region"
        );
    }

    private static void nestedRegionsMustKeepBothClosingMarkersOutsideTheirOwnFolds() {
        String sql = """
            #region OUTER
                #region INNER
                    SELECT 1;
                    #endregion
                SELECT 2;
            #endregion
            """;

        int outerStart = lineStart(sql, sql.indexOf("#region OUTER"));
        int innerStart = lineStart(sql, sql.indexOf("#region INNER"));
        int innerEnd = lineStart(sql, sql.indexOf("#endregion"));
        int outerEnd = lineStart(sql, sql.lastIndexOf("#endregion"));

        List<IndentationFoldParser.FoldRegion> regions =
            FoldingRegionCalculator.parse(sql, 4, true, true);

        IndentationFoldParser.FoldRegion inner = onlyRegionStartingAt(regions, innerStart);
        IndentationFoldParser.FoldRegion outer = onlyRegionStartingAt(regions, outerStart);

        assertEquals(innerEnd - innerStart, inner.length(), "inner #endregion must stay visible");
        assertEquals(outerEnd - outerStart, outer.length(), "outer #endregion must stay visible");
    }

    private static void markerMatchingMustRemainCaseInsensitive() {
        String sql = """
            #REGION CASE TEST
                SELECT 1;
            #ENDREGION
            """;

        int start = sql.indexOf("#REGION");
        int end = sql.indexOf("#ENDREGION");

        IndentationFoldParser.FoldRegion region = onlyRegionStartingAt(
            FoldingRegionCalculator.parse(sql, 4, true, true),
            start
        );

        assertEquals(end - start, region.length(), "case-insensitive markers must still fold");
    }

    private static int lineStart(String text, int offset) {
        int previousNewline = text.lastIndexOf('\n', Math.max(0, offset - 1));
        return previousNewline < 0 ? 0 : previousNewline + 1;
    }

    private static IndentationFoldParser.FoldRegion onlyRegionStartingAt(
        List<IndentationFoldParser.FoldRegion> regions,
        int offset
    ) {
        List<IndentationFoldParser.FoldRegion> anchored = regions.stream()
            .filter(region -> region.offset() == offset)
            .toList();

        if (anchored.size() != 1) {
            throw new AssertionError(
                "Expected exactly one fold at offset " + offset + ", got " + anchored.size() + ": " + anchored
            );
        }
        return anchored.getFirst();
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }
}
