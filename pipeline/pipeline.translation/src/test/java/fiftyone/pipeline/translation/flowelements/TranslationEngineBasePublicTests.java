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

package fiftyone.pipeline.translation.flowelements;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TranslationEngineBase is a released public class with a public
 * constructor, so a patch release keeps it concrete and keeps that
 * constructor, and code that makes one with {@code new} still compiles.
 */
public class TranslationEngineBasePublicTests {

    @Test
    public void translationEngineBase_IsConcrete() {
        assertFalse(
            Modifier.isAbstract(TranslationEngineBase.class.getModifiers()),
            "TranslationEngineBase is abstract, so code that makes one " +
                "with new no longer compiles");
    }

    @Test
    public void translationEngineBase_HasPublicConstructor() {
        boolean found = false;
        for (Constructor<?> constructor :
            TranslationEngineBase.class.getConstructors()) {
            found |= Modifier.isPublic(constructor.getModifiers());
        }
        assertTrue(found, "TranslationEngineBase has no public constructor");
    }
}
