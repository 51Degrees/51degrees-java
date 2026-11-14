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

package fiftyone.pipeline.engines.fiftyone.data;

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageElement;
import fiftyone.pipeline.engines.fiftyone.trackers.ShareUsageTracker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fiftyone.pipeline.core.Constants.*;

/**
 * This filter is used by the {@link ShareUsageElement} to decide which of the
 * values {@link ShareUsageEvidence#find} finds are shared. It is not the
 * element's accepted Evidence, which is empty.
 * It will include anything that is:
 * 1) An HTTP header that is not blocked by the constructor parameter, other
 *     than Cookie.
 * 2) A cookie that starts with {@link Constants#FIFTYONE_COOKIE_PREFIX} or is
 *     the session cookie (if configured in the constructor).
 * 3) A session value that starts with {@link Constants#FIFTYONE_COOKIE_PREFIX}.
 * 4) A query string parameter that starts with
 *     {@link Constants#FIFTYONE_COOKIE_PREFIX} or has been configured to be
 *     shared using the constructor parameter.
 * 5) Not a header, cookie, session value or query string parameter.
 * When all Evidence is shared, everything is included. When all query string
 * parameters are shared, every query string parameter is included.
 * The prefix of a key is everything before its first '.', and the field
 * everything after it. An email address, the salt for hashed email
 * identifiers, the query string, the path and the Cookie header are never
 * included, whatever the configuration.
 */
public class EvidenceKeyFilterShareUsage implements EvidenceKeyFilter {

    /**
     * If true then the session cookie will be included in the filter.
     * The session cookie is used by the {@link ShareUsageTracker} but we do not
     * actually want to share it.
     */
    private final boolean includeSession;

    /**
     * The cookie name being used to store the Java session id.
     */
    private final String sessionCookieName;

    /**
     * If true then every key is included, apart from those that are never
     * shared.
     */
    private final boolean shareAllEvidence;

    /**
     * If true then every query string parameter is included.
     */
    private final boolean shareAllQueryStringParameters;

    /**
     * The content of HTTP headers in this array will not be included in the
     * request information sent to 51degrees. Any header names added here are
     * hard-coded to be blocked regardless of the settings passed to the
     * constructor.
     */
    private final Set<String> blockedHttpHeaders = new HashSet<>();

    /**
     * Query string parameters will not be shared by default. Any query string
     * parameters to be shared must be added to this collection.
     */
    private final Set<String> includedQueryStringParams = new HashSet<>();

    /**
     * Evidence keys that are never shared, matched without regard to case as
     * the whole key or after a '.'. 'id.email' and 'id.salt' are a raw email
     * address and the salt used to make hashed email identifiers.
     * 'header.cookie' is the Cookie header, which carries every cookie.
     * 'request-query' is the whole query string, which would undo the rules
     * for its parameters. 'request-path' would make each page a visitor opens
     * look like a new session to the tracker.
     */
    private static final String[] NEVER_SHARED_EVIDENCE_KEYS = {
        "id.email",
        "id.salt",
        "header.cookie",
        "request-query",
        "request-path"
    };

    /**
     * Constructor.
     * @param blockedHttpHeaders a list of the names of the HTTP headers that
     *                           share usage should not send to 51Degrees
     * @param includedQueryStringParams a list of the names of query string
     *                                  parameters that share usage should send
     *                                  to 51Degrees
     * @param includeSession if true then the session cookie will be included in
     *                       the filter
     * @param sessionCookieName the name of the cookie that contains the session
     *                          id
     */
    public EvidenceKeyFilterShareUsage(
        List<String> blockedHttpHeaders,
        List<String> includedQueryStringParams,
        boolean includeSession,
        String sessionCookieName) {
        this(
            blockedHttpHeaders,
            includedQueryStringParams,
            includeSession,
            sessionCookieName,
            false,
            false);
    }

