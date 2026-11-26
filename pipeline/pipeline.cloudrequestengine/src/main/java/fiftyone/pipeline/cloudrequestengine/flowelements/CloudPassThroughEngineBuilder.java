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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.pipeline.cloudrequestengine.data.CloudPassThroughData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngineBuilderBase;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.slf4j.ILoggerFactory;

import java.util.List;

/**
 * Builds a {@link CloudPassThroughEngine} for one part of the cloud
 * service's response. Add one after the Cloud Request Engine for each part
 * no other engine reads, for example {@code browserderived} for the browser
 * tests' script, {@code derived} for HumanConfidence and {@code fodid} for
 * the 51Did.
 */
public class CloudPassThroughEngineBuilder
    extends CloudAspectEngineBuilderBase<
        CloudPassThroughEngineBuilder,
        CloudPassThroughEngine> {

    private final String elementDataKey;

    /**
     * Construct a new instance.
     * @param loggerFactory creates the engine's logger
     * @param elementDataKey the name of the part of the cloud response the
     *                       engine reads, which is also its Data Key
     */
    public CloudPassThroughEngineBuilder(
        ILoggerFactory loggerFactory,
        String elementDataKey) {
        super(loggerFactory);
        this.elementDataKey = elementDataKey;
    }

    @Override
    protected CloudPassThroughEngine newEngine(List<String> properties) {
        return new CloudPassThroughEngine(
            loggerFactory.getLogger(CloudPassThroughEngine.class.getName()),
            new ElementDataFactory<CloudPassThroughData>() {
                @Override
                public CloudPassThroughData create(
                    FlowData flowData,
                    FlowElement<CloudPassThroughData, ?> flowElement) {
                    return new CloudPassThroughData(
                        loggerFactory.getLogger(
                            CloudPassThroughData.class.getName()),
                        flowData,
                        (CloudPassThroughEngine)flowElement,
                        MissingPropertyServiceDefault.getInstance());
                }
            },
            elementDataKey);
    }

    /**
     * Build the engine.
     * @return the engine
     * @throws Exception if the engine cannot be built
     */
    public CloudPassThroughEngine build() throws Exception {
        return buildEngine();
    }
}
