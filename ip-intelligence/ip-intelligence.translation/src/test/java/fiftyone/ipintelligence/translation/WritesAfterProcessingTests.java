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

package fiftyone.ipintelligence.translation;

import fiftyone.ipintelligence.translation.data.ICountriesTranslationData;
import fiftyone.ipintelligence.translation.data.ICountryCodeTranslationData;
import fiftyone.ipintelligence.translation.flowelements.CountriesTranslationEngine;
import fiftyone.ipintelligence.translation.flowelements.CountryCodeTranslationEngine;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectDataBase;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.Collections;

import static fiftyone.ipintelligence.translation.testhelpers.Fixtures.apv;
import static fiftyone.ipintelligence.translation.testhelpers.Fixtures.build;
import static fiftyone.ipintelligence.translation.testhelpers.Fixtures.weightify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * The check, for the Flow Elements in this module, that processes each one,
 * then writes to its Element Data and expects the error: the
 * {@link CountryCodeTranslationEngine} and the
 * {@link CountriesTranslationEngine}, after a stand-in IP Intelligence
 * engine.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 */
public class WritesAfterProcessingTests {

    private static void assertFails(String what, ThrowingRunnable write) {
        assertThrows(what, IllegalStateException.class, write);
    }

    /**
     * Every write method of Element Data, and of Aspect Data, fails once its
     * Flow Element has finished.
     * @param data the Element Data of a finished Flow Element
     */
    static void assertWritesFail(ElementData data) {
        assertFails("put", () -> data.put("after", "value"));
        assertFails("populateFromMap", () -> data.populateFromMap(
            Collections.<String, Object>singletonMap("after", "value")));
        if (data instanceof AspectDataBase) {
            AspectDataBase aspect = (AspectDataBase) data;
            assertFails("setCloudRequestFailed", aspect::setCloudRequestFailed);
            assertFails("addEngine", () -> aspect.addEngine(null));
            assertFails("addProcessCallable",
                () -> aspect.addProcessCallable(null));
        }
    }

    @Test
    public void translationEngines_ElementDataRefusesWrites()
        throws Exception {
        try (Pipeline pipeline =
                build(apv(weightify("GB", "FR")), apv(weightify("GB", "FR")));
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "fr_FR");
            data.process();
            assertEquals(String.valueOf(data.getErrors()),
                0, data.getErrors().size());

            assertWritesFail(data.get(ICountryCodeTranslationData.class));
            assertWritesFail(data.get(ICountriesTranslationData.class));
        }
    }
}