    /**
     * Constructor.
     * @param blockedHttpHeaders a list of the names of the HTTP headers that
     *                           share usage should not send to 51Degrees
     * @param includedQueryStringParams a list of the names of query string
     *                                  parameters that share usage should send
     *                                  to 51Degrees
     * @param includeSession if true then the session cookie will be included in
     *                       the filter
     * @param sessionCookieName the name of the cookie that contains the session
     *                          id
     * @param shareAllEvidence if true then all Evidence is included, apart from
     *                         the keys that are never shared
     * @param shareAllQueryStringParameters if true then all query string
     *                                      parameters are included
     */
    public EvidenceKeyFilterShareUsage(
        List<String> blockedHttpHeaders,
        List<String> includedQueryStringParams,
        boolean includeSession,
        String sessionCookieName,
        boolean shareAllEvidence,
        boolean shareAllQueryStringParameters) {
        this.shareAllEvidence = shareAllEvidence;
        this.shareAllQueryStringParameters = shareAllQueryStringParameters;
        // The Cookie header carries every cookie, including the ones the
        // cookie rule leaves out.
        this.blockedHttpHeaders.add("cookie");
        this.includeSession = includeSession;
        this.sessionCookieName = sessionCookieName == null ?
            null : sessionCookieName.toLowerCase();
        for (String header : blockedHttpHeaders) {
            String lowerHeader = header.toLowerCase();
            if (!this.blockedHttpHeaders.contains(lowerHeader)) {
                this.blockedHttpHeaders.add(lowerHeader);
            }
        }
        for (String parameter : includedQueryStringParams) {
            String lowerParameter = parameter.toLowerCase();
            if (!this.includedQueryStringParams.contains(lowerParameter)) {
                this.includedQueryStringParams.add(lowerParameter);
            }
        }
    }

    @Override
    public boolean include(String key) {
        // The Usage Sharing Element reads the whole web request itself, and
        // the request is never a value to share.
        if (ShareUsageEvidence.HTTP_REQUEST_KEY.equalsIgnoreCase(key)) {
            return false;
        }
        String lowerKey = key.toLowerCase();
        for (String neverShared : NEVER_SHARED_EVIDENCE_KEYS) {
            if (lowerKey.equals(neverShared) ||
                lowerKey.endsWith(EVIDENCE_SEPERATOR + neverShared)) {
                return false;
            }
        }

        if (shareAllEvidence) {
            return true;
        }

        boolean result;
        int separator = lowerKey.indexOf(EVIDENCE_SEPERATOR);
        if (separator > 0) {
            String prefix = lowerKey.substring(0, separator);
            String field = lowerKey.substring(separator + 1);
            switch (prefix) {
                case EVIDENCE_HTTPHEADER_PREFIX:
                    // Add the header to the list if the header name does not
                    // appear in the list of blocked headers.
                    result = blockedHttpHeaders.contains(field) == false;
                    break;
                case EVIDENCE_COOKIE_PREFIX:
                    // Only add cookies that start with the 51Degrees cookie
                    // prefix.
                    result = field.startsWith(Constants.FIFTYONE_COOKIE_PREFIX) ||
                        (includeSession && field.equals(sessionCookieName));
                    break;
                case EVIDENCE_SESSION_PREFIX:
                    // Only add session values that start with the 51Degrees cookie
                    // prefix.
                    result = field.startsWith(Constants.FIFTYONE_COOKIE_PREFIX);
                    break;
                case EVIDENCE_QUERY_PREFIX:
                    // Only include query string parameters that start with the
                    // 51Degrees prefix or have been specified in the
                    // constructor, unless all of them are shared.
                    result = shareAllQueryStringParameters ||
                        field.startsWith(Constants.FIFTYONE_COOKIE_PREFIX) ||
                        includedQueryStringParams.contains(field);
                    break;
                default:
                    // Add anything that is not a cookie or a header.
                    result = true;
                    break;
            }
        } else {
            result = true;
        }

        return result;
    }

    @Override
    public Integer order(String key) {
        return 100;
    }
}
