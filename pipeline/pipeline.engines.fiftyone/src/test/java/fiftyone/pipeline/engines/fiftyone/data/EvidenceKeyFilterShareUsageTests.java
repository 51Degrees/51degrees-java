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

import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests which evidence {@link EvidenceKeyFilterShareUsage} lets usage sharing
 * keep and send.
 */
public class EvidenceKeyFilterShareUsageTests {

    private static EvidenceKeyFilterShareUsage filter(
        List<String> blockedHttpHeaders,
        List<String> includedQueryStringParams,
        boolean includeSession) {
        return new EvidenceKeyFilterShareUsage(
            blockedHttpHeaders,
            includedQueryStringParams,
            includeSession,
            DEFAULT_SESSION_COOKIE_NAME);
    }

    private static EvidenceKeyFilter uses(String... keys) {
        return new EvidenceKeyFilterWhitelist(Arrays.asList(keys));
    }

    private static EvidenceKeyFilterShareUsage defaults() {
        return filter(
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            false);
    }

    private static EvidenceKeyFilterShareUsage filter(
        boolean includeSession,
        boolean shareAllEvidence,
        boolean shareAllQueryStringParameters) {
        return new EvidenceKeyFilterShareUsage(
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            includeSession,
            DEFAULT_SESSION_COOKIE_NAME,
            shareAllEvidence,
            shareAllQueryStringParameters);
    }

    /**
     * Shares only the 51D_ query string parameters and those named, as when
     * share all query string parameters is turned off.
     */
    private static EvidenceKeyFilterShareUsage namedParametersOnly(
        List<String> blockedHttpHeaders,
        List<String> includedQueryStringParams) {
        return new EvidenceKeyFilterShareUsage(
            blockedHttpHeaders,
            includedQueryStringParams,
            false,
            DEFAULT_SESSION_COOKIE_NAME,
            false,
            false);
    }

    /**
     * The whole web request is read by the Usage Sharing Element and never
     * shared as a value, so the filter does not include it.
     */
    @Test
    public void webRequestIsNeverIncluded() {
        EvidenceKeyFilterShareUsage filter = defaults();

        assertFalse(filter.include("server.http-request"));
        assertFalse(filter.include("Server.Http-Request"));
    }

    /**
     * By default every header but Cookie and any other evidence are shared,
     * cookies and session values only when their names start with 51D_, and
     * query string parameters only when their names start with 51D_ or the
     * Pipeline uses them.
     */
    @Test
    public void defaultRules() {
        EvidenceKeyFilterShareUsage filter = defaults();

        assertTrue(filter.include("header.user-agent"));
        assertFalse(filter.include("header.cookie"));
        assertTrue(filter.include("cookie.51d_screenpixelswidth"));
        assertTrue(filter.include("cookie.51D_ScreenPixelsWidth"));
        assertFalse(filter.include("cookie.sessionid"));
        assertFalse(filter.include("cookie.a.b"));
        assertTrue(filter.include("session.51d_value"));
        assertFalse(filter.include("session.org.example.attribute"));
        assertTrue(filter.include("query.51d_gethighentropyvalues"));
        assertTrue(filter.include("query.51D_Test"));
        assertFalse(filter.include("query.password"));
        assertFalse(filter.include("Query.Utm_Source"));
        assertTrue(filter.include(
            "query.utm_source",
            uses("query.utm_source")));
        assertFalse(filter.include("query.id.email"));
        assertFalse(filter.include("server.request-query"));
        assertTrue(filter.include("server.client-ip"));
        assertTrue(filter.include("nodot"));
    }

    /**
     * The deprecated included parameters list has no effect, so only the
     * 51D_ parameters and those the Pipeline uses are shared.
     */
    @Test
    public void shareAllQueryStringParametersOff_OnlyNamedAnd51D() {
        EvidenceKeyFilterShareUsage filter = namedParametersOnly(
            Collections.<String>emptyList(),
            Collections.singletonList("campaign"));

        assertFalse(filter.include("query.password"));
        assertFalse(filter.include("query.campaign"));
        assertTrue(filter.include("query.campaign", uses("query.campaign")));
        assertTrue(filter.include("query.51D_Test"));
        assertTrue(filter.include("header.user-agent"));
    }

