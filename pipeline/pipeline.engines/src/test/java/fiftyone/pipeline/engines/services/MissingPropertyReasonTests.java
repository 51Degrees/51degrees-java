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

import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The reason a property is missing, decided for each engine in the order
 * the specification gives. The data tier is compared for an On-premise
 * Engine only. A Cloud Engine whose metadata is not loaded gives
 * CloudRequestFailed when its request for the property list failed, and
 * Unknown otherwise. An On-premise Engine whose metadata does not list the
 * property, but whose Element Data has a typed accessor for it, gives
 * DataFileUpgradeRequired with no data tiers. When every engine gives
 * Unknown, the message names the last engine.
 */
public class MissingPropertyReasonTests {

    /**
     * A Cloud Engine that says whether its request for the property list
     * failed.
     */
    public abstract static class PropertyListCloudEngine
        implements CloudAspectEngine<AspectData, AspectPropertyMetaData> {
        public abstract boolean propertyListRequestFailed();
    }

    /**
     * Element Data whose typed accessor for HardwareVendor is inherited.
     */
    public interface BaseDeviceData extends AspectData {
        AspectPropertyValue<String> getHardwareVendor();
    }

    public interface DeviceData extends BaseDeviceData {
    }

    private static final String PREFIX =
        "Property '%s' not found in data for element '%s'. ";

    private static final String CLOUD_REQUEST_FAILED =
        "This is because an upstream cloud request failed, so no data was " +
        "available to populate the property.";

    private static final String UNKNOWN =
        "The reason for this is unknown. Please check that the aspect and " +
        "property name are correct.";

    private static final Pattern CAMPAIGN =
        Pattern.compile("\\?utm_\\S*?(?=\\.(\\s|$)|\\s|$)");

    private final MissingPropertyService service =
        MissingPropertyServiceDefault.getInstance();

    @Test
    public void CloudEngine_PropertyListed_NoDataTierCheck() {
        CloudAspectEngine<?, ?> engine = cloud();
        listed(engine, "Premium", true);

        assertReason(
            "PropertyNotAccessibleWithResourceKey",
            service.getMissingPropertyReason("HardwareVendor", engine));
    }

    @Test
    public void OtherEngine_PropertyListed_NoDataTierCheck() {
        @SuppressWarnings("unchecked")
        AspectEngine<AspectData, AspectPropertyMetaData> engine =
            mock(AspectEngine.class);
        when(engine.getElementDataKey()).thenReturn("device");
        when(engine.getDataSourceTier()).thenReturn("Lite");
        listed(engine, "Premium", true);

        assertReason(
            "Unknown",
            service.getMissingPropertyReason("HardwareVendor", engine));
    }

    @Test
    public void OnPremiseEngine_PropertyListed_DataTierChecked() {
        AspectEngine<?, ?> engine = onPremise(null);
        listed(engine, "Premium", true);

        assertReason(
            "DataFileUpgradeRequired",
            service.getMissingPropertyReason("hardwarevendor", engine));
    }

    @Test
    public void CloudEngine_PropertyListedNotAvailable_Excluded() {
        CloudAspectEngine<?, ?> engine = cloud();
        listed(engine, "Premium", false);

        assertReason(
            "PropertyExcludedFromEngineConfiguration",
            service.getMissingPropertyReason("HardwareVendor", engine));
    }

    @Test
    public void CloudEngine_MetadataNotLoaded_ListRequestFailed() {
        PropertyListCloudEngine engine = notLoaded(true);

        MissingPropertyResult result =
            service.getMissingPropertyReason("HardwareVendor", engine);

        assertReason("CloudRequestFailed", result);
        assertEquals(
            String.format(PREFIX, "HardwareVendor", "device") +
                CLOUD_REQUEST_FAILED,
            result.getDescription());
    }

    @Test
    public void CloudEngine_MetadataNotLoaded_ListRequestNotFailed() {
        PropertyListCloudEngine engine = notLoaded(false);

        assertReason(
            "Unknown",
            service.getMissingPropertyReason("HardwareVendor", engine));
    }

