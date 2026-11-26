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

package fiftyone.pipeline.cloudrequestengine.data;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.slf4j.Logger;

/**
 * The values of one part of the cloud service's response, as a
 * {@link fiftyone.pipeline.cloudrequestengine.flowelements.CloudPassThroughEngine}
 * reads them. Each value is an
 * {@link fiftyone.pipeline.engines.data.AspectPropertyValue} read by the
 * name the cloud service gives it, without regard to case.
 */
public class CloudPassThroughData extends AspectDataBase {

    /**
     * Construct a new instance.
     * @param logger used for logging
     * @param flowData the Flow Data this is part of
     * @param engine the engine that creates it
     * @param missingPropertyService says why a property is missing
     */
    public CloudPassThroughData(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine,
        MissingPropertyService missingPropertyService) {
        super(logger, flowData, engine, missingPropertyService);
    }
}
