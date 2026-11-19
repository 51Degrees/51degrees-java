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

package pipeline.developerexamples.clientsideelement;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.developerexamples.clientsideelement.data.StarSignData;
import fiftyone.pipeline.developerexamples.clientsideelement.flowelements.SimpleClientSideElement;
import fiftyone.pipeline.developerexamples.clientsideelement.flowelements.SimpleClientSideElementBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The client-side element is one of the core's demonstrations of how to
 * create a Flow Element (pipeline-specification/required-examples.md, "The
 * examples that MUST be implemented in the core are all demonstrations of
 * how to create Flow Elements"). The web example that shows it needs a
 * browser, so this test builds the element as that example does, puts it in
 * a Pipeline and reads what it gives for each kind of evidence.
 */
public class ExampleTests {

    private static StarSignData process(String dateOfBirth) throws Exception {
        SimpleClientSideElement element =
            new SimpleClientSideElementBuilder(
                LoggerFactory.getILoggerFactory()).build();
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElement(element)
                .build();
             FlowData data = pipeline.createFlowData()) {
            if (dateOfBirth != null) {
                data.addEvidence("cookie.date-of-birth", dateOfBirth);
            }
            data.process();
            return data.getFromElement(element);
        }
    }

    @Test
    public void NoDateOfBirth_AsksForIt() throws Exception {
        StarSignData result = process(null);
        assertEquals("Unknown", result.getStarSign());
        assertTrue(result.getDobJavaScript().toString()
            .contains("date-of-birth"));
    }

    @Test
    public void DateOfBirth_GivesStarSign() throws Exception {
        StarSignData result = process("01/04/1990");
        assertEquals("Aries", result.getStarSign());
        assertEquals("", result.getDobJavaScript().toString());
    }

    @Test
    public void FirstAndLastDaysOfASign_GiveThatSign() throws Exception {
        assertEquals("Aries", process("21/03/1990").getStarSign());
        assertEquals("Aries", process("19/04/1990").getStarSign());
        assertEquals("Pisces", process("20/03/1990").getStarSign());
    }

    @Test
    public void SignAcrossTheYearEnd_GivesThatSign() throws Exception {
        assertEquals("Capricorn", process("22/12/1990").getStarSign());
        assertEquals("Capricorn", process("05/01/1990").getStarSign());
        assertEquals("Capricorn", process("19/01/1990").getStarSign());
    }
}
