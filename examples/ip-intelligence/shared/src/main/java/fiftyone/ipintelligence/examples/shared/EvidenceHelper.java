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

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class EvidenceHelper {

    /**
     * Prepare evidence for use in examples
     */
    public static List<Map<String, String>> setUpEvidence() {
        Map<String, String> evidence1 = new HashMap<>();
        evidence1.put("query.client-ip", "116.154.188.222");
        Map<String, String> evidence2 = new HashMap<>();
        evidence2.put("query.client-ip", "45.236.48.61");
        Map<String, String> evidence3 = new HashMap<>();
        evidence3.put("query.client-ip", "2001:0db8:085a:0000:0000:8a2e:0370:7334");

        List<Map<String, String>> evidence = new ArrayList<>();
        evidence.add(evidence1);
        evidence.add(evidence2);
        evidence.add(evidence3);
        return evidence;
    }

    /**
     * Load a Yaml file as a list of documents (each being a Map containing evidence)
     * @param yamlFile a yaml file
     * @param max maximum entries
     * @return a List
     * @throws IOException in case of error
     */
    public static List<Map<String, String>> getEvidenceList(File yamlFile, int max) throws IOException {
        return StreamSupport.stream(getEvidenceIterable(yamlFile).spliterator(), false)
                .limit(max)
                .collect(Collectors.toList());
    }

    /**
     * Create an Iterable<Map<String, String>> for reading documents from the passed yamlFile
     * @param yamlFile a yamlFile
     * @return an Iterable
     * @throws IOException for file errors
     */
    @SuppressWarnings("unchecked")
    public static Iterable<Map<String, String>> getEvidenceIterable(File yamlFile) throws IOException {
        final Iterator<Object> objectIterator =
                new Yaml().loadAll(Files.newInputStream(yamlFile.toPath())).iterator();
        return () -> new Iterator<Map<String, String>>() {
            @Override
            public boolean hasNext() {
                return objectIterator.hasNext();
            }

            @Override
            public Map<String, String> next() {
                return (Map<String, String>) objectIterator.next();
            }
        };
    }

}
