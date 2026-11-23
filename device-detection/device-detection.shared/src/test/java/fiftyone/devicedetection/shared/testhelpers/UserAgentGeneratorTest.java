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

package fiftyone.devicedetection.shared.testhelpers;

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

/**
 * The User-Agent values come from a YAML evidence file, which holds one
 * record of evidence in each document.
 */
public class UserAgentGeneratorTest {

    private static final String FIRST =
        "Mozilla/5.0 (Sample; First) Sample/1.0";
    private static final String QUOTED =
        "Mozilla/5.0 (compatible; Sample; id: second)";
    private static final String THIRD = "Sample/3.0";

    private File evidence;

    @Before
    public void writeEvidence() throws Exception {
        evidence = File.createTempFile("evidence", ".yml");
        Files.write(evidence.toPath(), Arrays.asList(
            "---",
            "header.user-agent: " + FIRST,
            "header.sec-ch-ua-mobile: ?0",
            "---",
            "header.user-agent: '" + QUOTED + "'",
            "---",
            "header.sec-ch-ua-platform: '\"Sample\"'",
            "---",
            "{}",
            "---",
            "header.sec-ch-ua-mobile: ?1",
            "header.user-agent: " + THIRD,
            "..."), StandardCharsets.UTF_8);
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
    public void getIterable_GivesTheUserAgentOfEachRecordThatHasOne()
        throws Exception {
        UserAgentGenerator generator = new UserAgentGenerator(evidence);

        assertEquals(Arrays.asList(FIRST, QUOTED, THIRD),
            list(generator.getIterable(10)));
    }

    @Test
    public void getIterable_StopsAtTheCountAskedFor() throws Exception {
        UserAgentGenerator generator = new UserAgentGenerator(evidence);

        assertEquals(Arrays.asList(FIRST, QUOTED),
            list(generator.getIterable(2)));
    }

    @Test
    public void getIterable_GivesOnlyTheUserAgentsThatMatch()
        throws Exception {
        UserAgentGenerator generator = new UserAgentGenerator(evidence);

        assertEquals(Arrays.asList(FIRST, QUOTED),
            list(generator.getIterable(10, "Mozilla.*")));
    }

    @Test
    public void getRandomUserAgent_GivesAUserAgentFromTheFile()
        throws Exception {
        UserAgentGenerator generator = new UserAgentGenerator(evidence);

        for (int i = 0; i < 20; i++) {
            String value = generator.getRandomUserAgent(0);
            assertEquals(true,
                Arrays.asList(FIRST, QUOTED, THIRD).contains(value));
        }
    }
}
