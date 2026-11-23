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

import org.locationtech.jts.geom.Polygon;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A latitude and longitude rectangle and its associated UTM transformation.
 * The transformation is used to handle area calculations that are aware the
 * earth is not a sphere and has more complex mappings between WGS84 latitudes
 * and longitudes and geographic areas.
 */
public class Rectangle {

    private static final CRSFactory crsFactory = new CRSFactory();

    private static final CoordinateTransformFactory transformFactory =
            new CoordinateTransformFactory();

    /**
     * The WGS84 geographic coordinate system used as the source for all
     * transformations.
     */
    private static final CoordinateReferenceSystem wgs84 =
            crsFactory.createFromParameters(
                    "WGS84",
                    "+proj=longlat +datum=WGS84 +no_defs");

    /**
     * Cache of transformations keyed on UTM zone and hemisphere. There are
     * only 120 distinct UTM transformations so caching them avoids repeated
     * construction for every grid rectangle.
     */
    private static final Map<String, CoordinateTransform> transformCache =
            new ConcurrentHashMap<>();

    private final CoordinateTransform transformation;

    private final Polygon polygon;

    public Rectangle(Polygon polygon) {
        this.polygon = polygon;
        this.transformation = createTransform(
                polygon.getInteriorPoint().getX(),
                polygon.getInteriorPoint().getY());
    }

    public CoordinateTransform getTransformation() {
        return transformation;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    /**
     * Create a transformation from WGS84 to the UTM projected coordinate
     * system for the zone containing the point provided.
     * @param x longitude of the point
     * @param y latitude of the point
     * @return a coordinate transformation to the relevant UTM zone
     */
    public static CoordinateTransform createTransform(double x, double y) {
        // Create UTM projected coordinate system for a specific zone
        int utmZone = (int) Math.floor((x + 180) / 6) + 1;
        boolean isNorthernHemisphere = y >= 0;

        String key = utmZone + (isNorthernHemisphere ? "N" : "S");
        return transformCache.computeIfAbsent(key, k -> {
            CoordinateReferenceSystem utm = crsFactory.createFromParameters(
                    "UTM" + k,
                    "+proj=utm +zone=" + utmZone +
                            (isNorthernHemisphere ? "" : " +south") +
                            " +datum=WGS84 +units=m +no_defs");
            return transformFactory.createTransform(wgs84, utm);
        });
    }
}