    /**
     * Even when all Evidence is shared, a cookie is shared only when its
     * name starts with 51D_, whatever its case. The session cookie is kept
     * only by a filter that includes it for the tracker to compare.
     */
    @Test
    public void shareAllEvidence_NoCookieBut51D() {
        EvidenceKeyFilterShareUsage sent = filter(false, true, true);
        EvidenceKeyFilterShareUsage kept = filter(true, true, true);

        for (EvidenceKeyFilterShareUsage filter : Arrays.asList(sent, kept)) {
            assertTrue(filter.include("cookie.51D_Profile"));
            assertTrue(filter.include("cookie.51d_screenpixelswidth"));
            assertFalse(filter.include("cookie._ga"));
            assertFalse(filter.include("Cookie.Other"));
            assertFalse(filter.include("cookie.a.b"));
            assertTrue(filter.include("session.org.example.attribute"));
            assertTrue(filter.include("header.x-anything"));
            // Not even all Evidence shares a parameter the Pipeline does
            // not use.
            assertFalse(filter.include("query.password"));
            assertTrue(filter.include(
                "query.user-agent",
                uses("query.user-agent")));
        }
        assertFalse(sent.include("cookie." + DEFAULT_SESSION_COOKIE_NAME));
        assertTrue(kept.include("cookie." + DEFAULT_SESSION_COOKIE_NAME));
    }

    /**
     * With all Evidence shared, the tracker compares the session cookie only
     * when track session is on, and no other cookie but the 51D_ ones.
     */
    @Test
    public void trackerWithShareAllEvidence_SessionCookieOnlyWhenTracked() {
        EvidenceKeyFilterShareUsageTracker tracked =
            new EvidenceKeyFilterShareUsageTracker(
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                true,
                DEFAULT_SESSION_COOKIE_NAME,
                true,
                true);
        EvidenceKeyFilterShareUsageTracker untracked =
            new EvidenceKeyFilterShareUsageTracker(
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                false,
                DEFAULT_SESSION_COOKIE_NAME,
                true,
                true);

        assertTrue(tracked.include("cookie." + DEFAULT_SESSION_COOKIE_NAME));
        assertFalse(untracked.include("cookie." + DEFAULT_SESSION_COOKIE_NAME));
        assertFalse(tracked.include("cookie._ga"));
        assertFalse(untracked.include("cookie._ga"));
        assertTrue(untracked.include("cookie.51D_Profile"));
    }

    /**
     * A name containing a dot is matched as a whole against the keys the
     * Pipeline uses.
     */
    @Test
    public void fieldIsEverythingAfterTheFirstDot() {
        EvidenceKeyFilterShareUsage filter = namedParametersOnly(
            Collections.<String>emptyList(),
            Collections.singletonList("valid.email"));
        EvidenceKeyFilter uses = uses("query.valid.email");

        assertTrue(filter.include("query.valid.email", uses));
        assertFalse(filter.include("query.valid", uses));
        assertFalse(filter.include("query.other.name", uses));
    }

    /**
     * The headers not to share are matched without regard to case. The
     * deprecated parameters to share have no effect.
     */
    @Test
    public void listsIgnoreCase() {
        EvidenceKeyFilterShareUsage filter = namedParametersOnly(
            Collections.singletonList("X-Secret"),
            Collections.singletonList("Campaign"));

        assertFalse(filter.include("header.x-secret"));
        assertFalse(filter.include("query.campaign"));
        assertTrue(filter.include("header.user-agent"));
    }

    /**
     * The session cookie is included only when asked for, whatever the case of
     * its name, and no other ordinary cookie is.
     */
    @Test
    public void sessionCookie() {
        EvidenceKeyFilterShareUsage kept = filter(
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            true);
        EvidenceKeyFilterShareUsage sent = defaults();

        assertTrue(kept.include("cookie." + DEFAULT_SESSION_COOKIE_NAME));
        assertTrue(kept.include("cookie.jsessionid"));
        assertFalse(sent.include("cookie." + DEFAULT_SESSION_COOKIE_NAME));
        assertFalse(kept.include("cookie._ga"));
        assertFalse(sent.include("cookie._ga"));
    }

