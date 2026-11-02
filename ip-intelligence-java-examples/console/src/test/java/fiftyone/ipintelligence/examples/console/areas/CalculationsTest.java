/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.ipintelligence.examples.console.areas;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CalculationsTest {

    private static final GeometryFactory factory = new GeometryFactory();

    /**
     * Tolerance applied to area assertions. The expected values were
     * calculated with a different projection library so small differences
     * are allowed.
     */
    private static void assertAreaEquals(int expected, int actual) {
        double tolerance = Math.max(2, expected * 0.02);
        assertEquals("Expected area " + expected + " but was " + actual,
                expected, actual, tolerance);
    }

    @Test
    public void testPolygonAtEquator() throws Exception {
        assertAreaEquals(12323,
                Calculations.getAreas(createRectangle(0, 0, 1), 0, 0)
                        .getSquareKms());
    }

    @Test
    public void testPolygonAtMidLatitude() throws Exception {
        assertAreaEquals(7725,
                Calculations.getAreas(createRectangle(0, 51, 1), 0, 0)
                        .getSquareKms());
    }

    @Test
    public void testPolygonAtMidLatitudeOffset() throws Exception {
        assertAreaEquals(7809,
                Calculations.getAreas(createRectangle(0, 50.5, 1), 0, 0)
                        .getSquareKms());
    }

    @Test
    public void testSmallPolygon() throws Exception {
        assertAreaEquals(78,
                Calculations.getAreas(createRectangle(0, 51, 0.1), 0, 0)
                        .getSquareKms());
    }

    @Test
    public void testSmallPolygonNorthern() throws Exception {
        assertAreaEquals(62,
                Calculations.getAreas(createRectangle(24.9, 60.1, 0.1), 0, 0)
                        .getSquareKms());
    }

    @Test
    public void testWktPolygon() throws Exception {
        String wkt = "POLYGON ((-0.027 48.061, -0.016 48.091, 0.011 48.119, " +
                "0.044 48.144, 1.73 49.11, 1.763 49.127, 1.796 49.135, " +
                "1.829 49.143, 2.318 49.215, 3.878 49.415, 3.922 49.415, " +
                "4.032 49.415, 4.071 49.415, 4.115 49.407, 4.147 49.393, " +
                "4.18 49.377, 4.208 49.358, 4.23 49.333, 4.263 49.294, " +
                "4.279 49.267, 4.285 49.239, 4.285 49.209, 4.274 49.182, " +
                "4.252 49.157, 4.23 49.132, 4.197 49.113, 4.158 49.097, " +
                "0.286 47.855, 0.247 47.844, 0.203 47.839, 0.154 47.841, " +
                "0.11 47.85, 0.071 47.861, 0.033 47.88, 0.005 47.905, " +
                "-0.016 47.932, -0.027 47.96, -0.033 47.99, -0.033 48.031, " +
                "-0.027 48.061))";
        Result result = Calculations.getAreas(wkt, 0, 0);
        assertAreaEquals(19836, result.getSquareKms());
        assertEquals(1, result.getGeometries());
    }

    @Test
    public void testPolygonWithHole() throws Exception {
        Polygon outer = createRectangle(0, 51, 1);
        Polygon hole = createRectangle(0.25, 51.25, 0.5);
        Polygon withHole = factory.createPolygon(
                outer.getExteriorRing(),
                new LinearRing[]{hole.getExteriorRing()});
        assertAreaEquals(
                Calculations.getAreas(outer, 0, 0).getSquareKms() -
                        Calculations.getAreas(hole, 0, 0).getSquareKms(),
                Calculations.getAreas(withHole, 0, 0).getSquareKms());
    }

    @Test
    public void testPolygonSplitByGridCell() throws Exception {
        // A U shape whose arms cross into the next grid cell, where they
        // are two separate pieces.
        Polygon shape = factory.createPolygon(new Coordinate[]{
                new Coordinate(0.2, 50.5),
                new Coordinate(0.8, 50.5),
                new Coordinate(0.8, 51.5),
                new Coordinate(0.6, 51.5),
                new Coordinate(0.6, 50.7),
                new Coordinate(0.4, 50.7),
                new Coordinate(0.4, 51.5),
                new Coordinate(0.2, 51.5),
                new Coordinate(0.2, 50.5)});
        Polygon whole = factory.createPolygon(new Coordinate[]{
                new Coordinate(0.2, 50.5),
                new Coordinate(0.8, 50.5),
                new Coordinate(0.8, 51.5),
                new Coordinate(0.2, 51.5),
                new Coordinate(0.2, 50.5)});
        Polygon gap = factory.createPolygon(new Coordinate[]{
                new Coordinate(0.4, 50.7),
                new Coordinate(0.6, 50.7),
                new Coordinate(0.6, 51.5),
                new Coordinate(0.4, 51.5),
                new Coordinate(0.4, 50.7)});
        assertAreaEquals(
                Calculations.getAreas(whole, 0, 0).getSquareKms() -
                        Calculations.getAreas(gap, 0, 0).getSquareKms(),
                Calculations.getAreas(shape, 0, 0).getSquareKms());
    }

    @Test
    public void testWktAreaIsCached() throws Exception {
        String wkt = createRectangle(10, 51, 1).toText();

        Calculations.getAreas(wkt, 0, 0);
        int first = Calculations.getCachedWktCount();
        Result again = Calculations.getAreas(wkt, 51.5, 10.5);

        assertEquals(first, Calculations.getCachedWktCount());
        assertTrue(again.getContains());
    }

    @Test
    public void testContains() throws Exception {
        Result result = Calculations.getAreas(
                createRectangle(0, 51, 1), 51.5, 0.5);
        assertTrue("The point should be contained in the area",
                result.getContains());
        Result result2 = Calculations.getAreas(
                createRectangle(0, 51, 1), 0, 0);
        assertEquals("The point should not be contained in the area",
                false, result2.getContains());
    }

    private static Polygon createRectangle(double x, double y, double d) {
        return factory.createPolygon(new Coordinate[]{
                new Coordinate(x, y),
                new Coordinate(x + d, y),
                new Coordinate(x + d, y + d),
                new Coordinate(x, y + d),
                new Coordinate(x, y)});
    }
}
