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

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Every Flow Element has to give the Evidence keys it uses, the Properties
 * it populates and its Data Key, because the FlowElement interface declares
 * all three without a default, so no element compiles without them. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/conceptual-overview.md#L73">conceptual-overview.md line 73</a>:
 * "Each Element MUST:" advertize the Evidence keys that it can make use of,
 * publicize details of the Properties it will populate, and expose the
 * string Data Key that is used to identify its Element Data within Flow
 * Data.
 */
public class FlowElementContractTests {

    private static void assertRequired(
        String name, Class<?> returnType) throws Exception {
        Method method = FlowElement.class.getMethod(name);
        assertTrue(name + " is not public.",
            Modifier.isPublic(method.getModifiers()));
        assertTrue(name + " is not abstract, so an element need not give it.",
            Modifier.isAbstract(method.getModifiers()));
        assertFalse(name + " has a default.", method.isDefault());
        assertEquals(returnType, method.getReturnType());
    }

    @Test
    public void evidenceKeys_AreRequired() throws Exception {
        assertRequired("getEvidenceKeyFilter", EvidenceKeyFilter.class);
    }

    @Test
    public void properties_AreRequired() throws Exception {
        assertRequired("getProperties", List.class);
    }

    @Test
    public void dataKey_IsRequired() throws Exception {
        assertRequired("getElementDataKey", String.class);
    }
}
