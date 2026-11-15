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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngine;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The reason a Property is missing is decided in the specification's order.
 * <p>
 * Specification: pipeline-specification/features/properties.md line 143 at
 * 51Degrees/specifications 706de1f0, "An implementation SHOULD decide the
 * reason in this order, using the first that applies.", for steps 1 to 3.4
 * at lines 146 to 177.
 */
public class MissingPropertyReasonOrderTests {

    private final MissingPropertyService service =
        MissingPropertyServiceDefault.getInstance();

    private final FlowData flowData = mock(FlowData.class);

    /**
     * An Element Data type with a typed accessor of its own and one it
     * inherits.
     */
    public interface InheritedAccessors extends AspectData {
        AspectPropertyValue<Boolean> getIsInherited();
    }

    public interface TestData extends InheritedAccessors {
        AspectPropertyValue<String> getOwnAccessor();
    }

    @SafeVarargs
    private static List<AspectEngine<? extends AspectData,
        ? extends AspectPropertyMetaData>> list(
        AspectEngine<? extends AspectData,
            ? extends AspectPropertyMetaData>... engines) {
        return Arrays.asList(engines);
    }

    @SuppressWarnings("unchecked")
    private static AspectEngine<TestData, AspectPropertyMetaData> onPremise(
        String tier,
        List<String> tiersWherePresent,
        boolean available) {
        AspectEngine<TestData, AspectPropertyMetaData> engine =
            mock(AspectEngine.class);
        when(engine.getDataSourceTier()).thenReturn(tier);
        when(engine.getElementDataKey()).thenReturn("onpremise");
        when(engine.getTypedDataKey()).thenReturn(
            new TypedKeyDefault<>("onpremise", TestData.class));
        when(engine.hasLoadedProperties()).thenReturn(true);
        List<AspectPropertyMetaData> properties = new ArrayList<>();
        properties.add(new AspectPropertyMetaDataDefault(
            "testProperty", engine, "", String.class,
            tiersWherePresent, available));
        when(engine.getProperties()).thenReturn(properties);
        return engine;
    }

    @SuppressWarnings("unchecked")
    private CloudAspectEngine<TestData, AspectPropertyMetaData> cloud(
        String key,
        boolean loaded,
        boolean propertiesRequestFailed,
        boolean requestFailed,
        String... names) {
        CloudAspectEngine<TestData, AspectPropertyMetaData> engine =
            mock(CloudAspectEngine.class);
        when(engine.getElementDataKey()).thenReturn(key);
        when(engine.getDataSourceTier()).thenReturn("Lite");
        when(engine.getTypedDataKey()).thenReturn(
            new TypedKeyDefault<>(key, TestData.class));
        when(engine.hasLoadedProperties()).thenReturn(loaded);
        when(engine.propertiesRequestFailed())
            .thenReturn(propertiesRequestFailed);
        when(engine.requestFailed(any(FlowData.class)))
            .thenReturn(requestFailed);
        List<AspectPropertyMetaData> properties = new ArrayList<>();
        for (String name : names) {
            properties.add(new AspectPropertyMetaDataDefault(
                name, engine, "", String.class,
                Collections.<String>emptyList(), true));
        }
        when(engine.getProperties()).thenReturn(properties);
        return engine;
    }

    private static String prefix(String property, String key) {
        return String.format(
            Constants.MissingPropertyMessages.PREFIX, property, key);
    }

    @Test
    public void step1_CloudRequestFailed_NamesTheFirstEngine() {
        MissingPropertyResult result = service.getMissingPropertyReason(
            "ismobile",
            list(
                cloud("first", true, false, true, "browsername"),
                cloud("second", true, false, true, "browsername")),
            flowData);

        assertEquals(MissingPropertyReason.CloudRequestFailed,
            result.getReason());
        assertEquals(
            prefix("ismobile", "first") +
                Constants.MissingPropertyMessages.CLOUD_REQUEST_FAILED,
            result.getDescription());
    }

    @Test
    public void step1_ComesBeforeTheMetadata() {
        MissingPropertyResult result = service.getMissingPropertyReason(
            "ismobile",
            list(cloud("device", true, false, true)),
            flowData);

        assertEquals(MissingPropertyReason.CloudRequestFailed,
            result.getReason());
    }

    @Test
    public void step2_EveryEngineUnknown_NamesTheLastEngine() {
        MissingPropertyResult result = service.getMissingPropertyReason(
            "other",
            list(
                onPremise("Premium", Arrays.asList("Premium"), true),
                cloud("last", false, false, false)),
            flowData);

        assertEquals(MissingPropertyReason.Unknown, result.getReason());
        assertEquals(
            prefix("other", "last") +
                Constants.MissingPropertyMessages.UNKNOWN,
            result.getDescription());
    }

    @Test
    public void step31_TierComparedIgnoringCase() {
        MissingPropertyResult result = service.getMissingPropertyReason(
            "TESTPROPERTY",
            onPremise("premium", Arrays.asList("Premium"), true));

        assertEquals(MissingPropertyReason.Unknown, result.getReason());
    }

    @Test
    public void step31_TierCheckNotAppliedToCloudEngines() {
        MissingPropertyResult result = service.getMissingPropertyReason(
            "browsername",
            cloud("device", true, false, false, "browsername"));

        assertEquals(MissingPropertyReason.Unknown, result.getReason());
    }

    @Test
    public void step32_CloudMetadataNotLoaded_RequestFailed() {
        MissingPropertyResult result = service.getMissingPropertyReason(
            "ismobile",
            cloud("device", false, true, false));

        assertEquals(MissingPropertyReason.CloudRequestFailed,
            result.getReason());
        assertEquals(
            prefix("ismobile", "device") +
                Constants.MissingPropertyMessages.CLOUD_REQUEST_FAILED,
            result.getDescription());
    }

    @Test
    public void step32_CloudMetadataNotLoaded_Unknown() {
        MissingPropertyResult result = service.getMissingPropertyReason(
            "ismobile",
            cloud("device", false, false, false));

        assertEquals(MissingPropertyReason.Unknown, result.getReason());
    }

    @Test
    public void step32_CloudMetadataLoadedAndEmpty_ProductNotAccessible() {
        MissingPropertyResult result = service.getMissingPropertyReason(
            "ismobile",
            cloud("device", true, false, false));

        assertEquals(
            MissingPropertyReason.ProductNotAccessibleWithResourceKey,
            result.getReason());
    }

    @Test
    public void step33_OnPremiseTypedAccessor_UpgradeWithTiersNotKnown() {
        MissingPropertyResult result = service.getMissingPropertyReason(
            "ownaccessor",
            onPremise("Lite", Arrays.asList("Lite"), true));

        assertEquals(MissingPropertyReason.DataFileUpgradeRequired,
            result.getReason());
        assertEquals(
            prefix("ownaccessor", "onpremise") + Constants
                .MissingPropertyMessages.DATA_UPGRADE_REQUIRED_TIERS_UNKNOWN,
            result.getDescription());
    }

    @Test
    public void step33_InheritedTypedAccessor_Counts() {
        MissingPropertyResult result = service.getMissingPropertyReason(
            "IsInherited",
            onPremise("Lite", Arrays.asList("Lite"), true));

        assertEquals(MissingPropertyReason.DataFileUpgradeRequired,
            result.getReason());
    }

    @Test
    public void step33_NotAppliedToCloudEngines() {
        MissingPropertyResult result = service.getMissingPropertyReason(
            "ownaccessor",
            cloud("device", false, false, false));

        assertEquals(MissingPropertyReason.Unknown, result.getReason());
    }
}
