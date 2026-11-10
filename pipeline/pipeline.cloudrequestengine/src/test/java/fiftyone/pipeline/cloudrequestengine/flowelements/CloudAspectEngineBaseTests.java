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

import static fiftyone.pipeline.cloudrequestengine.Constants.Messages.ExceptionFailedToLoadProperties;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fiftyone.common.testhelpers.TestLogger;
import fiftyone.common.testhelpers.TestLoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.MultiProfileData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.AspectEngineBase;

public class CloudAspectEngineBaseTests {
    public class TestData extends AspectDataBase {
        public TestData(
            Logger logger,
            FlowData flowData,
            AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
            super(logger, flowData, engine);
        }
    }

    private class TestInstance extends CloudAspectEngineBase<TestData> {
        public TestInstance() {
            super(loggerFactory.getLogger("TestInstance"), new ElementDataFactory<TestData>() {
                @Override
                public TestData create(FlowData flowData, FlowElement<TestData, ?> flowElement) {
                    return new TestData(null, flowData, (AspectEngine<TestData, ?>) flowElement);
                }
            });
        }

        @Override
        public String getElementDataKey() {
            return "test";
        }

        private final EvidenceKeyFilter evidenceKeyFilter = new EvidenceKeyFilterWhitelist(new ArrayList<String>());

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return evidenceKeyFilter;
        }

        @Override
        protected void unmanagedResourcesCleanup() {

        }

