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

package fiftyone.pipeline.javascriptbuilder;

/** Evidence keys and defaults used by the JavaScript Builder Element. */
public class Constants {
    /** Create a new instance. */
    public Constants() {
    }

    /** Evidence key of the Host header. */
    public static final String EVIDENCE_HOST_KEY = 
            fiftyone.pipeline.core.Constants.EVIDENCE_HTTPHEADER_PREFIX +
            fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR +
            "Host";
    
    /** Suffix of the evidence that names the JavaScript object. */
    public static final String EVIDENCE_OBJECT_NAME_SUFFIX =
            "fod-js-object-name";
    
    /**
     * Evidence key, in the query string, of the name of the JavaScript object.
     */
    public static final String EVIDENCE_OBJECT_NAME = 
             fiftyone.pipeline.core.Constants.EVIDENCE_QUERY_PREFIX +
             fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR +
            EVIDENCE_OBJECT_NAME_SUFFIX;

    /**
     * The suffix used when the JavaScriptBuilderElement 'enable cookies'
     * parameter is supplied as evidence.
     */
    public static final String EVIDENCE_ENABLE_COOKIES_SUFFIX =
        "fod-js-enable-cookies";

    /**
     * The complete key to be used when the JavaScriptBuilderElement
     * 'enable cookies' parameter is supplied as part of the query string.
     */
    public static final String EVIDENCE_ENABLE_COOKIES =
        fiftyone.pipeline.core.Constants.EVIDENCE_QUERY_PREFIX +
            fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR +
            EVIDENCE_ENABLE_COOKIES_SUFFIX;

    /** Protocol of the callback URL when none is configured or found. */
    public static final String DEFAULT_PROTOCOL = "https";
    
    /** Path of the template the JavaScript is made from. */
    public static final String TEMPLATE = "/fiftyone/pipeline/javascriptbuilder/templates/JavaScriptResource.mustache";
    /** Name of the JavaScript object when none is configured. */
    public static final String DEFAULT_OBJECT_NAME = "fod";
}
