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

import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngine;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Each missing property message is the text the specification's table of
 * reasons gives, with campaign parameters allowed on each link. Lists in a
 * message are joined with a comma and no space.
 */
public class MissingPropertyMessageTests {

    private static final String DOMAIN = "51degrees.com";

    private static final String PRICING = "https://" + DOMAIN + "/pricing";

    private static final String CONFIGURATOR = "https://configure." + DOMAIN;

    /**
     * The campaign parameters on a link, up to the end of the link, which
     * is a space, the end of the message, or a full stop that ends it.
     */
    private static final Pattern CAMPAIGN =
        Pattern.compile("\\?utm_\\S*?(?=\\.(\\s|$)|\\s|$)");

    private static final String PROPERTY = "HardwareVendor";

    private static final String DATA_KEY = "device";

    private static final String PREFIX = "Property '" + PROPERTY +
        "' not found in data for element '" + DATA_KEY + "'. ";

    private final MissingPropertyService service =
        MissingPropertyServiceDefault.getInstance();

    @Test
    public void DataFileUpgradeRequired_TiersKnown() {
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine =
            onPremise("Lite");
        listed(engine, Arrays.asList("Premium", "Enterprise"), true);

        assertMessage(
            PREFIX + "This is because your license or data file does not " +
                "include this property. It is available with the " +
                "Premium,Enterprise license/data for the " +
                engine.getClass().getSimpleName() + ". To upgrade, see " +
                PRICING + ".",
            engine);
    }

    @Test
    public void DataFileUpgradeRequired_TiersNotKnown() {
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine =
            onPremise("Lite");
        listed(engine, Collections.<String>emptyList(), true);

        assertMessage(
            PREFIX + "This is because your license or data file does not " +
                "include this property. To see which licenses include it, " +
                "see " + PRICING + ".",
            engine);
    }

    @Test
    public void PropertyExcludedFromEngineConfiguration() {
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine =
            onPremise("Premium");
        listed(engine, Arrays.asList("Premium", "Enterprise"), false);

        assertMessage(
            PREFIX + "This is because the property has been excluded when " +
                "configuring the engine.",
            engine);
    }

    @Test
    public void ProductNotAccessibleWithResourceKey() {
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine =
            cloud(Collections.<String>emptyList());

        assertMessage(
            PREFIX + "This is because your resource key does not include " +
                "access to any properties under '" + DATA_KEY + "'. Check " +
                "or create a resource key with the properties you need in " +
                "the configurator at " + CONFIGURATOR + ".",
            engine);
    }

    @Test
    public void PropertyNotAccessibleWithResourceKey() {
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine =
            cloud(Arrays.asList("IsMobile", "IsTablet"));

        assertMessage(
            PREFIX + "This is because your resource key does not include " +
                "access to this property. The properties included for your " +
                "key under '" + DATA_KEY + "' are IsMobile,IsTablet. Check " +
                "or change your key in the configurator at " +
                CONFIGURATOR + ".",
            engine);
    }

    @Test
    public void Unknown() {
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine =
            onPremise("Premium");
        when(engine.getProperties()).thenReturn(
            Collections.<AspectPropertyMetaData>emptyList());

        assertMessage(
            PREFIX + "The reason for this is unknown. Please check that the " +
                "aspect and property name are correct.",
            engine);
    }

    private void assertMessage(
        String expected,
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine) {
        String message = service.getMissingPropertyReason(PROPERTY, engine)
            .getDescription();
        assertEquals(expected, CAMPAIGN.matcher(message).replaceAll(""));
    }

    @SuppressWarnings("unchecked")
    private static AspectEngine<? extends AspectData, AspectPropertyMetaData>
        onPremise(String tier) {
        OnPremiseAspectEngine<? extends AspectData, AspectPropertyMetaData>
            engine = mock(OnPremiseAspectEngine.class);
        when(engine.getDataSourceTier()).thenReturn(tier);
        when(engine.getElementDataKey()).thenReturn(DATA_KEY);
        return engine;
    }

    /**
     * A Cloud Engine whose metadata is loaded and lists the given names.
     */
    @SuppressWarnings("unchecked")
    private static AspectEngine<? extends AspectData, AspectPropertyMetaData>
        cloud(List<String> names) {
        CloudAspectEngine<? extends AspectData, AspectPropertyMetaData>
            engine = mock(CloudAspectEngine.class);
        when(engine.getElementDataKey()).thenReturn(DATA_KEY);
        when(engine.hasLoadedProperties()).thenReturn(true);
        List<AspectPropertyMetaData> properties = new ArrayList<>();
        for (String name : names) {
            properties.add(new AspectPropertyMetaDataDefault(
                name,
                engine,
                "",
                String.class,
                Collections.<String>emptyList(),
                true));
        }
        when(engine.getProperties()).thenReturn(properties);
        return engine;
    }

    /**
     * The engine's metadata lists the property, in the given data tiers.
     */
    private static void listed(
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine,
        List<String> tiers,
        boolean available) {
        AspectPropertyMetaData property = new AspectPropertyMetaDataDefault(
            PROPERTY,
            engine,
            "",
            String.class,
            tiers,
            available);
        when(engine.getProperties()).thenReturn(Arrays.asList(property));
    }
}
