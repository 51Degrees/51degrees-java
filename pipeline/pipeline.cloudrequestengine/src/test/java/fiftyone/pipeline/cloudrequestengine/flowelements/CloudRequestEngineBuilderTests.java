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

import fiftyone.common.testhelpers.TestLogger;
import fiftyone.pipeline.cloudrequestengine.CloudRequestException;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class CloudRequestEngineBuilderTests extends CloudRequestEngineTestsBase {
    private final String testResourceKey = "resource_key";
	private final String testEndpoint = "https://testEndpoint/";
	private final String testEnvVarEndpoint = "https://testEnvVarEndpoint/";
	private final String testOldEnvVarEndpoint =
		"https://testOldEnvVarEndpoint/";

	// The deprecated name of the end point variable, which is also the
	// name of the constant that holds it.
	private static final String OLD_ENDPOINT_VARIABLE = "FOD_CLOUD_API_URL";

	// The old name is logged as deprecated once in a process, so what was
	// logged is carried from one test of this class to the next.
	private static final List<String> deprecationsInEarlierTests =
		new ArrayList<>();
	
	public CloudRequestEngineBuilderTests() throws MalformedURLException {
        super();
    }
	
    @BeforeEach
    public void setUp() throws IOException {
    	configureMockedClient();
    }

    @AfterEach
    public void countDeprecations() {
        deprecationsInEarlierTests.addAll(deprecationsLogged());
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
     * Gives the builder an environment holding only the variables in the
     * map, so that a test chooses which of the two names is set.
     */
    private void setEnvironment(
        CloudRequestEngineBuilder builder,
        Map<String, String> environment) throws Exception {
        Field getEnvVarField = builder.getClass().getDeclaredField("getEnvVar");
        getEnvVarField.setAccessible(true);
        getEnvVarField.set(
            builder,
            (Function<String, String>)(name) -> environment.get(name));
    }

    /**
     * A builder that finds the old name in the environment, and the new
     * name only where a value is given for it.
     */
    private CloudRequestEngineBuilder builderWithOldName(
        String newNameValue) throws Exception {
        Map<String, String> environment = new HashMap<>();
        environment.put(OLD_ENDPOINT_VARIABLE, testOldEnvVarEndpoint);
        if (newNameValue != null) {
            environment.put(Constants.CLOUD_ENDPOINT_VARIABLE, newNameValue);
        }
        CloudRequestEngineBuilder builder =
            new CloudRequestEngineBuilder(loggerFactory, httpClient);
        setEnvironment(builder, environment);
        return builder.setResourceKey(testResourceKey);
    }

    /**
     * The warnings this test's loggers hold that name the old variable.
     */
    private List<String> deprecationsLogged() {
        List<String> result = new ArrayList<>();
        for (TestLogger logger : loggerFactory.loggers) {
            for (String warning : logger.warningsLogged) {
                if (warning.contains(OLD_ENDPOINT_VARIABLE)) {
                    result.add(warning);
                }
            }
        }
        return result;
    }

    @Test
    public void CloudEndPoint_Old_Environment_Name() throws Exception {
        CloudRequestEngineBuilder builder = builderWithOldName(null);

        builder.build();

        assertEquals(testOldEnvVarEndpoint, getEndpointValue(builder));
        assertEquals(
            testOldEnvVarEndpoint + "accessibleproperties",
            getPropertiesEndpointValue(builder));
    }

    @Test
    public void CloudEndPoint_Old_Environment_Name_When_New_Is_Empty()
        throws Exception {
        CloudRequestEngineBuilder builder = builderWithOldName("");

        builder.build();

        assertEquals(testOldEnvVarEndpoint, getEndpointValue(builder));
    }

    @Test
    public void CloudEndPoint_New_Environment_Name_Wins() throws Exception {
        CloudRequestEngineBuilder builder =
            builderWithOldName(testEnvVarEndpoint);

        builder.build();

        assertEquals(testEnvVarEndpoint, getEndpointValue(builder));
        assertEquals(
            testEnvVarEndpoint + "accessibleproperties",
            getPropertiesEndpointValue(builder));
        assertEquals(0, deprecationsLogged().size());
    }

    @Test
    public void CloudEndPoint_Explicit_Setting_Beats_Old_Name()
        throws Exception {
        CloudRequestEngineBuilder builder = builderWithOldName(null);

        builder.setEndpoint(testEndpoint).build();

        assertEquals(testEndpoint, getEndpointValue(builder));
        assertEquals(0, deprecationsLogged().size());
    }

    /**
     * Two engines built from the old name log one warning between them,
     * however many of this class's tests read the old name before this one.
     */
    @Test
    public void CloudEndPoint_Old_Environment_Name_Logged_Once()
        throws Exception {
        builderWithOldName(null).build();
        builderWithOldName(null).build();

        List<String> logged = new ArrayList<>(deprecationsInEarlierTests);
        logged.addAll(deprecationsLogged());
        assertEquals(1, logged.size(), logged.toString());
        String warning = logged.get(0);
        assertTrue(
            warning.contains(Constants.CLOUD_ENDPOINT_VARIABLE), warning);
        assertTrue(warning.contains("deprecated"), warning);
    }

    @Test
    public void CloudEndPoint_Old_Constant_Is_Kept() throws Exception {
        Field old = Constants.class.getField(OLD_ENDPOINT_VARIABLE);

        assertEquals(OLD_ENDPOINT_VARIABLE, old.get(null));
        assertTrue(old.isAnnotationPresent(Deprecated.class));
        assertEquals(
            "_51DEGREES_CLOUD_ENDPOINT", Constants.CLOUD_ENDPOINT_VARIABLE);
    }
}