    @Test
    public void OnPremiseEngine_NotListed_TypedAccessor_TiersNotKnown() {
        AspectEngine<?, ?> engine = onPremise(DeviceData.class);
        listed(engine, "Premium", true, "IsMobile");

        MissingPropertyResult result =
            service.getMissingPropertyReason("hardwarevendor", engine);

        assertReason("DataFileUpgradeRequired", result);
        assertEquals(
            String.format(PREFIX, "hardwarevendor", "device") +
                "This is because your license or data file does not " +
                "include this property. To see which licenses include it, " +
                "see https://" + "51degrees.com/pricing.",
            CAMPAIGN.matcher(result.getDescription()).replaceAll(""));
    }

    @Test
    public void OnPremiseEngine_NotListed_NoTypedAccessor_Unknown() {
        AspectEngine<?, ?> engine = onPremise(DeviceData.class);
        listed(engine, "Premium", true, "IsMobile");

        assertReason(
            "Unknown",
            service.getMissingPropertyReason("PlatformName", engine));
    }

    @Test
    public void CloudEngine_NotListed_TypedAccessor_NotAnUpgrade() {
        CloudAspectEngine<?, ?> engine = cloud();
        doReturn(new TypedKeyDefault<>("device", DeviceData.class))
            .when(engine).getTypedDataKey();
        listed(engine, "Premium", true, "IsMobile");

        assertReason(
            "PropertyNotAccessibleWithResourceKey",
            service.getMissingPropertyReason("HardwareVendor", engine));
    }

    @Test
    public void EveryEngineUnknown_MessageNamesTheLastEngine() {
        AspectEngine<?, ?> first = onPremise(null);
        when(first.getElementDataKey()).thenReturn("first");
        listed(first, "Premium", true, "IsMobile");
        AspectEngine<?, ?> last = onPremise(null);
        when(last.getElementDataKey()).thenReturn("last");
        listed(last, "Premium", true, "IsMobile");
        List<AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData>>
            engines = new ArrayList<>();
        engines.add(first);
        engines.add(last);

        MissingPropertyResult result =
            service.getMissingPropertyReason("PlatformName", engines);

        assertReason("Unknown", result);
        assertEquals(
            String.format(PREFIX, "PlatformName", "last") + UNKNOWN,
            result.getDescription());
    }

    private static void assertReason(
        String expected,
        MissingPropertyResult result) {
        assertEquals(
            result.getDescription(),
            expected,
            result.getReason().name());
    }

    @SuppressWarnings("unchecked")
    private static CloudAspectEngine<?, ?> cloud() {
        CloudAspectEngine<AspectData, AspectPropertyMetaData> engine =
            mock(CloudAspectEngine.class);
        when(engine.getElementDataKey()).thenReturn("device");
        when(engine.getDataSourceTier()).thenReturn("Lite");
        when(engine.hasLoadedProperties()).thenReturn(true);
        return engine;
    }

    private static PropertyListCloudEngine notLoaded(boolean failed) {
        PropertyListCloudEngine engine = mock(PropertyListCloudEngine.class);
        when(engine.getElementDataKey()).thenReturn("device");
        when(engine.hasLoadedProperties()).thenReturn(false);
        when(engine.propertyListRequestFailed()).thenReturn(failed);
        when(engine.getProperties()).thenReturn(
            Collections.<AspectPropertyMetaData>emptyList());
        return engine;
    }

    @SuppressWarnings("unchecked")
    private static AspectEngine<?, ?> onPremise(Class<?> dataType) {
        OnPremiseAspectEngine<AspectData, AspectPropertyMetaData> engine =
            mock(OnPremiseAspectEngine.class);
        when(engine.getElementDataKey()).thenReturn("device");
        when(engine.getDataSourceTier()).thenReturn("Lite");
        when(engine.hasLoadedProperties()).thenReturn(true);
        if (dataType != null) {
            doReturn(new TypedKeyDefault<>("device", dataType))
                .when(engine).getTypedDataKey();
        }
        return engine;
    }

    private static void listed(
        AspectEngine<?, ?> engine,
        String tier,
        boolean available) {
        listed(engine, tier, available, "HardwareVendor");
    }

    private static void listed(
        AspectEngine<?, ?> engine,
        String tier,
        boolean available,
        String name) {
        AspectPropertyMetaData property = new AspectPropertyMetaDataDefault(
            name,
            engine,
            "",
            String.class,
            Arrays.asList(tier),
            available);
        doReturn(Arrays.asList(property)).when(engine).getProperties();
    }
}
