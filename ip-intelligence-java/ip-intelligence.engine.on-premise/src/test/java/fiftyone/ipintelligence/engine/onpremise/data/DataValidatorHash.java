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

package fiftyone.ipintelligence.engine.onpremise.data;

import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.ipintelligence.shared.testhelpers.data.DataValidator;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.NoValueException;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;

import java.util.Map;

import static org.junit.Assert.*;

public class DataValidatorHash implements DataValidator {

    private IPIntelligenceOnPremiseEngine engine;

    public DataValidatorHash(IPIntelligenceOnPremiseEngine engine) {
        this.engine = engine;
    }

    @Override
    public void validateData(FlowData data, boolean validEvidence) throws NoValueException {
        IPIntelligenceData elementData = data.getFromElement(engine);
        Map<String, Object> map = elementData.asKeyMap();
        for (FiftyOneAspectPropertyMetaData property :
                engine.getProperties()) {
            if (property.isAvailable()) {
                assertTrue(map.containsKey(property.getName()));
                AspectPropertyValue<?> value = (AspectPropertyValue<?>)map.get(property.getName());
                assertEquals(validEvidence, value.hasValue());
            }
        }
        assertNotNull(elementData.getRegisteredName());
        assertEquals(validEvidence, elementData.getRegisteredName().hasValue());
        if (validEvidence) {
            assertNotNull(elementData.getRegisteredName().getValue()); // todo  need to understand what these mean
        }
    }
}