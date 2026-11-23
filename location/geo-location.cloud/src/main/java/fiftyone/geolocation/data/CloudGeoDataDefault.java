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

package fiftyone.geolocation.data;

import fiftyone.geolocation.core.data.GeoDataDefault;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import java.util.Map;
import org.slf4j.Logger;

/**
 * The Location Element Data from the cloud. The cloud engine stores each
 * property the response holds as an AspectPropertyValue, so the getters of
 * GeoDataDefault return them as they are.
 */
public class CloudGeoDataDefault extends GeoDataDefault implements CloudGeoData {

    public CloudGeoDataDefault(Logger logger, FlowData flowData, AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine, MissingPropertyService missingPropertyService) {
        super(logger, flowData, engine, missingPropertyService);
    }

    /**
     * Has no effect.
     * @param value the messages for properties with no value, by name
     * @deprecated The cloud engine sets the message of each property with no
     * value on the value itself.
     */
    @Deprecated
    @Override
    public void setNoValueReasons(Map<String, String> value) {
    }
}
