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

package fiftyone.ipintelligence.examples.web;

import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngineBuilder;
import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.ipintelligence.examples.shared.FindOutMore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

/**
 * The service answers in JSON, so what it writes when it starts is the only
 * place the person running it reads what the Enterprise data file adds and
 * where to find out more.
 */
public class GettingStartedApiNotesTest {

    private static final String CONTACT_US = "https://51degrees.com/contact-us?";

    /**
     * On the free Lite data file, the service says what the Enterprise data
     * file adds with the link to the contact page, then writes the "Find out
     * more" section, closed by its rule.
     */
    @Test
    public void startupNotes_FreeDataFile() throws Exception {
        String lite = DataFileHelper.getDataFileLocation(
                DataFileHelper.LITE_DATA_FILE_REL_PATH);
        try (IPIntelligenceOnPremiseEngine engine =
                     new IPIntelligenceOnPremiseEngineBuilder(
                             LoggerFactory.getILoggerFactory())
                             .setAutoUpdate(false)
                             .build(lite, false)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            GettingStartedApi.writeStartupNotes(
                    new PrintStream(buffer, true, "UTF-8"), engine);
            String output = buffer.toString("UTF-8");

            assertTrue("The output should say what the Enterprise data " +
                            "file adds:\n" + output,
                    output.contains("This example used a free data file."));
            assertTrue("The output should link the contact page:\n" + output,
                    output.contains(CONTACT_US));
            int heading = output.lastIndexOf(FindOutMore.HEADING);
            assertTrue("The output should have the '" + FindOutMore.HEADING +
                            "' section after the link to the contact " +
                            "page:\n" + output,
                    heading > output.lastIndexOf(CONTACT_US));
            assertTrue("The '" + FindOutMore.HEADING +
                            "' section should end with its rule:\n" + output,
                    output.indexOf(FindOutMore.RULE, heading) > heading);
        }
    }
}