    /**
     * The email address, the salt for hashed email identifiers, the query
     * string and the path are never shared, whether bare or after a prefix,
     * and without regard to case, even when the rules for their prefixes
     * would share them.
     */
    @Test
    public void neverShared() {
        List<EvidenceKeyFilterShareUsage> filters = Arrays.asList(
            defaults(),
            filter(
                Collections.<String>emptyList(),
                Arrays.asList("id.email", "id.salt"),
                true));
        List<String> keys = Arrays.asList(
            "id.email", "id.salt",
            "query.id.email", "QUERY.ID.EMAIL",
            "header.id.email", "header.id.salt", "HEADER.ID.SALT",
            "server.request-query", "SERVER.REQUEST-QUERY",
            "server.request-path", "SERVER.REQUEST-PATH");
        for (EvidenceKeyFilterShareUsage filter : filters) {
            for (String key : keys) {
                assertFalse(filter.include(key), key);
            }
        }
    }

    /**
     * The Cookie header carries every cookie, so it is never shared, whether
     * bare or after a prefix, and without regard to case.
     */
    @Test
    public void cookieHeaderNeverShared() {
        EvidenceKeyFilterShareUsage filter = defaults();
        for (String key : Arrays.asList(
            "header.cookie", "HEADER.COOKIE", "x.header.cookie",
            "server.header.cookie")) {
            assertFalse(filter.include(key), key);
        }
        assertTrue(filter.include("header.cookies-allowed"));
        assertTrue(filter.include("server.request-method"));
    }

    /**
     * By default every header but Cookie is shared, so a header named
     * Cookies is shared like any other header.
     */
    @Test
    public void onlyTheCookieHeaderIsBlockedByDefault() {
        EvidenceKeyFilterShareUsage filter = defaults();
        assertFalse(filter.include("header.cookie"));
        assertFalse(filter.include("header.Cookie"));
        assertTrue(filter.include("header.cookies"));
        assertTrue(filter.include("header.Cookies"));
    }

    /**
     * A header named Cookies is blocked when the blocked headers name it.
     */
    @Test
    public void aCookiesHeaderIsBlockedWhenNamed() {
        EvidenceKeyFilterShareUsage filter = filter(
            Collections.singletonList("Cookies"),
            Collections.<String>emptyList(),
            false);
        assertFalse(filter.include("header.cookies"));
        assertFalse(filter.include("header.Cookies"));
        assertFalse(filter.include("header.cookie"));
    }

    /**
     * A key that only ends with the same letters as a key never shared is
     * treated as an ordinary key.
     */
    @Test
    public void neverSharedKeysMatchWholeParts() {
        EvidenceKeyFilterShareUsage filter = filter(
            Collections.<String>emptyList(),
            Collections.singletonList("valid.email"),
            false);

        assertTrue(filter.include(
            "query.valid.email",
            uses("query.valid.email")));
    }

    /**
     * Sharing one parameter does not share the whole query string, which
     * carries every parameter.
     */
    @Test
    public void namingAParameterDoesNotShareTheQueryString() {
        EvidenceKeyFilterShareUsage filter = filter(
            Collections.<String>emptyList(),
            Collections.singletonList("utm_source"),
            false);

        assertTrue(filter.include(
            "query.utm_source",
            uses("query.utm_source")));
        assertFalse(filter.include(
            "server.request-query",
            uses("server.request-query")));
    }

    /**
     * The tracker leaves out the session id and sequence number, which differ
     * on every request, and follows the same rules otherwise.
     */
    @Test
    public void trackerLeavesOutSessionIdAndSequence() {
        EvidenceKeyFilterShareUsageTracker tracker =
            new EvidenceKeyFilterShareUsageTracker(
                Collections.<String>emptyList(),
                Arrays.asList("session-id", "sequence"),
                false,
                DEFAULT_SESSION_COOKIE_NAME);

        assertFalse(tracker.include("query.session-id"));
        assertFalse(tracker.include("query.sequence"));
        assertTrue(tracker.include("header.user-agent"));
        assertFalse(tracker.include("query.id.email"));
    }
}
