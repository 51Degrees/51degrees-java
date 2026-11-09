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

package fiftyone.pipeline.util;

import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * A license key in a log entry or an error message shows only its last 4
 * characters, with every character before them replaced.
 */
public class LicenseKeyMaskTests {

    private static final String KEY = "TESTKEYNOTREALABCDEFGHIJ1234";
    private static final String MASKED = "************************1234";
    private static final String OTHER_KEY = "OTHERTESTKEYNOTREALXYZ5678";
    private static final String OTHER_MASKED = "**********************5678";

    @Test
    public void mask_ShowsTheLast4Characters() {
        assertEquals(MASKED, LicenseKeyMask.mask(KEY));
        assertEquals(KEY.length(), LicenseKeyMask.mask(KEY).length());
    }

    /**
     * A key of 4 characters or fewer would be shown whole, so all of it is
     * replaced.
     */
    @Test
    public void mask_ShortKeyIsReplacedInFull() {
        assertEquals("****", LicenseKeyMask.mask("1234"));
        assertEquals("**", LicenseKeyMask.mask("12"));
        assertEquals("*2345", LicenseKeyMask.mask("12345"));
        assertEquals("", LicenseKeyMask.mask(""));
        assertNull(LicenseKeyMask.mask(null));
    }

    @Test
    public void maskIn_MasksEveryKeyInAUrl() {
        String url = "https://distributor.example/api/v2/download?" +
            "LicenseKeys=" + KEY + "|" + OTHER_KEY + "&Download=True";

        String masked = LicenseKeyMask.maskIn(
            url,
            Arrays.asList(KEY, OTHER_KEY));

        assertEquals(
            "https://distributor.example/api/v2/download?" +
                "LicenseKeys=" + MASKED + "|" + OTHER_MASKED +
                "&Download=True",
            masked);
    }

    /**
     * A key that a URL holds encoded is masked in its encoded form.
     */
    @Test
    public void maskIn_MasksAKeyEncodedInAUrl() {
        String key = "KEY+WITH/CHARS=ENCODED9876";
        String url = "https://example.test/?license=" +
            "KEY%2BWITH%2FCHARS%3DENCODED9876";

        String masked = LicenseKeyMask.maskIn(url, Collections.singletonList(key));

        assertFalse(masked, masked.contains("KEY%2BWITH"));
        assertTrue(masked, masked.endsWith("9876"));
    }

    @Test
    public void maskIn_LeavesTextWithoutAKeyAlone() {
        String text = "No key here.";
        assertEquals(text, LicenseKeyMask.maskIn(text, Arrays.asList(KEY)));
        assertNull(LicenseKeyMask.maskIn((String) null, Arrays.asList(KEY)));
        assertEquals(text, LicenseKeyMask.maskIn(text, null));
        assertEquals(
            text,
            LicenseKeyMask.maskIn(text, Arrays.asList(null, "")));
    }

    /**
     * The message of an exception an HTTP library raises often holds the
     * URL it requested, and so the key. The throwable to log holds the key
     * in none of its messages, its causes' or its suppressed throwables',
     * and keeps the name of each class it stands for.
     */
    @Test
    public void maskIn_MasksAThrowableAndItsCauses() {
        IOException cause = new IOException(
            "Server returned HTTP response code: 403 for URL: " +
                "https://distributor.example/?LicenseKeys=" + KEY);
        RuntimeException thrown = new RuntimeException(
            "Download failed for " + KEY, cause);
        thrown.addSuppressed(new IllegalStateException(OTHER_KEY));
        List<String> keys = Arrays.asList(KEY, OTHER_KEY);

        Throwable masked = LicenseKeyMask.maskIn(thrown, keys);
        String printed = printed(masked);

        assertFalse(printed, printed.contains(KEY));
        assertFalse(printed, printed.contains(OTHER_KEY));
        assertTrue(printed, printed.contains(MASKED));
        assertTrue(printed, printed.contains(OTHER_MASKED));
        assertTrue(printed, printed.contains("java.lang.RuntimeException"));
        assertTrue(printed, printed.contains("java.io.IOException"));
        assertTrue(printed,
            printed.contains("java.lang.IllegalStateException"));
        assertEquals(
            thrown.getStackTrace().length,
            masked.getStackTrace().length);
    }

    @Test
    public void maskIn_ThrowableWithoutAKeyIsReturnedAsItIs() {
        IOException thrown = new IOException("Connection refused");
        assertSame(thrown, LicenseKeyMask.maskIn(thrown, Arrays.asList(KEY)));
        assertNull(LicenseKeyMask.maskIn((Throwable) null, Arrays.asList(KEY)));
    }

    private static String printed(Throwable throwable) {
        StringWriter text = new StringWriter();
        throwable.printStackTrace(new PrintWriter(text));
        return text.toString();
    }
}
