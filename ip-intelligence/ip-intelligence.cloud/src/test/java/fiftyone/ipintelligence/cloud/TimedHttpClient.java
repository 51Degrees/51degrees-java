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

package fiftyone.ipintelligence.cloud;

import fiftyone.pipeline.engines.services.HttpClientDefault;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

/**
 * Sends requests as the default client does, and prints how long each one
 * to the live cloud service took, so each run records the measurement the
 * tests' cloud timeout rests on. Only the path is printed, never the query,
 * which can hold a Resource Key.
 */
class TimedHttpClient extends HttpClientDefault {

    @Override
    public String postData(
        HttpURLConnection connection,
        Map<String, String> headers,
        byte[] data) throws IOException {
        long start = System.currentTimeMillis();
        String outcome = "failed";
        try {
            String reply = super.postData(connection, headers, data);
            outcome = "answered";
            return reply;
        } finally {
            print(connection, outcome, start);
        }
    }

    @Override
    public String getResponseString(
        HttpURLConnection connection,
        Map<String, String> headers) throws IOException {
        long start = System.currentTimeMillis();
        String outcome = "failed";
        try {
            String reply = super.getResponseString(connection, headers);
            outcome = "answered";
            return reply;
        } finally {
            print(connection, outcome, start);
        }
    }

    private static void print(
        HttpURLConnection connection,
        String outcome,
        long start) {
        System.out.println(
            "Cloud request " + connection.getURL().getPath() + " " +
            outcome + " in " + (System.currentTimeMillis() - start) + " ms");
    }
}
