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

package fiftyone.pipeline.javascriptbuilder.flowelements;

import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SetHeadersElementBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageBuilder;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The string key of each Pipeline API element's data contains only
 * alphanumeric characters, full stop or hyphen.
 * <p>
 * Specification: pipeline-specification/features/access-to-results.md line 7
 * at 51Degrees/specifications 706de1f0, "Strings SHOULD only contain
 * alphanumeric characters, full stop or hyphen."
 */
public class ElementDataKeyFormatTests {

    private static List<FlowElement<?, ?>> elements() throws Exception {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        List<FlowElement<?, ?>> elements = new ArrayList<>();
        elements.add(new JavaScriptBuilderElementBuilder(loggerFactory).build());
        elements.add(new JsonBuilderElementBuilder(loggerFactory).build());
        elements.add(new SequenceElementBuilder(loggerFactory).build());
        elements.add(new SetHeadersElementBuilder(loggerFactory).build());
        elements.add(new ShareUsageBuilder(loggerFactory).build());
        return elements;
    }

    @Test
    public void keys_OnlyAlphanumericFullStopOrHyphen() throws Exception {
        for (FlowElement<?, ?> element : elements()) {
            try {
                String key = element.getElementDataKey();
                assertTrue(key.matches("[A-Za-z0-9.-]+"),
                    element.getClass().getSimpleName() + " key '" + key + "'");
            } finally {
                element.close();
            }
        }
    }

    /**
     * Specification: access-to-results.md line 10, "Lower-case SHOULD be
     * used."
     */
    @Test
    public void keys_LowerCase() throws Exception {
        for (FlowElement<?, ?> element : elements()) {
            try {
                String key = element.getElementDataKey();
                assertTrue(key.equals(key.toLowerCase(Locale.ROOT)),
                    element.getClass().getSimpleName() + " key '" + key + "'");
            } finally {
                element.close();
            }
        }
    }
}
