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

import fiftyone.ipintelligence.translation.flowelements.CountryCodeTranslationEngineBuilder;
import fiftyone.pipeline.annotations.DefaultValue;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Every configuration setter of the country code translation engine
 * builder gives its default value beside it, with {@link DefaultValue}.
 * See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/reference-implementation-notes.md#L79">reference-implementation-notes.md line 79</a>:
 * "In all cases, there SHOULD be some mechanism to specify the default
 * values alongside configuration property setters."
 */
public class BuilderDefaultValueTests {

    @Test
    public void builderSetters_GiveTheirDefaults() {
        List<String> missing = new ArrayList<>();
        for (Method method :
            CountryCodeTranslationEngineBuilder.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) &&
                method.getName().startsWith("set") &&
                method.isSynthetic() == false &&
                method.isBridge() == false &&
                method.getAnnotation(DefaultValue.class) == null) {
                missing.add(method.getName() +
                    Arrays.toString(method.getParameterTypes()));
            }
        }
        assertEquals(new ArrayList<String>(), missing);
    }
}
