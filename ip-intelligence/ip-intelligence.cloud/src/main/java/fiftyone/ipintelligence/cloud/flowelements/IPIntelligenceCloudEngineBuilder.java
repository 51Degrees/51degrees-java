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

import fiftyone.ipintelligence.cloud.data.IPIntelligenceDataCloud;
import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngineBuilderBase;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.slf4j.ILoggerFactory;

import java.util.List;

/**
 * Builder for the {@link IPIntelligenceCloudEngine}. All options for the engine
 * should be set here.
 */
@ElementBuilder(alternateName = "CloudIPIntelligence")
public class IPIntelligenceCloudEngineBuilder
    extends CloudAspectEngineBuilderBase<
            IPIntelligenceCloudEngineBuilder,
            IPIntelligenceCloudEngine> {

    /**
     * Construct a new instance using the {@link ILoggerFactory} supplied.
     * @param loggerFactory the logger factory to use
     */
    public IPIntelligenceCloudEngineBuilder(ILoggerFactory loggerFactory) {
        super(loggerFactory);
    }

    @Override
    protected IPIntelligenceCloudEngine newEngine(List<String> properties) {
        return new IPIntelligenceCloudEngine(
            loggerFactory.getLogger(IPIntelligenceCloudEngine.class.getName()),
            new IPIDataCloudFactory(loggerFactory));
    }

    /**
     * Build an engine using the current options.
     * @return new instance
     */
    public IPIntelligenceCloudEngine build() throws Exception {
        return buildEngine();
    }

    private static class IPIDataCloudFactory
        implements ElementDataFactory<IPIntelligenceDataCloud> {

        private final ILoggerFactory loggerFactory;

        public IPIDataCloudFactory(ILoggerFactory loggerFactory) {
            this.loggerFactory = loggerFactory;
        }

        @Override
        public IPIntelligenceDataCloud create(
            FlowData flowData,
            FlowElement<IPIntelligenceDataCloud, ?> engine) {
            return new IPIntelligenceDataCloudInternal(
                loggerFactory.getLogger(IPIntelligenceDataCloud.class.getName()),
                flowData,
                (IPIntelligenceCloudEngine) engine,
                MissingPropertyServiceDefault.getInstance());
        }
    }
}