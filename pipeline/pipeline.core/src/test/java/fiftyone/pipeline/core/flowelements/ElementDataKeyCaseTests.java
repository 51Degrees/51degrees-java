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

package fiftyone.pipeline.core.flowelements;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.testclasses.data.TestElementData;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * The string key of an Element Data instance is compared without regard to
 * case.
 * <p>
 * Specification: pipeline-specification/features/access-to-results.md line 11
 * at 51Degrees/specifications 706de1f0, "String comparisons MUST be case
 * insensitive."
 */
public class ElementDataKeyCaseTests {

    private FlowDataDefault flowData;
    private ElementData data;

    @Before
    public void init() {
        flowData = new FlowDataDefault(
            mock(Logger.class),
            mock(PipelineInternal.class),
            new EvidenceDefault(mock(Logger.class)));
        // Element Data is added before processing, because a Flow Data
        // that has finished processing refuses new Element Data. Each test
        // processes once its own adds are done.
        data = flowData.getOrAdd(
            "device",
            (FlowElement.DataFactory<ElementData>) fd ->
                new TestElementData(mock(Logger.class), fd));
    }

    @After
    public void close() throws Exception {
        flowData.close();
    }

    @Test
    public void get_StringKeyInAnotherCase_ReturnsTheData() {
        flowData.process();

        assertSame(data, flowData.get("Device"));
        assertSame(data, flowData.get("DEVICE"));
    }

    @Test
    public void get_TypedKeyInAnotherCase_ReturnsTheData() {
        flowData.process();
        TypedKey<TestElementData> key = new TypedKeyDefault<>(
            "Device",
            TestElementData.class);

        assertSame(data, flowData.get(key));
        TryGetResult<TestElementData> result = flowData.tryGetValue(key);
        assertTrue(result.hasValue());
        assertSame(data, result.getValue());
    }

    @Test
    public void getOrAdd_KeyInAnotherCase_KeepsOneInstance() {
        ElementData again = flowData.getOrAdd(
            "DEVICE",
            (FlowElement.DataFactory<ElementData>) fd ->
                new TestElementData(mock(Logger.class), fd));
        flowData.process();

        assertSame(data, again);
        assertEquals(1, flowData.elementDataAsMap().size());
    }

    @Test
    public void get_KeyInAnotherCase_TurkishLocale_ReturnsTheData() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            ElementData ip = flowData.getOrAdd(
                "ip",
                (FlowElement.DataFactory<ElementData>) fd ->
                    new TestElementData(mock(Logger.class), fd));
            flowData.process();
            assertSame(ip, flowData.get("IP"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
