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

package fiftyone.geolocation.core;

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;

import java.util.Arrays;

public class Constants {
    public static final String EVIDENCE_GEO_LAT_KEY = "location.latitude";
    public static final String EVIDENCE_GEO_LON_KEY = "location.longitude";
    public static final String EVIDENCE_GEO_LAT_COOKIE_NAME = "51D_Pos_latitude";
    public static final String EVIDENCE_GEO_LON_COOKIE_NAME = "51D_Pos_longitude";
    public static final String EVIDENCE_GEO_LAT_COOKIE_KEY = "cookie." + EVIDENCE_GEO_LAT_COOKIE_NAME;
    public static final String EVIDENCE_GEO_LON_COOKIE_KEY = "cookie." + EVIDENCE_GEO_LON_COOKIE_NAME;
    public static final String EVIDENCE_GEO_LAT_PARAM_KEY = "query." + EVIDENCE_GEO_LAT_COOKIE_NAME;
    public static final String EVIDENCE_GEO_LON_PARAM_KEY = "query." + EVIDENCE_GEO_LON_COOKIE_NAME;

    public static final EvidenceKeyFilter DefaultGeoEvidenceKeyFilter =
        new EvidenceKeyFilterWhitelist(Arrays.asList(
                fiftyone.pipeline.core.Constants.EVIDENCE_CLIENTIP_KEY,
                Constants.EVIDENCE_GEO_LAT_KEY,
                Constants.EVIDENCE_GEO_LON_KEY,
                Constants.EVIDENCE_GEO_LAT_COOKIE_KEY,
                Constants.EVIDENCE_GEO_LON_COOKIE_KEY,
                Constants.EVIDENCE_GEO_LAT_PARAM_KEY,
                Constants.EVIDENCE_GEO_LON_PARAM_KEY),
            String.CASE_INSENSITIVE_ORDER);

    public static final String NO_EVIDENCE_MESSAGE = "This property requires evidence" +
        " values from JavaScript running on the client. It cannot be" +
        " populated until a future request is made that contains this" +
        " additional data.";
}
