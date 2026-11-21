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

package fiftyone.geolocation;

import fiftyone.geolocation.core.Enums;
import fiftyone.geolocation.flowelements.GeoLocationCloudEngine;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import static org.junit.Assert.assertTrue;

/**
 * The string key of the Location cloud engine's data contains only
 * alphanumeric characters, full stop or hyphen.
 * <p>
 * The key for the DigitalElement provider, "location_digitalelement", is
 * left out. It is the name the cloud service gives that product in its
 * response and property list, which the engine reads by its key, and the
 * name every language uses, so it cannot change here alone.
 * <p>
 * Specification: pipeline-specification/features/access-to-results.md line 7
 * at 51Degrees/specifications 706de1f0, "Strings SHOULD only contain
 * alphanumeric characters, full stop or hyphen."
 */
public class ElementDataKeyFormatTests {

    private static String key(Enums.GeoLocationProvider provider) {
        return new GeoLocationCloudEngine(
            LoggerFactory.getLogger(GeoLocationCloudEngine.class),
            null,
            provider).getElementDataKey();
    }

    @Test
    public void keys_OnlyAlphanumericFullStopOrHyphen() {
        String key = key(Enums.GeoLocationProvider.FiftyOneDegrees);
        assertTrue(key, key.matches("[A-Za-z0-9.-]+"));
    }

    /**
     * Every provider's key, the DigitalElement one included, is lower-case.
     * Specification: access-to-results.md line 10, "Lower-case SHOULD be
     * used."
     */
    @Test
    public void keys_LowerCase() {
        for (Enums.GeoLocationProvider provider :
            Enums.GeoLocationProvider.values()) {
            String key = key(provider);
            assertTrue(key, key.equals(key.toLowerCase(Locale.ROOT)));
        }
    }
}
