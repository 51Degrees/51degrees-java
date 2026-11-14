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

package fiftyone.devicedetection.examples.console;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertTrue;

/**
 * The client IP address ShareUsageExample makes up for each record is
 * Evidence that an engine reads, so it is written with the digits 0 to 9
 * whatever the default locale.
 */
public class RandomIpTest {

    private static final int ADDRESSES = 20;

    @Test
    public void randomIp_ArabicDefaultLocale_AsciiDigits() {
        Locale saved = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("ar-SA"));
        try {
            for (int i = 0; i < ADDRESSES; i++) {
                String ip = ShareUsageExample.generateRandomIp();
                assertTrue(ip, ip.matches("[0-9]{1,3}(\.[0-9]{1,3}){3}"));
            }
        } finally {
            Locale.setDefault(saved);
        }
    }
}
