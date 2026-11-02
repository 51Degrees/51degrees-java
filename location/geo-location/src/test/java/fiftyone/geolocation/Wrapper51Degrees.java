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

import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngineBuilder;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.geolocation.flowelements.GeoLocationCloudEngine;
import fiftyone.geolocation.flowelements.GeoLocationCloudEngineBuilder;

import org.slf4j.LoggerFactory;

public class Wrapper51Degrees implements Wrapper {

    protected static final TestLoggerFactory logger =
            new TestLoggerFactory(LoggerFactory.getILoggerFactory());
    private Pipeline pipeline;
    private CloudRequestEngine cloudRequestEngine;
    private GeoLocationCloudEngine engine;

    public Wrapper51Degrees() throws Exception {
        
        String envResourceKey = System.getenv(Constants.RESOURCE_KEY_ENV_VAR);
        String propertyResourceKey = System.getProperty(Constants.RESOURCE_KEY_ENV_VAR);
        String resourceKey = null;
        
        if(envResourceKey == null || envResourceKey.isEmpty()) {
    	if (propertyResourceKey == null || propertyResourceKey.isEmpty())
    		throw new Exception("Resource key is required to run Cloud tests.");
    	else {
    		resourceKey = propertyResourceKey;
    	}
        }
        else {
        	resourceKey = envResourceKey;
        }

    	cloudRequestEngine = new CloudRequestEngineBuilder(logger)
                .setResourceKey(resourceKey)
                .build();
        engine =  new GeoLocationCloudEngineBuilder(logger)
                .setGeoLocationProvider("FiftyOneDegrees")
                .build();;
        pipeline = new PipelineBuilder(logger)
        		.addFlowElement(cloudRequestEngine)
                .addFlowElement(engine)
                .build();
    }

    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public GeoLocationCloudEngine getEngine() {
        return engine;
    }

    @Override
    public Iterable<AspectPropertyMetaData> getProperties() {
        return engine.getProperties();
    }

   
    @Override
    public void close() {
        try {
            pipeline.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        pipeline = null;
        try {
            engine.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        engine = null;
    }
}