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

package fiftyone.pipeline.translation.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MissingTranslationBehaviorTests {

    @ParameterizedTest
    @CsvSource({
        "Original, ORIGINAL",
        "original, ORIGINAL",
        "ORIGINAL, ORIGINAL",
        "EmptyString, EMPTY_STRING",
        "EMPTYSTRING, EMPTY_STRING",
        "empty_string, EMPTY_STRING",
        "FlowError, FLOW_ERROR",
        "flowerror, FLOW_ERROR",
        "FLOW_ERROR, FLOW_ERROR"
    })
    public void parse_SpecificationAndConstantNames_IgnoringCase(
            String text, MissingTranslationBehavior expected) {
        assertEquals(expected, MissingTranslationBehavior.parse(text));
    }

    @Test
    public void parse_WhiteSpaceAroundTheText_IsIgnored() {
        assertEquals(MissingTranslationBehavior.FLOW_ERROR,
            MissingTranslationBehavior.parse("  FlowError  "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Sometimes", "Empty String", "Flow-Error"})
    public void parse_AnythingElse_IsRefusedNamingTheAcceptedValues(
            String text) {
        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> MissingTranslationBehavior.parse(text));
        assertEquals("'" + text + "' is not a missing translation " +
            "behavior. Use Original, EmptyString or FlowError.",
            thrown.getMessage());
    }
}
