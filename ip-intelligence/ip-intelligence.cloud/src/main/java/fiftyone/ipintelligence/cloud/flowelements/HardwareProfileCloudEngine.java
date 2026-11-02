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

package fiftyone.ipintelligence.cloud.flowelements;

import fiftyone.ipintelligence.cloud.data.MultiIPIDataCloud;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.cloudrequestengine.flowelements.PropertyKeyedCloudEngineBase;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;

import org.slf4j.Logger;

/**
 * Engine that takes the JSON response from the {@link CloudRequestEngine} and
 * uses it to populate a {@link MultiIPIDataCloud} instance for easier
 * consumption.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/ip-intelligence-specification/pipeline-elements/hardware-profile-lookup-cloud.md">Specification</a>
 */
public class HardwareProfileCloudEngine
    extends PropertyKeyedCloudEngineBase<MultiIPIDataCloud, IPIntelligenceData> {

    public HardwareProfileCloudEngine(
        Logger logger,
        ElementDataFactory<MultiIPIDataCloud> aspectDataFactory) {
        super(logger, aspectDataFactory);
    }

    @Override
    protected IPIntelligenceData createProfileData(FlowData flowData) {
        return new IPIntelligenceDataCloudInternal(
            logger,
            flowData ,
            this,
            MissingPropertyServiceDefault.getInstance());
    }

    @Override
    public String getElementDataKey() {
        return "hardware";
    }

    @Override
    protected void unmanagedResourcesCleanup() {

    }
}
