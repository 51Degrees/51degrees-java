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

package fiftyone.geolocation.flowelements;

import fiftyone.geolocation.core.Enums;
import fiftyone.geolocation.core.data.GeoDataDefault;
import fiftyone.geolocation.data.CloudGeoData;
import fiftyone.geolocation.data.CloudGeoDataDefault;
import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngineBuilderBase;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.slf4j.ILoggerFactory;

import java.util.List;

@ElementBuilder(alternateName = "CloudGeoLocation")
public class GeoLocationCloudEngineBuilder extends CloudAspectEngineBuilderBase<
    GeoLocationCloudEngineBuilder,
    GeoLocationCloudEngine> {

    private CloudRequestEngine requestEngine;
    private Enums.GeoLocationProvider provider;

    public GeoLocationCloudEngineBuilder(ILoggerFactory loggerFactory) {
        super(loggerFactory);
    }
    
    public GeoLocationCloudEngineBuilder setGeoLocationProvider(String provider) throws Exception {
        this.provider = Enums.GeoLocationProvider.valueOf(provider);
        return this;
    }
    
    public GeoLocationCloudEngine build() throws Exception {
        return buildEngine();
    }

    public GeoLocationCloudEngine build(Enums.GeoLocationProvider provider) throws Exception {
        this.provider = provider;
        return buildEngine();
    }

    @Override
    protected GeoLocationCloudEngine newEngine(List<String> properties) {
        return new GeoLocationCloudEngine(
            loggerFactory.getLogger(GeoLocationCloudEngine.class.getName()),
            new GeoLocationCloudDataFactory(),
            provider);
    }

    private class GeoLocationCloudDataFactory implements ElementDataFactory<CloudGeoData> {

        @Override
        public CloudGeoData create(FlowData flowData, FlowElement<CloudGeoData, ?> flowElement) {
            return new CloudGeoDataDefault(
                loggerFactory.getLogger(GeoDataDefault.class.getName()),
                flowData,
                (GeoLocationCloudEngine)flowElement,
                MissingPropertyServiceDefault.getInstance());
        }
    }
}
