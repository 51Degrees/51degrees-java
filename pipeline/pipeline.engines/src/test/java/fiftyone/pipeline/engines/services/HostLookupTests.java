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

package fiftyone.pipeline.engines.services;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Checks that a host lookup that does not answer fails within the connect
 * timeout, as the connection's own timeouts do not cover the lookup.
 */
public class HostLookupTests {

    private static final String HOST = "unanswered.example";

    /** The timeout given to each lookup. */
    private static final int TIMEOUT_MILLIS = 200;

    /**
     * How long a resolver that does not answer holds a lookup unless the
     * test releases it. It stands in for a system resolver that waits 30
     * seconds before giving up.
     */
    private static final long UNANSWERED_MILLIS = 30000;

    /**
     * The longest a timed out lookup may take. It is many times the timeout,
     * so a slow runner passes, and far below the resolver's wait, so a
     * lookup that waited for the resolver fails.
     */
    private static final long LIMIT_MILLIS = 5000;

    /** The longest a test waits for another thread to reach a point. */
    private static final long WAIT_MILLIS = 10000;

    private final CountDownLatch release = new CountDownLatch(1);
    private final CountDownLatch entered = new CountDownLatch(1);
    private final AtomicInteger calls = new AtomicInteger();

    @After
    public void releaseResolver() {
        release.countDown();
    }

    /**
     * A resolver that holds each lookup until released, then answers with
     * the loopback address, or fails if it is never released.
     */
    private HostLookup.Resolver held() {
        return host -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                if (release.await(UNANSWERED_MILLIS, TimeUnit.MILLISECONDS)) {
                    return new InetAddress[] { InetAddress.getLoopbackAddress() };
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new UnknownHostException(host);
        };
    }

    private static URL url() throws IOException {
        return new URL("https://" + HOST + "/api/v4/evidencekeys");
    }

    private static long millisSince(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    @Test
    public void LookUp_ResolverDoesNotAnswer_TimesOutNamingTheHost()
        throws Exception {
        HostLookup lookup = new HostLookup(held());
        long start = System.nanoTime();
        try {
            lookup.lookUp(url(), TIMEOUT_MILLIS);
            fail("The lookup should have timed out.");
        } catch (SocketTimeoutException e) {
            long took = millisSince(start);
            assertTrue("The lookup took " + took + " ms.", took < LIMIT_MILLIS);
            assertEquals(
                String.format(HostLookup.MessageLookupTimedOut,
                    HOST, TIMEOUT_MILLIS),
                e.getMessage());
        }
    }

    @Test
    public void LookUp_UnknownHost_ThrowsUnknownHostWithTheCause()
        throws Exception {
        UnknownHostException unknown = new UnknownHostException(HOST);
        HostLookup lookup = new HostLookup(host -> {
            throw unknown;
        });
        try {
            lookup.lookUp(url(), TIMEOUT_MILLIS);
            fail("The lookup should have failed.");
        } catch (UnknownHostException e) {
            assertEquals(HOST, e.getMessage());
            assertSame(unknown, e.getCause());
        }
    }

    @Test
    public void LookUp_NoTimeout_DoesNotLookUp() throws Exception {
        new HostLookup(held()).lookUp(url(), 0);
        assertEquals(0, calls.get());
    }

    @Test
    public void LookUp_AfterAFailure_LooksUpAgain() throws Exception {
        HostLookup lookup = new HostLookup(host -> {
            calls.incrementAndGet();
            throw new UnknownHostException(host);
        });
        for (int i = 0; i < 2; i++) {
            try {
                lookup.lookUp(url(), TIMEOUT_MILLIS);
                fail("The lookup should have failed.");
            } catch (UnknownHostException e) {
                // Expected.
            }
        }
        assertEquals(2, calls.get());
    }

    @Test
    public void LookUp_SameHostAtOnce_SharesOneLookup() throws Exception {
        HostLookup lookup = new HostLookup(held());
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();
        Thread first = lookUpOnThread(lookup, firstFailure);
        assertTrue(entered.await(WAIT_MILLIS, TimeUnit.MILLISECONDS));
        Thread second = lookUpOnThread(lookup, secondFailure);
        // The second caller is waiting on the first one's lookup once it is
        // in a timed wait.
        long start = System.nanoTime();
        while (second.getState() != Thread.State.TIMED_WAITING &&
            millisSince(start) < WAIT_MILLIS) {
            Thread.sleep(10);
        }
        release.countDown();
        first.join(WAIT_MILLIS);
        second.join(WAIT_MILLIS);
        assertNull(firstFailure.get());
        assertNull(secondFailure.get());
        assertEquals(1, calls.get());
    }

    private static Thread lookUpOnThread(
        HostLookup lookup,
        AtomicReference<Throwable> failure) {
        Thread thread = new Thread(() -> {
            try {
                lookup.lookUp(url(), (int) WAIT_MILLIS);
            } catch (Throwable e) {
                failure.set(e);
            }
        });
        thread.start();
        return thread;
    }

    @Test
    public void LookUp_ThroughAProxy_DoesNotLookUp() throws Exception {
        ProxySelector original = ProxySelector.getDefault();
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return Collections.singletonList(new Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress.createUnresolved("proxy.example", 8080)));
            }

            @Override
            public void connectFailed(
                URI uri, SocketAddress address, IOException e) {
            }
        });
        try {
            new HostLookup(held()).lookUp(url(), TIMEOUT_MILLIS);
        } finally {
            ProxySelector.setDefault(original);
        }
        assertEquals(0, calls.get());
    }

    @Test
    public void GetResponseString_HostDoesNotAnswer_FailsWithinConnectTimeout()
        throws Exception {
        HttpClient client = new HttpClientDefault(new HostLookup(held()));
        HttpURLConnection connection = client.connect(url());
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        long start = System.nanoTime();
        try {
            client.getResponseString(connection);
            fail("The request should have timed out.");
        } catch (SocketTimeoutException e) {
            long took = millisSince(start);
            assertTrue("The request took " + took + " ms.", took < LIMIT_MILLIS);
        }
    }

    @Test
    public void PostData_HostDoesNotAnswer_FailsWithinConnectTimeout()
        throws Exception {
        HttpClient client = new HttpClientDefault(new HostLookup(held()));
        HttpURLConnection connection = client.connect(url());
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        long start = System.nanoTime();
        try {
            client.postData(
                connection, Collections.emptyMap(), new byte[] { 1 });
            fail("The request should have timed out.");
        } catch (SocketTimeoutException e) {
            long took = millisSince(start);
            assertTrue("The request took " + took + " ms.", took < LIMIT_MILLIS);
        }
    }
}
