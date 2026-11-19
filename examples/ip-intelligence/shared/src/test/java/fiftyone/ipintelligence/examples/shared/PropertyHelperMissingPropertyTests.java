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

package fiftyone.ipintelligence.examples.shared;

import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.services.MissingPropertyReason;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A Property the data file or resource key in use cannot give is answered
 * with plain text that names it and gives the tagged contact link, because
 * the examples print the answer to a console, where HTML is read as it is
 * written.
 */
public class PropertyHelperMissingPropertyTests {

    private static final String CONTACT_LINK =
        "https://51degrees.com/contact-us?utm_source=code" +
        "&utm_medium=example&utm_campaign=51degrees-java" +
        "&utm_content=shared-src-main-java-fiftyone-ipintelligence-examples-shared-propertyhelper.java";

    private static AspectPropertyValue<String> missing() {
        return PropertyHelper.tryGet(() -> {
            throw new PropertyMissingException(
                MissingPropertyReason.DataFileUpgradeRequired,
                "HardwareVendor",
                "HardwareVendor is not in this data file.");
        });
    }

    @Test
    public void MissingProperty_IsPlainText() {
        String message = missing().getNoValueMessage();
        assertFalse("The message holds HTML: " + message,
            message.contains("<") || message.contains("&amp;"));
    }

    @Test
    public void MissingProperty_NamesItAndGivesTheContactLink() {
        String message = missing().getNoValueMessage();
        assertTrue("The message does not name the property: " + message,
            message.contains("'HardwareVendor'"));
        assertTrue("The message has no tagged contact link: " + message,
            message.contains(CONTACT_LINK));
    }

    @Test
    public void PricingMessage_GivesTheContactLink() {
        assertTrue("The advisory has no tagged contact link: " +
                PropertyHelper.PRICING_MESSAGE,
            PropertyHelper.PRICING_MESSAGE.contains(CONTACT_LINK));
        assertFalse("The advisory holds HTML: " +
                PropertyHelper.PRICING_MESSAGE,
            PropertyHelper.PRICING_MESSAGE.contains("<"));
    }
}
