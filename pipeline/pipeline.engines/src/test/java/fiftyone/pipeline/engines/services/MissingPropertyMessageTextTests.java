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

package fiftyone.pipeline.engines.services;

import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngine;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The text of each missing Property message is the text of the
 * specification's table, apart from the campaign parameters an
 * implementation may add to each link, which are removed before comparing.
 * <p>
 * Specification: pipeline-specification/features/properties.md lines 123 to
 * 136 at 51Degrees/specifications 706de1f0, ending "Lists in a message are
 * joined with a comma and no space, for example `Premium,Enterprise`. An
 * implementation SHOULD give the same text, and MAY add to each link the
 * campaign parameters its own repository requires."
 */
public class MissingPropertyMessageTextTests {

    private static final String PREFIX =
        "Property 'testProperty' not found in data for element " +
            "'testElement'. ";

    private final MissingPropertyService service =
        MissingPropertyServiceDefault.getInstance();

    /**
     * Removes the query string from each link, which is where the campaign
     * parameters go.
     */
    private static String withoutCampaign(String message) {
        return message.replaceAll("(https://[^\\s?]+)\\?[^\\s]*?(?=\\.(\\s|$)|\\s|$)", "$1");
    }

    @SuppressWarnings("unchecked")
    private static AspectEngine<? extends AspectData, AspectPropertyMetaData>
        onPremise(String tier, List<String> tiersWherePresent, boolean available) {
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine =
            mock(AspectEngine.class);
        when(engine.getDataSourceTier()).thenReturn(tier);
        when(engine.getElementDataKey()).thenReturn("testElement");
        List<AspectPropertyMetaData> properties = new ArrayList<>();
        properties.add(new AspectPropertyMetaDataDefault(
            "testProperty", engine, "", String.class,
            tiersWherePresent, available));
        when(engine.getProperties()).thenReturn(properties);
        return engine;
    }

    @SuppressWarnings("unchecked")
    private static CloudAspectEngine<? extends AspectData, AspectPropertyMetaData>
        cloud(String... propertyNames) {
        CloudAspectEngine<? extends AspectData, AspectPropertyMetaData> engine =
            mock(CloudAspectEngine.class);
        when(engine.getElementDataKey()).thenReturn("testElement");
        List<AspectPropertyMetaData> properties = new ArrayList<>();
        for (String name : propertyNames) {
            properties.add(new AspectPropertyMetaDataDefault(
                name, engine, "", String.class,
                Collections.<String>emptyList(), true));
        }
        when(engine.getProperties()).thenReturn(properties);
        return engine;
    }

    @Test
    public void dataFileUpgradeRequired() {
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine =
            onPremise("Lite", Arrays.asList("Premium", "Enterprise"), true);

        assertEquals(
            PREFIX + "This is because your license or data file does not " +
                "include this property. It is available with the " +
                "Premium,Enterprise license/data for the " +
                engine.getClass().getSimpleName() + ". To upgrade, see " +
                "https://51degrees.com/pricing.",
            withoutCampaign(service.getMissingPropertyReason(
                "testProperty", engine).getDescription()));
    }

    @Test
    public void dataFileUpgradeRequired_TiersNotKnown() {
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine =
            onPremise("Lite", Collections.<String>emptyList(), true);

        assertEquals(
            PREFIX + "This is because your license or data file does not " +
                "include this property. To see which licenses include it, " +
                "see https://51degrees.com/pricing.",
            withoutCampaign(service.getMissingPropertyReason(
                "testProperty", engine).getDescription()));
    }

    @Test
    public void propertyExcluded() {
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine =
            onPremise("Premium", Arrays.asList("Premium"), false);

        assertEquals(
            PREFIX + "This is because the property has been excluded when " +
                "configuring the engine.",
            withoutCampaign(service.getMissingPropertyReason(
                "testProperty", engine).getDescription()));
    }

    @Test
    public void productNotAccessibleWithResourceKey() {
        assertEquals(
            PREFIX + "This is because your resource key does not include " +
                "access to any properties under 'testElement'. Check or " +
                "create a resource key with the properties you need in the " +
                "configurator at https://configure.51degrees.com.",
            withoutCampaign(service.getMissingPropertyReason(
                "testProperty", cloud()).getDescription()));
    }

    @Test
    public void propertyNotAccessibleWithResourceKey() {
        assertEquals(
            PREFIX + "This is because your resource key does not include " +
                "access to this property. The properties included for your " +
                "key under 'testElement' are ismobile,browsername. Check or " +
                "change your key in the configurator at " +
                "https://configure.51degrees.com.",
            withoutCampaign(service.getMissingPropertyReason(
                "testProperty",
                cloud("ismobile", "browsername")).getDescription()));
    }

    @Test
    public void cloudRequestFailed() throws Exception {
        // Read by name, so the other messages are still checked where the
        // text is not there at all.
        String text = (String) Constants.MissingPropertyMessages.class
            .getField("CLOUD_REQUEST_FAILED").get(null);
        assertEquals(
            "This is because an upstream cloud request failed, so no data " +
                "was available to populate the property.",
            withoutCampaign(text));
    }

    @Test
    public void unknown() {
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine =
            onPremise("Premium", Arrays.asList("Premium"), true);

        assertEquals(
            PREFIX + "The reason for this is unknown. Please check that the " +
                "aspect and property name are correct.",
            withoutCampaign(service.getMissingPropertyReason(
                "testProperty", engine).getDescription()));
    }
}
