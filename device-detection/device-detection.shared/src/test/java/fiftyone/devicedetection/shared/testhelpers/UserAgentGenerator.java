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

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Gives the tests User-Agent values from a YAML evidence file, which holds
 * one record of evidence in each document.
 */
public class UserAgentGenerator {

    /**
     * The name a record of evidence gives its User-Agent under.
     */
    private static final String USER_AGENT_KEY = "header.user-agent";

    private final List<String> userAgents;

    private Random random = new Random();

    /**
     * Reads the User-Agent of every record that has one, in the order of the
     * file.
     * @param evidenceFile the YAML evidence file
     * @throws IOException if the file cannot be read
     */
    public UserAgentGenerator(File evidenceFile) throws IOException {
        userAgents = new ArrayList<>();
        Load load = new Load(LoadSettings.builder().build());
        try (Reader reader = Files.newBufferedReader(
            evidenceFile.toPath(), StandardCharsets.UTF_8)) {
            for (Object record : load.loadAllFromReader(reader)) {
                if (record instanceof Map) {
                    Object value = ((Map<?, ?>) record).get(USER_AGENT_KEY);
                    if (value != null) {
                        userAgents.add(value.toString());
                    }
                }
            }
        }
    }

    public String getRandomUserAgent(int randomness) {
        String value = userAgents.get(random.nextInt(userAgents.size()));
        if (randomness > 0) {
            byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
            for (int i = 0; i < randomness; i++) {
                int indexA = random.nextInt(value.length());
                int indexB = random.nextInt(value.length());
                byte temp = bytes[indexA];
                bytes[indexA] = bytes[indexB];
                bytes[indexB] = temp;
            }
            value = new String(bytes, StandardCharsets.US_ASCII);
        }
        return value;
    }

    public Iterable<String> getIterable(int count, int randomness) {
        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(getRandomUserAgent(randomness));
        }
        return result;
    }

    public Iterable<String> getIterable(int count, String pattern) {
        int counter = 0;
        int index = 0;
        List<String> result = new ArrayList<>();
        Pattern regex = Pattern.compile(pattern);
        while (counter < count && index < userAgents.size()) {
            if (regex.matcher(userAgents.get(index)).matches()) {
                result.add(userAgents.get(index));
                counter++;
            }
            index++;
        }
        return result;
    }

    public Iterable<String> getIterable(int count) {
        return getIterable(count, "(.*?)");
    }

    public Iterable<String> getRandomUserAgents(int count) {
        return getIterable(count);
    }
}
