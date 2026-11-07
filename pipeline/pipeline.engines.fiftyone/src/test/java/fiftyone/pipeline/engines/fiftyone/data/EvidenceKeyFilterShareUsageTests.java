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

    private static EvidenceKeyFilterShareUsage defaults() {
        return filter(
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            false);
    }

    /**
     * By default headers other than Cookie are shared, cookies, session
     * values and query string parameters only when their names start with
     * 51D_, and any other evidence.
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
        assertFalse(filter.include("query.id.email"));
        assertTrue(filter.include("server.client-ip"));
        assertTrue(filter.include("nodot"));
    }

    /**
     * A name containing a dot is matched as a whole against the parameters to
     * share.
     */
    @Test
    public void fieldIsEverythingAfterTheFirstDot() {
        EvidenceKeyFilterShareUsage filter = filter(
            Collections.<String>emptyList(),
            Collections.singletonList("valid.email"),
            false);

        assertTrue(filter.include("query.valid.email"));
        assertFalse(filter.include("query.valid"));
        assertFalse(filter.include("query.other.name"));
    }

    /**
     * The headers not to share and the parameters to share are matched
     * without regard to case.
     */
    @Test
    public void listsIgnoreCase() {
        EvidenceKeyFilterShareUsage filter = filter(
            Collections.singletonList("X-Secret"),
            Collections.singletonList("Campaign"),
            false);

        assertFalse(filter.include("header.x-secret"));
        assertTrue(filter.include("query.campaign"));
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
     * A key that only ends with the same letters as a key never shared is
     * treated as an ordinary key.
     */
    @Test
    public void neverSharedKeysMatchWholeParts() {
        EvidenceKeyFilterShareUsage filter = filter(
            Collections.<String>emptyList(),
            Collections.singletonList("valid.email"),
            false);

        assertTrue(filter.include("query.valid.email"));
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

        assertTrue(filter.include("query.utm_source"));
        assertFalse(filter.include("server.request-query"));
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
