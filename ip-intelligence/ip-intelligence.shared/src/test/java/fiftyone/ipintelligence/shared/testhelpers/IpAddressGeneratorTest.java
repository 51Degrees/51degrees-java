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

package fiftyone.ipintelligence.shared.testhelpers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The IP addresses come from a YAML evidence file, which holds one record
 * of evidence in each document, as the file made for the tests does.
 */
public class IpAddressGeneratorTest {

    private static final String FIRST = "203.0.113.7";
    private static final String QUOTED = "198.51.100.24";
    private static final String V6 =
        "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

    private File evidence;

    @Before
    public void writeEvidence() throws Exception {
        evidence = File.createTempFile("evidence", ".yml");
        // The file the tests are given ends with an empty document.
        Files.write(evidence.toPath(), Arrays.asList(
            "---",
            "server.client-ip: " + FIRST,
            "---",
            "server.client-ip: '" + QUOTED + "'",
            "---",
            "header.user-agent: Sample/1.0",
            "---",
            "{}",
            "---",
            "server.client-ip: " + V6,
            "---"), StandardCharsets.UTF_8);
    }

    @After
    public void deleteEvidence() {
        evidence.delete();
    }

    private static List<String> list(Iterable<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }

    @Test
    public void getIterable_GivesTheAddressOfEachRecordThatHasOne()
        throws Exception {
        IpAddressGenerator generator = new IpAddressGenerator(evidence);

        assertEquals(Arrays.asList(FIRST, QUOTED, V6),
            list(generator.getIterable(10)));
    }

    @Test
    public void getIterable_StopsAtTheCountAskedFor() throws Exception {
        IpAddressGenerator generator = new IpAddressGenerator(evidence);

        assertEquals(Arrays.asList(FIRST, QUOTED),
            list(generator.getIterable(2)));
    }

    @Test
    public void getIterable_GivesOnlyTheAddressesThatMatch()
        throws Exception {
        IpAddressGenerator generator = new IpAddressGenerator(evidence);

        assertEquals(Arrays.asList(FIRST, QUOTED),
            list(generator.getIterable(10, "[0-9.]+")));
    }

    @Test
    public void getRandomIpAddress_GivesAnAddressFromTheFile()
        throws Exception {
        IpAddressGenerator generator = new IpAddressGenerator(evidence);

        for (int i = 0; i < 20; i++) {
            String value = generator.getRandomIpAddress(0);
            assertTrue(value,
                Arrays.asList(FIRST, QUOTED, V6).contains(value));
        }
    }
}
