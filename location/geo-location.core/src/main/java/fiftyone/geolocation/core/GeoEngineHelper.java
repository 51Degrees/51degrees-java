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

package fiftyone.geolocation.core;

import fiftyone.geolocation.core.data.GeoEvidence;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.engines.services.HttpClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static fiftyone.pipeline.core.Constants.EVIDENCE_CLIENTIP_KEY;

/**
 * Helper class that contains shared functionality required
 * by geo-location engines.
 * This functionality is here rather than a geo engine base
 * class because different geo-location engines require
 * different engine base class functionality.
 */
public class GeoEngineHelper {
    /**
     * The result of a call to performGeoLookup.
      */
    public static class GeoLookupResult {
        /**
         * The raw string response from the geo-location web service.
         * If the FlowData does not contain the required evidence then the
         * clientsideEvidenceJS property will be populated instead.
         */
        public String geoServiceResponse;

        /**
         * A JavaScript instance containing the JavaScript
         * to run on the client device in order to supply the evidence
         * required by the geo-location service.
         * This is only populated if the required evidence is not
         * available in the FlowData.
         */
        public JavaScript clientsideEvidenceJS;
    }

    /**
     * Try getting the specified coordinate keys from evidence
     * and adding them to geo-evidence.
     * @param evidence evidence collection to get the coordinates from
     * @param geoEvidence geo-evidence to add the coordinates to
     * @param latitudeKey The name of the key for the latitude value in the evidence
     * @param longitudeKey The name of the key for the longitude value in the evidence
     * @return True if both values were found in the evidence. False if not.
     */
    private static boolean tryAddCoordinates(
        Map<String, Object> evidence,
        GeoEvidence geoEvidence,
        String latitudeKey,
        String longitudeKey) {
        boolean found = false;
        if (evidence.containsKey(latitudeKey) &&
            evidence.containsKey(longitudeKey))
        {
            geoEvidence.setLatitude(evidence.get(latitudeKey).toString());
            geoEvidence.setLongitude(evidence.get(longitudeKey).toString());
            found = true;
        }
        return found;
    }

    /**
     * Use evidence from the specified FlowData instance to construct a query
     * @param data the flow data to process
     * @param webClient the HttpClient to use when making a request
     *                  to the geo-location service.
     * @param requestFormatter a formatter that will create a URL that can be
     *                         used to query a web service based on the supplied
     *                         GeoEvidence
     * @return A new GeoLookupResult instance. If the supplied FlowData contains
     * the required evidence then this will contain the raw response from the
     * web service.
     * If not, it will contain the JavaScript to run on the client
     * device in order to supply the required evidence.
     */
    public static GeoLookupResult performGeoLookup(
        FlowData data,
        HttpClient webClient,
        RequestFormatter requestFormatter) throws IOException {
        GeoLookupResult result = new GeoLookupResult();
        // Get the evidence as a dictionary.
        Map<String, Object> allEvidence = data.getEvidence().asKeyMap();
        GeoEvidence geoEvidence = new GeoEvidence();

        // Try to get any coordinates from the location key, the cookie,
        // then overrides. If any of these find latitude or longitude
        // values, then getServerSide will be set to true.
        boolean getServerSide = tryAddCoordinates(
            allEvidence,
            geoEvidence,
            Constants.EVIDENCE_GEO_LAT_KEY,
            Constants.EVIDENCE_GEO_LON_KEY);
        getServerSide = getServerSide ||
            tryAddCoordinates(
                allEvidence,
                geoEvidence,
                Constants.EVIDENCE_GEO_LAT_COOKIE_KEY,
                Constants.EVIDENCE_GEO_LON_COOKIE_KEY);
        getServerSide = getServerSide ||
            tryAddCoordinates(
                allEvidence,
                geoEvidence,
                Constants.EVIDENCE_GEO_LAT_PARAM_KEY,
                Constants.EVIDENCE_GEO_LON_PARAM_KEY);

        if (getServerSide == true) {
            // There are coordinates in the evidence.
            // Add the IP address.
            if (allEvidence.containsKey(EVIDENCE_CLIENTIP_KEY)) {
                geoEvidence.setIpAddress((String)allEvidence.get(EVIDENCE_CLIENTIP_KEY));
            }
            // Format the URL correctly for whatever service
            // we're querying
            URL url = requestFormatter.format(geoEvidence);
            // Send the request and store the response in
            // the result.
            HttpURLConnection connection = webClient.connect(url);
            result.geoServiceResponse = webClient.getResponseString(connection);
            connection.disconnect();
        }
        else {
            // No location data found in evidence so set the
            // JavaScript value in the geo data.
            // If this JavaScript is executed on the client device
            // then we'll have location data on the next request
            // the device makes.
            result.clientsideEvidenceJS = new JavaScript(
                JavaScriptResource.getJavaScript());
        }

        return result;
    }

}
