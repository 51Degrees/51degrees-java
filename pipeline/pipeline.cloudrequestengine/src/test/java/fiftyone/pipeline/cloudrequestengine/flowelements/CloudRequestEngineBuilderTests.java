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

import fiftyone.pipeline.cloudrequestengine.CloudRequestException;
import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class CloudRequestEngineBuilderTests extends CloudRequestEngineTestsBase {
    private final String testResourceKey = "resource_key";
	private final String testEndpoint = "https://testEndpoint/";
	private final String testEnvVarEndpoint = "https://testEnvVarEndpoint/";

	
	public CloudRequestEngineBuilderTests() throws MalformedURLException {
        super();
    }
	
    @BeforeEach
    public void setUp() throws IOException {
    	configureMockedClient();
    }

    
    @Test
    @SuppressWarnings("unused")
    public void BuildEngine_ResourceKey_NotSet() {
        assertThrows(PipelineConfigurationException.class, () -> {
            CloudRequestEngine cloudRequestsEngine =
                new CloudRequestEngineBuilder(
                    LoggerFactory.getILoggerFactory(),
                    new HttpClientDefault())
                    .build();
        });
    }

    private void setExpectedEnvVarValue(
    	CloudRequestEngineBuilder builder,
    	String expectedValue) throws Exception {
    	Field getEnvVarField = builder.getClass().getDeclaredField("getEnvVar");
    	getEnvVarField.setAccessible(true);
    	getEnvVarField.set(builder, (Function<String, String>)(name) -> {
    		return expectedValue;
    	});
    }
    
    private String getEndpointValue(
    		CloudRequestEngineBuilder builder) throws Exception {
    	Field endPointField = builder.getClass().getDeclaredField("endPoint");
    	endPointField.setAccessible(true);
    	return (String)endPointField.get(builder);
    }
 
    private String getPropertiesEndpointValue(
    		CloudRequestEngineBuilder builder) throws Exception {
    	Field propertiesEndPointField = builder.getClass().getDeclaredField("propertiesEndpoint");
    	propertiesEndPointField.setAccessible(true);
    	return (String)propertiesEndPointField.get(builder);
    }
    
    @Test
    public void CloudEndPoint_Explicit_Setting() throws Exception {
    	CloudRequestEngineBuilder builder =
    		new CloudRequestEngineBuilder(loggerFactory, httpClient);
    	setExpectedEnvVarValue(builder, testEnvVarEndpoint);
    	
    	builder.setResourceKey(testResourceKey)
    		.setEndpoint(testEndpoint)
    		.build();
    	assertEquals(testEndpoint, getEndpointValue(builder));
    	assertEquals(testEndpoint + "accessibleproperties", getPropertiesEndpointValue(builder));
    }
    
    @Test
    public void CloudEndPoint_Environment_Setting() throws Exception {
    	CloudRequestEngineBuilder builder =
    		new CloudRequestEngineBuilder(loggerFactory, httpClient);
    	setExpectedEnvVarValue(builder, testEnvVarEndpoint);
    	
    	builder.setResourceKey(testResourceKey)
    		.build();
    	assertEquals(testEnvVarEndpoint, getEndpointValue(builder));
    	assertEquals(testEnvVarEndpoint + "accessibleproperties", getPropertiesEndpointValue(builder));
    }
    
    @Test
    public void CloudEndPoint_Default_Setting() throws Exception {
    	CloudRequestEngineBuilder builder =
    		new CloudRequestEngineBuilder(loggerFactory, httpClient);
    	setExpectedEnvVarValue(builder, null);
    	
    	builder.setResourceKey(testResourceKey)
    		.build();
    	assertEquals(
    		Constants.END_POINT_DEFAULT + "/", getEndpointValue(builder));
    }

    /**
     * A variable that is set but empty counts as not set, so the default
     * is used.
     */
    @Test
    public void CloudEndPoint_Blank_Environment_Setting() throws Exception {
        CloudRequestEngineBuilder builder =
            new CloudRequestEngineBuilder(loggerFactory, httpClient);
        setExpectedEnvVarValue(builder, "");

        builder.setResourceKey(testResourceKey)
            .build();
        assertEquals(
            Constants.END_POINT_DEFAULT + "/", getEndpointValue(builder));
    }

    /**
     * With no address given, the engine uses the address the environment
     * this test runs in gives, which is what {@link ExpectedEndpoint}
     * expects for the other tests.
     */
    @Test
    public void CloudEndPoint_Matches_Expected() throws Exception {
        CloudRequestEngineBuilder builder =
            new CloudRequestEngineBuilder(loggerFactory, httpClient);

        builder.setResourceKey(testResourceKey)
            .build();
        assertEquals(ExpectedEndpoint.root(), getEndpointValue(builder));
    }

    /**
     * With no address given, the engine uses the address
     * {@link ExpectedEndpoint} works out from the variable, when the
     * variable is missing, empty, an address without a trailing slash or
     * an address with one.
     */
    @Test
    public void CloudEndPoint_Matches_Expected_For_Each_Variable()
        throws Exception {
        String[] values = {
            null,
            "",
            "https://a.example/api/v4",
            "https://a.example/api/v4/"
        };
        for (String value : values) {
            CloudRequestEngineBuilder builder =
                new CloudRequestEngineBuilder(loggerFactory, httpClient);
            setExpectedEnvVarValue(builder, value);

            builder.setResourceKey(testResourceKey)
                .build();
            assertEquals(
                ExpectedEndpoint.root(value),
                getEndpointValue(builder),
                String.valueOf(value));
        }
    }

    /**
     * An address given in configuration is used over the environment
     * variable, so every request goes to that address.
     */
    @Test
    public void CloudEndPoint_Configuration_Setting() throws Exception {
        CloudRequestEngineBuilder builder =
            new CloudRequestEngineBuilder(loggerFactory, httpClient);
        setExpectedEnvVarValue(builder, testEnvVarEndpoint);

        ElementOptions element = new ElementOptions();
        element.builderName = "CloudRequestEngineBuilder";
        element.buildParameters.put("ResourceKey", testResourceKey);
        element.buildParameters.put("Endpoint", testEndpoint);
        PipelineOptions options = new PipelineOptions();
        options.elements.add(element);

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addElementBuilder(builder)
            .buildFromConfiguration(options)) {
            assertNotNull(pipeline.getElement(CloudRequestEngine.class));
        }
        assertEquals(testEndpoint, getEndpointValue(builder));

        ArgumentCaptor<URL> urls = ArgumentCaptor.forClass(URL.class);
        verify(httpClient, atLeastOnce()).connect(urls.capture());
        for (URL url : urls.getAllValues()) {
            assertTrue(url.toString().startsWith(testEndpoint),
                url.toString());
        }
    }
}
