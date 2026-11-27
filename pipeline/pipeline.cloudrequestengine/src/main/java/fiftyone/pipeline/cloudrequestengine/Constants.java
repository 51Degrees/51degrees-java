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

package fiftyone.pipeline.cloudrequestengine;

/** Constants used by the Cloud Request Engine. */
public class Constants {
    /** Create a new instance. */
    public Constants() {
    }
    
    /** Name of the Origin header sent with each request. */
    public static final String OriginHeaderName = "Origin";

    /** The messages the Cloud Request Engine logs and throws. */
    public class Messages {
        /** Create a new instance. */
        public Messages() {
        }

        /** Error when neither a resource key nor a license key is set. */
        public static final String NoCredential =
            "The Cloud Request Engine needs a resource key or a license key "
            + "to authenticate with, as the cloud service answers 401 to a "
            + "request that carries neither. Use setResourceKey(String) with "
            + "a resource key created for free at "
            + "https://51degrees.com/configure?utm_source=code&utm_medium=comment&utm_campaign=51degrees-java&utm_content=pipeline.cloudrequestengine-src-main-java-fiftyone-pipeline-cloudrequestengine-constants.java&utm_term=no-credential";

        /** Error when a license key is set with no requested properties. */
        public static final String LicenseKeyNeedsProperties =
            "The Cloud Request Engine has a license key and no requested "
            + "properties. A license key names no properties of its own, so "
            + "the cloud service answers 400 to every request made with one "
            + "that does not list the properties it wants. Use "
            + "setRequestedProperties to name the fully qualified "
            + "properties wanted, for example \"device.ismobile\" and "
            + "\"device.iscrawler\".";

        /** Warning naming requested properties the keys in use do not give. */
        public static final String PropertiesNotCovered =
            "The cloud service did not return the requested properties %s "
            + "in its answer. They are not covered by the keys in use, so "
            + "they will be missing from every result. The other requested "
            + "properties came back and are usable.";

        /** Warning naming requested names that are not properties. */
        public static final String RequestedNamesNotProperties =
            "The cloud service did not return the requested properties %s "
            + "in its answer. Every property is answered, with a reason "
            + "when it has no value, so these names are not properties. "
            + "Check the spelling of each one. The other requested "
            + "properties came back and are usable.";

        /**
         * Error when two pieces of evidence give different values for one key.
         */
        public static final String EvidenceConflict =
            "'%s:%s' evidence conflicts with %s";

        /**
         * Warning a cloud engine logs when the Cloud Request Engine has no
         * response for it to read. The argument is the engine's class name.
         */
        public static final String NoCloudResponse =
            "The %s did not process, as the Cloud Request Engine has no "
            + "response. The Cloud Request Engine's error in the log says "
            + "why.";

        /** Error when the answer from the cloud service holds no data. */
        public static final String MessageNoDataInResponse =
            "No data in response from cloud service at '%s'";
        /**
         * Error when the cloud service answers with a status code that is not
         * 200.
         */
        public static final String MessageErrorCodeReturned =
            "Cloud service at '%s' returned status code '%d' with content: %s";

        /** Error when the cloud service returns more than one error. */
        public static final String ExceptionCloudErrorsMultiple =
            "Multiple errors returned from 51Degrees cloud service. See inner " +
            "exceptions for details.";

        /** Error when the cloud service returns an error. */
        public static final String ExceptionCloudError =
            "Error returned from 51Degrees cloud service: %s";

        /** Error when the answer from the cloud service is not JSON. */
        public static final String ExceptionResponseNotJson =
            "Cloud service returned status code '%d' with content that is " +
            "not JSON";
        
        /**
         * Error when the resource key gives no properties of the product an
         * element needs.
         */
        public static final String ExceptionFailedToLoadProperties = 
                "Failed to load aspect properties for element '%s'. This is "
                + "because your resource key does not include access to any "
                + "properties under '%s'. For more details on resource keys, "
                + "see our explainer: "
                + "https://51degrees.com/documentation/_services__cloud__resource_keys.html?utm_source=code&utm_medium=comment&utm_campaign=51degrees-java&utm_content=pipeline.cloudrequestengine-src-main-java-fiftyone-pipeline-cloudrequestengine-constants.java&utm_term=exceptionfailedtoloadproperties";
        
        /**
         * Warning that the properties and evidence keys will be fetched when
         * the engine is first used.
         */
        public static final String MessageDiscoveryDeferred =
                "The cloud request engine could not fetch its accessible "
                + "properties or accepted evidence keys when it was built, "
                + "because of a failure that can pass. It will fetch them "
                + "again when it is first used, and sends no data request "
                + "until it has both.";

        /**
         * Warning that the resource key does not include a product, so its
         * engine returns no values.
         */
        public static final String MessageProductNotInResourceKey =
                "The resource key does not include the '%s' product, so "
                + "%s has no properties and its results hold no values. "
                + "Use a resource key that includes the product to get them.";

        /**
         * Error from a cloud engine that does not override the method which
         * reads the response.
         */
        public static final String ProcessCloudEngineNotImplemented = 
                  "This method should be overridden in the class derived from "
                + "CloudAspectEngineBase class. The implementation should use "
                + "the 'json' parameter to populate the 'aspectData' accordingly. " 
                + "This method will be called by the CloudAspectEngine.ProcessEngine() " 
                + "method after it has successfully retrieved the JsonResponse from "
                +  "the CloudRequestEngine.";

    }
}
