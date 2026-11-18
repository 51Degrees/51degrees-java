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
import java.util.Locale;
import java.util.Set;

import static fiftyone.pipeline.core.Constants.*;
import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.SHARE_USAGE_DEFAULT_SHARE_ALL_QUERY_STRING_PARAMETERS;

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
 * 4) A query string or HTTP form parameter that starts with
 *     {@link Constants#FIFTYONE_COOKIE_PREFIX}, or that is Evidence the
 *     Pipeline uses, being a key a Flow Element in the Pipeline accepts,
 *     whatever the configuration. The included query string parameters and
 *     share all query string parameters options are deprecated and have no
 *     effect.
 * 5) Not a header, cookie, session value or query string parameter.
 * When all Evidence is shared, everything is included but cookies and query
 * string and form parameters, which follow rules 2 and 4 whatever the
 * configuration.
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
     * The content of HTTP headers in this array will not be included in the
     * request information sent to 51degrees. Any header names added here are
     * hard-coded to be blocked regardless of the settings passed to the
     * constructor.
     */
    private final Set<String> blockedHttpHeaders = new HashSet<>();


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
     * Constructor. All Evidence is not shared, as is the default.
     * @param blockedHttpHeaders a list of the names of the HTTP headers that
     *                           share usage should not send to 51Degrees
     * @param includedQueryStringParams deprecated, and has no effect
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
            SHARE_USAGE_DEFAULT_SHARE_ALL_QUERY_STRING_PARAMETERS);
    }

    /**
     * Constructor.
     * @param blockedHttpHeaders a list of the names of the HTTP headers that
     *                           share usage should not send to 51Degrees
     * @param includedQueryStringParams deprecated, and has no effect
     * @param includeSession if true then the session cookie will be included in
     *                       the filter
     * @param sessionCookieName the name of the cookie that contains the session
     *                          id
     * @param shareAllEvidence if true then all Evidence is included, apart from
     *                         the keys that are never shared and the cookies
     *                         other than the 51Degrees ones
     * @param shareAllQueryStringParameters deprecated, and has no effect
     */
    public EvidenceKeyFilterShareUsage(
        List<String> blockedHttpHeaders,
        List<String> includedQueryStringParams,
        boolean includeSession,
        String sessionCookieName,
        boolean shareAllEvidence,
        boolean shareAllQueryStringParameters) {
        this.shareAllEvidence = shareAllEvidence;
        // The Cookie header carries every cookie, including the ones the
        // cookie rule leaves out.
        this.blockedHttpHeaders.add("cookie");
        this.includeSession = includeSession;
        this.sessionCookieName = sessionCookieName == null ?
            null : sessionCookieName.toLowerCase(Locale.ROOT);
        for (String header : blockedHttpHeaders) {
            String lowerHeader = header.toLowerCase(Locale.ROOT);
            if (!this.blockedHttpHeaders.contains(lowerHeader)) {
                this.blockedHttpHeaders.add(lowerHeader);
            }
        }
    }

    @Override
    public boolean include(String key) {
        return include(key, null);
    }

    /**
     * Whether the value with this key is shared.
     * @param key the Evidence key
     * @param pipelineUses the Evidence key filter of the Pipeline the value
     *                     came through, which decides whether a query string
     *                     or form parameter is Evidence the Pipeline uses, or
     *                     null if there is none
     * @return true if the value is shared
     */
    public boolean include(String key, EvidenceKeyFilter pipelineUses) {
        // The Usage Sharing Element reads the whole web request itself, and
        // the request is never a value to share.
        if (ShareUsageEvidence.HTTP_REQUEST_KEY.equalsIgnoreCase(key)) {
            return false;
        }
        String lowerKey = key.toLowerCase(Locale.ROOT);
        for (String neverShared : NEVER_SHARED_EVIDENCE_KEYS) {
            if (lowerKey.equals(neverShared) ||
                lowerKey.endsWith(EVIDENCE_SEPERATOR + neverShared)) {
                return false;
            }
        }

        int separator = lowerKey.indexOf(EVIDENCE_SEPERATOR);

        // Only the 51Degrees cookies are shared, even when all Evidence is.
        // The session cookie is included only for the tracker to compare.
        if (separator > 0 &&
            EVIDENCE_COOKIE_PREFIX.equals(lowerKey.substring(0, separator))) {
            String field = lowerKey.substring(separator + 1);
            return field.startsWith(Constants.FIFTYONE_COOKIE_PREFIX) ||
                (includeSession && field.equals(sessionCookieName));
        }

        // A query string or form parameter is shared only when its name
        // starts with 51D_ or the Pipeline uses it, even when all Evidence
        // is, so that a parameter such as a password is never shared.
        if (separator > 0 &&
            EVIDENCE_QUERY_PREFIX.equals(lowerKey.substring(0, separator))) {
            String field = lowerKey.substring(separator + 1);
            return field.startsWith(Constants.FIFTYONE_COOKIE_PREFIX) ||
                (pipelineUses != null && pipelineUses.include(key));
        }

        if (shareAllEvidence) {
            return true;
        }

        boolean result;
        if (separator > 0) {
            String prefix = lowerKey.substring(0, separator);
            String field = lowerKey.substring(separator + 1);
            switch (prefix) {
                case EVIDENCE_HTTPHEADER_PREFIX:
                    // Add the header to the list if the header name does not
                    // appear in the list of blocked headers.
                    result = blockedHttpHeaders.contains(field) == false;
                    break;
                case EVIDENCE_SESSION_PREFIX:
                    // Only add session values that start with the 51Degrees cookie
                    // prefix.
                    result = field.startsWith(Constants.FIFTYONE_COOKIE_PREFIX);
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
