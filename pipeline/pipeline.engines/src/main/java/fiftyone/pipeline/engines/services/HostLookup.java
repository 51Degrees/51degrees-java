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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Looks up the host of a URL within a time limit. The connect timeout of an
 * {@link java.net.HttpURLConnection} does not cover the name lookup, which
 * waits as long as the system resolver does. Looking the host up here first
 * leaves the address in the JVM's cache for the connection to use. Callers
 * waiting on the same host share one lookup, so a resolver that does not
 * answer holds one thread, not one for each request.
 */
final class HostLookup {

    /**
     * Message of the exception thrown when a lookup takes longer than the
     * timeout. The arguments are the host and the timeout in milliseconds.
     */
    static final String MessageLookupTimedOut =
        "Looking up the host '%s' took longer than the timeout of %d ms.";

    /**
     * Finds the addresses of a host.
     */
    interface Resolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    /**
     * The lookup {@link HttpClientDefault} uses, through the JVM's resolver.
     */
    static final HostLookup DEFAULT = new HostLookup(InetAddress::getAllByName);

    private static final Executor LOOKUPS = Executors.newCachedThreadPool(
        runnable -> {
            Thread thread = new Thread(runnable, "51Degrees host lookup");
            thread.setDaemon(true);
            return thread;
        });

    private final Resolver resolver;

    private final ConcurrentMap<String, CompletableFuture<InetAddress[]>>
        inProgress = new ConcurrentHashMap<>();

    HostLookup(Resolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Look up the host of the URL, waiting at most the timeout. Nothing is
     * looked up when the timeout is 0, which means no limit, or when a proxy
     * makes the request, because the proxy looks up the host.
     * @param url the URL to be requested
     * @param timeoutMillis the longest wait in milliseconds
     * @throws SocketTimeoutException if the lookup takes longer
     * @throws UnknownHostException if the host has no address
     * @throws IOException if the lookup fails in another way
     */
    void lookUp(URL url, int timeoutMillis) throws IOException {
        String host = url.getHost();
        if (timeoutMillis <= 0 ||
            host == null ||
            host.isEmpty() ||
            usesProxy(url)) {
            return;
        }
        String key = host.toLowerCase(Locale.ROOT);
        CompletableFuture<InetAddress[]> started = new CompletableFuture<>();
        CompletableFuture<InetAddress[]> lookup =
            inProgress.putIfAbsent(key, started);
        if (lookup == null) {
            lookup = started;
            start(key, host, started);
        }
        try {
            lookup.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new SocketTimeoutException(String.format(
                MessageLookupTimedOut, host, timeoutMillis));
        } catch (ExecutionException e) {
            throw failure(host, e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            InterruptedIOException interrupted = new InterruptedIOException(
                "Interrupted while looking up the host '" + host + "'.");
            interrupted.initCause(e);
            throw interrupted;
        }
    }

    /**
     * Start the lookup on its own thread. A finished lookup is removed from
     * those in progress, so the next request looks up again, which the JVM's
     * cache answers while the address is cached.
     */
    private void start(
        String key,
        String host,
        CompletableFuture<InetAddress[]> lookup) {
        try {
            LOOKUPS.execute(() -> {
                try {
                    lookup.complete(resolver.resolve(host));
                } catch (Throwable e) {
                    lookup.completeExceptionally(e);
                } finally {
                    inProgress.remove(key, lookup);
                }
            });
        } catch (RuntimeException | Error e) {
            inProgress.remove(key, lookup);
            lookup.completeExceptionally(e);
            throw e;
        }
    }

    /**
     * The exception for a lookup that failed, thrown on the caller's thread
     * with the lookup's own exception as the cause.
     */
    private static IOException failure(String host, Throwable cause) {
        if (cause instanceof UnknownHostException) {
            UnknownHostException unknown =
                new UnknownHostException(cause.getMessage());
            unknown.initCause(cause);
            return unknown;
        }
        return new IOException(
            "Looking up the host '" + host + "' failed.", cause);
    }

    /**
     * Whether the default proxy selector sends the request through a proxy.
     * A URL it cannot read counts as one, so it is not looked up here.
     */
    private static boolean usesProxy(URL url) {
        ProxySelector selector = ProxySelector.getDefault();
        if (selector == null) {
            return false;
        }
        List<Proxy> proxies;
        try {
            proxies = selector.select(url.toURI());
        } catch (URISyntaxException | IllegalArgumentException e) {
            return true;
        }
        if (proxies == null) {
            return false;
        }
        for (Proxy proxy : proxies) {
            if (proxy.type() != Proxy.Type.DIRECT) {
                return true;
            }
        }
        return false;
    }
}
