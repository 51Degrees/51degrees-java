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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * The transactions the engine has given pages and not yet used, by page
 * id. Each is used once, and is dropped after
 * {@link #PAGE_LIFETIME_MINUTES}, or sooner when {@link #MAX_PAGES} are
 * held, oldest first, so the memory used stays bounded.
 */
final class HumanConfidencePages {

    /** How long a page's transaction is kept for its sealed result. */
    static final long PAGE_LIFETIME_MINUTES = 30;

    /** The most transactions kept at once. */
    static final int MAX_PAGES = 100000;

    /** Random bytes in each page id and each transaction. */
    private static final int RANDOM_BYTES = 16;

    private final SecureRandom random = new SecureRandom();
    private final LongSupplier nanoClock;
    private final Map<String, Kept> pages = new LinkedHashMap<>();

    HumanConfidencePages() {
        this(System::nanoTime);
    }

    /**
     * @param nanoClock the time in nanoseconds, which a test can move on
     */
    HumanConfidencePages(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
    }

    /**
     * Make a new page id and transaction, and keep the transaction.
     * @return the page
     */
    HumanConfidencePage issue() {
        String id = randomValue();
        String transaction = randomValue();
        long now = nanoClock.getAsLong();
        synchronized (pages) {
            dropExpired(now);
            if (pages.size() >= MAX_PAGES) {
                Iterator<String> oldest = pages.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
            pages.put(id, new Kept(transaction, now));
        }
        return new HumanConfidencePage(id, transaction);
    }

    /**
     * Remove and return the transaction kept for a page.
     * @param key the page id, or null
     * @return the transaction, or null where none is kept for the key,
     * as when it was used before or has expired
     */
    String take(String key) {
        if (key == null) {
            return null;
        }
        synchronized (pages) {
            Kept kept = pages.remove(key);
            return kept == null || expired(kept, nanoClock.getAsLong()) ?
                null :
                kept.transaction;
        }
    }

    private void dropExpired(long now) {
        Iterator<Kept> oldest = pages.values().iterator();
        while (oldest.hasNext() && expired(oldest.next(), now)) {
            oldest.remove();
        }
    }

    private static boolean expired(Kept kept, long now) {
        return now - kept.issuedNanos >
            TimeUnit.MINUTES.toNanos(PAGE_LIFETIME_MINUTES);
    }

    private String randomValue() {
        byte[] bytes = new byte[RANDOM_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static final class Kept {
        final String transaction;
        final long issuedNanos;

        Kept(String transaction, long issuedNanos) {
            this.transaction = transaction;
            this.issuedNanos = issuedNanos;
        }
    }
}