        @Override
        protected void processCloudEngine(FlowData data, TestData aspectData, String json) {
            if (json == null || json.isEmpty())  
            {
                fail("'json' value should not be null or empty if " +
                        "this method is called");                
            }        
        }
    }

    public class TestProfiles extends AspectDataBase
        implements MultiProfileData<TestData> {
        private final List<TestData> profiles = new ArrayList<>();

        public TestProfiles(
            Logger logger,
            FlowData flowData,
            AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
            super(logger, flowData, engine);
        }

        @Override
        public List<TestData> getProfiles() {
            return profiles;
        }

        @Override
        public void addProfile(TestData profile) {
            profiles.add(profile);
        }
    }

    private class TestKeyedInstance
        extends PropertyKeyedCloudEngineBase<TestProfiles, TestData> {
        public TestKeyedInstance() {
            super(loggerFactory.getLogger("TestKeyedInstance"), new ElementDataFactory<TestProfiles>() {
                @Override
                public TestProfiles create(FlowData flowData, FlowElement<TestProfiles, ?> flowElement) {
                    return new TestProfiles(null, flowData, (AspectEngine<TestProfiles, ?>) flowElement);
                }
            });
        }

        @Override
        public String getElementDataKey() {
            return "test";
        }

        @Override
        protected TestData createProfileData(FlowData flowData) {
            return new TestData(null, flowData, this);
        }

        @Override
        protected void unmanagedResourcesCleanup() {

        }
    }

    private class TestRequestEngine extends AspectEngineBase<CloudRequestData, AspectPropertyMetaData>
        implements CloudRequestEngine {

        public TestRequestEngine() {
            super(loggerFactory.getLogger("TestRequestEngine"), new ElementDataFactory<CloudRequestData>() {
                @Override
                public CloudRequestData create(FlowData flowData, FlowElement<CloudRequestData, ?> flowElement) {
                    return new CloudRequestData(loggerFactory.getLogger("TestRequestEngine"), flowData, (AspectEngine<CloudRequestData, ?>) flowElement);
                }
            });
        }

        private Map<String, AccessiblePropertyMetaData.ProductMetaData> publicProperties;
        @Override
        public Map<String, AccessiblePropertyMetaData.ProductMetaData> getPublicProperties() {
            return publicProperties;
        }

        public void setPublicProperties(Map<String, AccessiblePropertyMetaData.ProductMetaData> properties) {
            this.publicProperties = properties;
        }

        @Override
        public String getDataSourceTier() {
            return "";
        }

        @Override
        public String getElementDataKey() {
            return "cloud";
        }

        private final EvidenceKeyFilter evidenceKeyFilter = new EvidenceKeyFilterWhitelist(new ArrayList<String>());
        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return evidenceKeyFilter;
        }

        private final List<AspectPropertyMetaData> properties = new ArrayList<>();
        @Override
        public List<AspectPropertyMetaData> getProperties() {
            return properties;
        }

        @Override
        protected void unmanagedResourcesCleanup() {

        }

        @Override
        protected void processEngine(FlowData data, CloudRequestData aspectData) {

        }

    }

    private TestInstance engine;
    private TestRequestEngine requestEngine;
    private Pipeline pipeline;

    private Map<String, AccessiblePropertyMetaData.ProductMetaData> propertiesReturnedByRequestEngine;
    private TestLoggerFactory loggerFactory;
    private int maxWarnings;
    private int maxErrors;

    @BeforeEach
    public void init() {
        propertiesReturnedByRequestEngine = new HashMap<>();
        loggerFactory = new TestLoggerFactory(null);
        maxWarnings = 0;
        maxErrors = 0;
    }

    @AfterEach
    public void cleanUp() throws Exception {
        try {
            loggerFactory.assertMaxWarnings(maxWarnings);
            loggerFactory.assertMaxErrors(maxErrors);
        } finally {
            if (pipeline != null) {
                pipeline.close();
            }
        }
    }
    
    private <T extends ElementPropertyMetaData> boolean propertiesContainName(
            List<T> properties,
            String name) {
        for (T property : properties) {
            if (property.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Test the LoadProperties method of the CloudAspectEngine which
     * retrieves property meta-data from the cloud request engine.
     * @throws Exception 
     */
    @Test
    public void loadProperties() throws Exception {
        List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null));
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevendor", "String", null, null));
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevariants", "Array", null, null));
        AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test", devicePropertyData);

        createPipeline();

        assertEquals(3, engine.getProperties().size());
        assertTrue(propertiesContainName(engine.getProperties(), "ismobile"));
        assertTrue(propertiesContainName(engine.getProperties(), "hardwarevendor"));
        assertTrue(propertiesContainName(engine.getProperties(), "hardwarevariants"));
    }
    
    /**
    * A resource key that includes no products at all includes none for this
    * engine, so the Pipeline builds, the engine has empty property metadata
    * and the missing product is logged once.
    * @throws Exception
    */
    @Test
    public void loadProperties_noProperties() throws Exception {
        createPipeline();

        assertEquals(0, engine.getProperties().size());
        assertProductMissingLoggedOnce();
        maxWarnings = 1;
    }

    /**
     * A resource key whose products hold only other engines' properties
     * does not include this engine's product. The Pipeline builds, the engine
     * has empty property metadata, and the missing product is logged once
     * however often the metadata is read.
     * @throws Exception
     */
    @Test
    public void loadProperties_wrongProperties() throws Exception {
        List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null));
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevendor", "String", null, null));
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevariants", "Array", null, null));
        AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test2", devicePropertyData);

        createPipeline();

        for (int i = 0; i < 3; i++) {
            assertEquals(0, engine.getProperties().size());
        }
        assertProductMissingLoggedOnce();
        maxWarnings = 1;
    }

    /**
     * Accessible properties that have not been loaded are not a missing
     * product, so reading the property metadata still fails rather than
     * keeping an empty result.
     * @throws Exception
     */
    @Test
    public void loadProperties_notLoaded() throws Exception {
        propertiesReturnedByRequestEngine = null;
        try {
            createPipeline();
            fail("RuntimeException should be thrown");
        } catch (RuntimeException ex){
            assertEquals(
                String.format(
                    ExceptionFailedToLoadProperties,
                    "test",
                    "test"),
                ex.getMessage()
            );
        }
        maxErrors = 1;
    }

    /**
     * A property keyed engine, such as the hardware profile engine, whose
     * product the resource key does not include builds, gives no profiles
     * for a response that holds nothing for it, and logs nothing on each
     * request.
     * @throws Exception
     */
    @Test
    public void propertyKeyed_productMissing_noProfiles() throws Exception {
        List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null));
        AccessiblePropertyMetaData.ProductMetaData otherProduct = new AccessiblePropertyMetaData.ProductMetaData();
        otherProduct.properties = properties;
        propertiesReturnedByRequestEngine.put("test2", otherProduct);
        TestKeyedInstance keyed = new TestKeyedInstance();
        requestEngine = new TestRequestEngine();
        requestEngine.setPublicProperties(propertiesReturnedByRequestEngine);
        pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(requestEngine)
            .addFlowElement(keyed)
            .build();

        for (int i = 0; i < 3; i++) {
            TestProfiles profiles = new TestProfiles(
                null,
                Mockito.mock(FlowData.class),
                keyed);
            keyed.processCloudEngine(
                Mockito.mock(FlowData.class),
                profiles,
                "{ \"test2\": { \"ismobile\": true } }");
            assertEquals(0, profiles.getProfiles().size());
        }
        assertEquals(0, keyed.getProperties().size());
        assertProductMissingLoggedOnce();
        maxWarnings = 1;
    }

    /**
     * A property keyed engine whose product the resource key includes gives
     * no profiles, and throws nothing, for a response that holds no part
     * for it or a part without profiles, as the Processing section of the
     * cloud aspect engine specification says.
     * @throws Exception
     */
    @Test
    public void propertyKeyed_productPresent_noProfiles() throws Exception {
        List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null));
        AccessiblePropertyMetaData.ProductMetaData product = new AccessiblePropertyMetaData.ProductMetaData();
        product.properties = properties;
        propertiesReturnedByRequestEngine.put("test", product);
        TestKeyedInstance keyed = new TestKeyedInstance();
        requestEngine = new TestRequestEngine();
        requestEngine.setPublicProperties(propertiesReturnedByRequestEngine);
        pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(requestEngine)
            .addFlowElement(keyed)
            .build();

        for (String response : Arrays.asList(
            "{ \"test2\": { \"ismobile\": true } }",
            "{ \"test\": { } }")) {
            TestProfiles profiles = new TestProfiles(
                null,
                Mockito.mock(FlowData.class),
                keyed);
            keyed.processCloudEngine(
                Mockito.mock(FlowData.class),
                profiles,
                response);
            assertEquals(0, profiles.getProfiles().size(), response);
        }
    }

    private void assertProductMissingLoggedOnce() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (TestLogger logger : loggerFactory.loggers) {
            errors.addAll(logger.errorsLogged);
            warnings.addAll(logger.warningsLogged);
        }
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("'test'"), warnings.get(0));
        assertEquals(0, errors.size(), errors.toString());
    }

    /**
     * Test loading sub-property meta data where a cloud aspect engine 
     * has nested properties. E.g. the cloud property keyed engine.
     * @throws Exception 
     */
    @Test
    public void loadProperties_subProperties() throws Exception {
        List<AccessiblePropertyMetaData.PropertyMetaData> subProperties = new ArrayList<>();
        subProperties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null));
        subProperties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevendor", "String", null, null));
        subProperties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevariants", "Array", null, null));

        List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("devices", "Array", null, subProperties));

        AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test", devicePropertyData);

        createPipeline();

        assertEquals(1, engine.getProperties().size());
        assertEquals("devices", engine.getProperties().get(0).getName());
        assertEquals(3, engine.getProperties().get(0).getItemProperties().size());
        assertTrue(propertiesContainName(engine.getProperties().get(0).getItemProperties(), "ismobile"));
        assertTrue(propertiesContainName(engine.getProperties().get(0).getItemProperties(), "hardwarevendor"));
        assertTrue(propertiesContainName(engine.getProperties().get(0).getItemProperties(), "hardwarevariants"));
    }
    
    /**
     * Test loading delayed evidence property meta data.
     * @throws Exception 
     */
    @Test
    public void loadProperties_delayedProperties() throws Exception {
        List<String> evidenceProperties = new ArrayList<>();
        evidenceProperties.add("javascript");
        
        List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("javascript", "JavaScript", null, null, true, null));
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevendor", "String", null, null, null, evidenceProperties));
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData("hardwarevariants", "Array", null, null, null, evidenceProperties));
        AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test", devicePropertyData);

        createPipeline();

        assertEquals(3, engine.getProperties().size());
        assertTrue(propertiesContainName(engine.getProperties(), "javascript"));
        assertTrue(propertiesContainName(engine.getProperties(), "hardwarevendor"));
        assertTrue(propertiesContainName(engine.getProperties(), "hardwarevariants"));
        assertTrue(engine.getProperty("javascript").getDelayExecution());
        assertEquals(1, engine.getProperty("hardwarevendor").getEvidenceProperties().size());
        assertEquals(1, engine.getProperty("hardwarevariants").getEvidenceProperties().size());
    }

    /*
     * Test that when processing the cloud aspect engine, the
     * ProcessCloudEngine Method is called when the JSON response is
     * populated.
     */
    @Test
    public void Process_CloudResponse() {
        // Setup properties.
    	List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();;
    	properties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null, true, null));
    	AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test", devicePropertyData);
        
        // Create mock TestInstance so we can see if the ProcessCloudEngine
        // method is called.        
        TestInstance mockTestInstance = Mockito.spy(new TestInstance());
        
        // Create mock TestInstance so we can see if the ProcessCloudEngine
        // method is called.        
        TestRequestEngine mockRequestEngine = Mockito.spy(new TestRequestEngine()); 
        mockRequestEngine.publicProperties = propertiesReturnedByRequestEngine;
        Answer<Object> responseStringAnswer = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            	CloudRequestData aspectData = (CloudRequestData)invocationOnMock.getArgument(1);
            	aspectData.setJsonResponse("{ \"response\": true }");
            	aspectData.setProcessStarted(true);
				return null;
            }
        };
        doAnswer(responseStringAnswer)
        	.when(mockRequestEngine)
        		.processEngine(any(FlowData.class), any(CloudRequestData.class));
       
        try {
			pipeline = new PipelineBuilder(loggerFactory)
			        .addFlowElement(mockRequestEngine)
			        .addFlowElement(mockTestInstance)
			        .build();
			try (FlowData flowData = pipeline.createFlowData()) {
				flowData.addEvidence("query.user-agent", "iPhone");
				flowData.process();
			}
			
		} catch (Exception e) {
			fail("An exception has occurred here.");
		}
 
        // Verify the ProcessCloudEngine method was called
        verify(mockTestInstance, times(1)) 
        .processCloudEngine(any(FlowData.class), any(TestData.class), any(String.class));
    }
 
    /*
     * Test that when processing the cloud aspect engine, the
     * ProcessCloudMethod is not called when the JSON response is not
     * populated.
     */
    @Test
    public void Process_NoCloudResponse() {
        // Setup properties.
    	List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();;
    	properties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null, true, null));
    	AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test", devicePropertyData);
        
        // Create mock TestInstance so we can see if the ProcessCloudEngine
        // method is called.        
        TestInstance mockTestInstance = Mockito.spy(new TestInstance());
        
        // Create mock TestInstance so we can see if the ProcessCloudEngine
        // method is called.        
        TestRequestEngine mockRequestEngine = Mockito.spy(new TestRequestEngine()); 
        mockRequestEngine.publicProperties = propertiesReturnedByRequestEngine;
        Answer<Object> responseStringAnswer = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            	CloudRequestData aspectData = (CloudRequestData)invocationOnMock.getArgument(1);
            	aspectData.setProcessStarted(true);
				return null;
            }
        };
        doAnswer(responseStringAnswer)
        	.when(mockRequestEngine)
        		.processEngine(any(FlowData.class), any(CloudRequestData.class));

        try {
			pipeline = new PipelineBuilder(loggerFactory)
			        .addFlowElement(mockRequestEngine)
			        .addFlowElement(mockTestInstance)
			        .build();
			try (FlowData flowData = pipeline.createFlowData()) {
				flowData.addEvidence("query.user-agent", "iPhone");
				flowData.process();
			}
			
		} catch (Exception e) {
			fail("An exception has occurred here.");
		}
 
        // Verify the ProcessCloudEngine method was called
        verify(mockTestInstance,times(0))
            .processCloudEngine(
                any(FlowData.class),
                any(TestData.class),
                any(String.class));

        maxWarnings = 1;
    }
    
    /*
     * Test that the expected exception is thrown when the
     * CloudRequestEngine and a CloudAspectEngine have been added to the
     * Pipeline in the wrong order.
     */
    @Test
    public void CloudEngines_WrongOrder() {
        // Setup properties.
    	List<AccessiblePropertyMetaData.PropertyMetaData> properties = new ArrayList<>();;
    	properties.add(new AccessiblePropertyMetaData.PropertyMetaData("ismobile", "Boolean", null, null, true, null));
    	AccessiblePropertyMetaData.ProductMetaData devicePropertyData = new AccessiblePropertyMetaData.ProductMetaData();
        devicePropertyData.properties = properties;
        propertiesReturnedByRequestEngine.put("test", devicePropertyData);
        
        // Create mock TestInstance so we can see if the ProcessCloudEngine
        // method is called.        
        TestInstance mockTestInstance = Mockito.spy(new TestInstance());
        
        // Create mock TestInstance so we can see if the ProcessCloudEngine
        // method is called.        
        TestRequestEngine mockRequestEngine = Mockito.spy(new TestRequestEngine()); 
        mockRequestEngine.publicProperties = propertiesReturnedByRequestEngine;
        Answer<Object> responseStringAnswer = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            	CloudRequestData aspectData = (CloudRequestData)invocationOnMock.getArgument(1);
            	aspectData.setJsonResponse("{ \"response\": true }");
            	aspectData.setProcessStarted(true);
				return null;
            }
        };
        doAnswer(responseStringAnswer)
        	.when(mockRequestEngine)
        		.processEngine(any(FlowData.class), any(CloudRequestData.class));
       
        try {
			pipeline = new PipelineBuilder(loggerFactory)
			        .addFlowElement(mockTestInstance)
			        .addFlowElement(mockRequestEngine)
			        .build();
			try (FlowData flowData = pipeline.createFlowData()) {
				flowData.addEvidence("query.user-agent", "iPhone");
				flowData.process();
			}
			fail("Expected exception was not thrown");
			
		} catch (Exception ex) {
			Throwable[] exceptions= ex.getSuppressed();
		    assertTrue(exceptions.length > 0);
		    assertTrue(exceptions[0] instanceof PipelineConfigurationException);
		    assertTrue(exceptions[0].getMessage().contains("requires a 'CloudRequestEngine'before"
		    		+ " it in the Pipeline. This engine will be unable to produce"
		    		+ " results until this is corrected"));		    
		}
        maxErrors = 1;
    }

    /**
     * Test that the expected exception is thrown when the 
     * CloudRequestEngine has not been added to the Pipeline but a
     * CloudAspectEngine has.
     */
    public void CloudEngines_NoRequestEngine() {
    	TestInstance engine = new TestInstance();
		try {
			pipeline = new PipelineBuilder(loggerFactory)
			        .addFlowElement(engine)
			        .build();
			fail("Expected exception was not thrown");
		} catch (Exception e) {
			assertTrue(e instanceof PipelineConfigurationException);
		}
    }
    
	private void createPipeline() throws Exception {
        engine = new TestInstance();
        requestEngine = new TestRequestEngine();
        requestEngine.setPublicProperties(propertiesReturnedByRequestEngine);
        pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(requestEngine)
            .addFlowElement(engine)
            .build();
    }
}
