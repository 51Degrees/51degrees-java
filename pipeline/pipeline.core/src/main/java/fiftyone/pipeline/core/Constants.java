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

package fiftyone.pipeline.core;

/** Evidence prefixes, suffixes and keys shared by every element. */
public class Constants {
    /** Create a new instance. */
    public Constants() {
    }

    /** Separates the parts of an evidence key. */
    public static final String EVIDENCE_SEPERATOR = ".";

    /** Prefix of evidence from HTTP headers. */
    public static final String EVIDENCE_HTTPHEADER_PREFIX = "header";
    /** Prefix of evidence from cookies. */
    public static final String EVIDENCE_COOKIE_PREFIX = "cookie";
    /** Prefix of evidence from the session. */
    public static final String EVIDENCE_SESSION_PREFIX = "session";
    /** Prefix of evidence from the query string. */
    public static final String EVIDENCE_QUERY_PREFIX = "query";
    /** Prefix of evidence from the server. */
    public static final String EVIDENCE_SERVER_PREFIX = "server";

    /** Suffix of the User-Agent evidence. */
    public static final String EVIDENCE_USERAGENT = "user-agent";
    /** Suffix of the web context root evidence. */
    public static final String EVIDENCE_WEB_CONTEXT_ROOT_SUFFIX = "contextroot";

    /** Evidence key of the client IP address. */
    public static final String EVIDENCE_CLIENTIP_KEY = EVIDENCE_SERVER_PREFIX + EVIDENCE_SEPERATOR + "client-ip";

    /** Evidence key of a User-Agent in the query string. */
    public static final String EVIDENCE_QUERY_USERAGENT_KEY = EVIDENCE_QUERY_PREFIX + EVIDENCE_SEPERATOR + EVIDENCE_USERAGENT;

    /** Evidence key of the User-Agent header. */
    public static final String EVIDENCE_HEADER_USERAGENT_KEY = EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + EVIDENCE_USERAGENT;
    
    /** Evidence key of the web context root. */
    public static final String EVIDENCE_WEB_CONTEXT_ROOT = EVIDENCE_SERVER_PREFIX + EVIDENCE_SEPERATOR + EVIDENCE_WEB_CONTEXT_ROOT_SUFFIX;
    
    /** Evidence key of the request protocol. */
    public static final String EVIDENCE_PROTOCOL = EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "protocol";

    /** Evidence key of the session. */
    public static final String EVIDENCE_SESSION_KEY = EVIDENCE_SESSION_PREFIX + EVIDENCE_SEPERATOR + "session";

    /**
     * Whether a Pipeline closes its elements when it is closed, unless its
     * builder is told otherwise.
     */
    public static final boolean DEFAULT_AUTO_CLOSE_ELEMENTS = false;

    /**
     * Whether a Pipeline records the exceptions thrown while processing as
     * errors rather than throwing them, unless its builder is told otherwise.
     */
    public static final boolean DEFAULT_SUPPRESS_PROCESS_EXCEPTIONS = false;
}
