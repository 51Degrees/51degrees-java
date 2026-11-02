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

package fiftyone.devicedetection.examples.shared;

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
        evidence1.put("header.user-agent",
                "Mozilla/5.0 (Linux; Android 9; SAMSUNG SM-G960U) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "SamsungBrowser/10.1 Chrome/71.0.3578.99 Mobile " +
                        "Safari/537.36");
        Map<String, String> evidence2 = new HashMap<>();
        evidence2.put("header.user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/78.0.3904.108 Safari/537.36");
        Map<String, String> evidence3 = new HashMap<>();
        evidence3.put("header.user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/98.0.4758.102 Safari/537.36");
        evidence3.put("header.sec-ch-ua-mobile", "?0");
        evidence3.put("header.sec-ch-ua",
                "\" Not A; Brand\";v=\"99\", \"Chromium\";v=\"98\", " +
                        "\"Google Chrome\";v=\"98\"");
        evidence3.put("header.sec-ch-ua-platform", "\"Windows\"");
        evidence3.put("header.sec-ch-ua-platform-version", "\"14.0.0\"");

        Map<String, String> evidence4 = new HashMap<>();
        Map<String, String> evidence5 = new HashMap<>();

        /**
         * A note on User-Agent Client Hint representations:
         * There are 3 common ways to represent UACH:
         * - [HTTP header map](https://wicg.github.io/ua-client-hints/)
         * - getHighEntropyValues() JS API call result in JSON format
         * - Structured User Agent Object from OpenRTB 2.6
         *
         * Links:
         * -
         * [getHighEntropyValues()](https://developer.mozilla.org/en-US/docs/Web/API/NavigatorUAData/getHighEntropyValues)
         * -
         * [device.sua](https://51degrees.com/blog/openrtb-structured-user-agent-and-user-agent-client-hints?utm_source=code&utm_medium=example&utm_campaign=device-detection-java-examples&utm_content=shared-src-main-java-fiftyone-devicedetection-examples-shared-evidencehelper.java&utm_term=setupevidence)
         * - [OpenRTB 2.6
         * spec](https://github.com/InteractiveAdvertisingBureau/openrtb2.x/blob/main/2.6.md#objectuseragent)
         *
         * 51Degrees historically used HTTP header map to represent User-Agent Client Hints and expected the evidence to
         * be provided as HTTP headers (or same name query parameters).  The header
         * names in question are:
         * - Sec-CH-UA
         * - Sec-CH-UA-Platform
         * - Sec-CH-UA-Mobile
         * - Sec-CH-UA-Model
         * - Sec-CH-UA-Full-Version-List
         * - Sec-CH-UA-Platform-Version
         * - Sec-CH-UA-Arch
         * - Sec-CH-UA-Bitness
         *
         * However in version 4.5 we introduced the ability to perform device detection using the 2 other User-Agent
         * Client Hints representations as evidence (internally it is done through conversion to the HTTP-header
         * representation but it's an implementation detail).  The 2 evidence parameter names in question are:
         * `51D_gethighentropyvalues` and `51D_structureduseragent` - the engine consumes them as either
         * query or cookie params.
         */

        /**
         * `query.51D_gethighentropyvalues` or `cookie.51D_gethighentropyvalues` is a base64-encoded JSON-string result of
         * calling a getHighEntropyValues() API, that normally would return a value similar to the below:
         *
         * {"architecture":"arm","brands":[{"brand":"Google Chrome","version":"131"},{"brand":"Chromium","version":"131"}
         * ,{"brand":"Not_A Brand","version":"24"}],"fullVersionList":[{"brand":"Google Chrome","version":"131.0.6778.140"}
         * ,{"brand":"Chromium","version":"131.0.6778.140"},{"brand":"Not_A Brand","version":"24.0.0.0"}],
         * "mobile":false,"model":"","platform":"macOS","platformVersion":"15.1.1"}
         *
         * to obtain the below evidence we called this JavaScript snippet in the Chrome browser dev console:
         * `btoa(JSON.stringify(await navigator.userAgentData.getHighEntropyValues(
         * ['bitness', 'architecture','fullVersionList','model', 'platformVersion'])))`
         */
        evidence4.put("query.51D_gethighentropyvalues", "eyJhcmNoaXRlY3R1cmUiOiJhcm0iLCJicmFuZHMiOlt7ImJyYW5kIjoiR29vZ2xlIENocm9tZSIsInZlcnNpb24iOiIxMzEifSx7ImJyYW5kIjoiQ2hyb21pdW0iLCJ2ZXJzaW9uIjoiMTMxIn0seyJicmFuZCI6Ik5vdF9BIEJyYW5kIiwidmVyc2lvbiI6IjI0In1dLCJmdWxsVmVyc2lvbkxpc3QiOlt7ImJyYW5kIjoiR29vZ2xlIENocm9tZSIsInZlcnNpb24iOiIxMzEuMC42Nzc4LjE0MCJ9LHsiYnJhbmQiOiJDaHJvbWl1bSIsInZlcnNpb24iOiIxMzEuMC42Nzc4LjE0MCJ9LHsiYnJhbmQiOiJOb3RfQSBCcmFuZCIsInZlcnNpb24iOiIyNC4wLjAuMCJ9XSwibW9iaWxlIjpmYWxzZSwibW9kZWwiOiIiLCJwbGF0Zm9ybSI6Im1hY09TIiwicGxhdGZvcm1WZXJzaW9uIjoiMTUuMS4xIn0=" );

        /**
         * `query.51D_structureduseragent` or `cookie.51D_structureduseragent` is a JSON-string representation of
         * User-Agent Client Hints used in the [OpenRTB 2.6](https://github.com/InteractiveAdvertisingBureau/openrtb2.x/blob/main/2.6.md#objectuseragent)
         */
        evidence5.put("query.51D_structureduseragent", "{\"browsers\":[{\"brand\":\"Chromium\",\"version\":[\"124\",\"0\",\"6367\",\"91\"]},{\"brand\":\"Google Chrome\",\"version\":[\"124\",\"0\",\"6367\",\"91\"]},{\"brand\":\"Not-A.Brand\",\"version\":[\"99\",\"0\",\"0\",\"0\"]}],\"platform\":{\"brand\":\"Windows\",\"version\":[\"14\",\"0\",\"0\"]},\"mobile\":0,\"architecture\":\"x86\",\"source\":2}");

        List<Map<String, String>> evidence = new ArrayList<>();
        evidence.add(evidence1);
        evidence.add(evidence2);
        evidence.add(evidence3);
        evidence.add(evidence4);
        evidence.add(evidence5);
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
