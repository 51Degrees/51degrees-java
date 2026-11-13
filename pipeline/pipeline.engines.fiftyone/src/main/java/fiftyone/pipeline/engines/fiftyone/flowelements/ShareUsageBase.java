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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.caching.LruPutCache;
import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.fiftyone.data.EvidenceKeyFilterShareUsage;
import fiftyone.pipeline.engines.fiftyone.data.EvidenceKeyFilterShareUsageTracker;
import fiftyone.pipeline.engines.fiftyone.data.ShareUsageEvidence;
import fiftyone.pipeline.engines.fiftyone.exceptions.HttpException;
import fiftyone.pipeline.engines.fiftyone.trackers.ShareUsageTracker;
import fiftyone.pipeline.engines.trackers.Tracker;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR;
import static fiftyone.pipeline.engines.fiftyone.flowelements.Constants.LOST_DATA_RESET_DEFAULT;


/**
 * Abstract base class for ShareUsage elements. Contains common functionality
 * such as filtering the evidence and building the XML records.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/usage-sharing-element.md">Specification</a>
 */
public abstract class ShareUsageBase
    extends FlowElementBase<ElementData, ElementPropertyMetaData> {

    /**
     * IP Addresses of local host device.
     */
    private static final InetAddress[] localHosts;
    protected static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    static {
        InetAddress[] localHosts1;
        try {
            localHosts1 = new InetAddress[]{
                InetAddress.getByName("127.0.0.1"),
                InetAddress.getByName("::1")
            };
        } catch (UnknownHostException e) {
            localHosts1 = new InetAddress[0];
        }
        localHosts = localHosts1;
    }

    static final Marker threadMarker = MarkerFactory.getMarker("Sending");

    /**
     * Executor service used to start data sending threads.
     */
    protected final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Queue used to store entries in memory prior to them being sent to
     * 51Degrees.
     */
    protected BlockingQueue<ShareUsageData> evidenceCollection;

    /**
     * Timeout to use when taking from the queue.
     */
    protected int takeTimeout;

    /**
     * The minimum number of request entries per message sent to 51Degrees.
     */
    protected final int minEntriesPerMessage;

    /**
     * The URL to send data to.
     */
    protected final String shareUsageUrl;

    /**
     * The Evidence this element accepts, which is none, so that cache keys,
     * the ETag and the Vary header never depend on it. It finds what to share
     * with {@link ShareUsageEvidence#find} instead.
     */
    private final EvidenceKeyFilter acceptedEvidence =
        new EvidenceKeyFilterWhitelist(new ArrayList<String>());

    /**
     * Which of the values found are shared, leaving out the session cookie.
     */
    private final EvidenceKeyFilter evidenceKeyFilterExclSession;

    /**
     * The filter used to determine if an item of evidence should be ignored or
     * not.
     */
    private final List<Map.Entry<String, String>> ignoreDataEvidenceFilter;

    /**
     * The host address of the current machine.
     */
    private String hostAddress;

    /**
     * Empty list. This engine returns no properties.
     */
    private List<ElementPropertyMetaData> properties;

    /**
     * Future for the last thread attempting to send data (null if none yet),
     * thread may or may not have finished
     */
    private volatile Future<?> sendDataFuture = null;

    /**
     * Lock used to start a new thread for sending data
     */
    private final Object lock = new Object();

    /**
     * Timeout to use when adding to the queue.
     */
    private final int addTimeout;

    /**
     * Random number generator used when sharing only a percentage of data i.e.
     * when {@link #sharePercentage} < 1.
     */
    private final Random random = new Random();

    /**
     * The tracker to use to determine if a {@link FlowData} instance should be
     * shared or not.
     */
    private Tracker tracker;

    /**
     * The interval is a timespan which is used to determine if a piece of
     * repeated evidence should be considered new evidence to share. If the
     * evidence from a request matches that in the tracker but this interval has
     * elapsed then the tracker will track it as new evidence.
     */
    private final long intervalMillis;

    /**
     * The approximate proportion of requests to be shared.
     * 1 = 100%, 0.5 = 50%, etc.
     */
    private double sharePercentage = 1;

    /**
     * List of {@link FlowElement} in the pipeline. This is populated when the
     * {@link #getFlowElements()} is called.
     */
    private List<String> flowElements = null;

    /**
     * Version of the OS the pipeline is being run on, as reported by
     * System.getProperty("os.name").
     */
    protected final String osVersion;

    /**
     * The Java version the pipeline is being run on, as reported by
     * System.getProperty("java.version")
     */
    protected final String languageVersion;

    /**
     * The version of the pipeline package.
     */
    protected final String coreVersion;

    /**
     * The version of this engine package.
     */
    private String enginesVersion;

    /**
     * True if usage sharing has been canceled.
     */
    private boolean canceled = false;

    /**
     * Set as soon as the element starts closing. While it is set, a consumer
     * must drain the queue completely rather than stopping as soon as fewer
     * than {@link #minEntriesPerMessage} entries are left, so that the final,
     * sub-threshold batch is not left unsent.
     */
    private volatile boolean stopping = false;

    /**
     * Keep a count of data lost
     */
    protected long lostData;

    /**
     * lostData is rest when it exceeds this value
     */
    protected long lostDataReset = LOST_DATA_RESET_DEFAULT;

    /**
     * True from the first entry discarded because the queue is full until an
     * entry is next queued. While true, a message is logged only for every
     * {@link Constants#QUEUE_FULL_LOG_EVERY} entries discarded.
     */
    private volatile boolean queueFullReported = false;

    /**
     * Decides when sending may happen after failed sends.
     */
    private final ShareUsageBackOff backOff;

    /**
     * Released when the element starts closing, to end any pause at once.
     */
    private final CountDownLatch closing = new CountDownLatch(1);

    /**
     * Constructor
     * @param logger the logger to use
     * @param sharePercentage the approximate proportion of requests to share.
     *                        1 = 100%, 0.5 = 50%, etc.
     * @param minimumEntriesPerMessage the minimum number of request entries per
     *                                 message sent to 51Degrees
     * @param maximumQueueSize the maximum number of items to hold in the queue
     *                         at one time. This must be larger than minimum
     *                         entries
     * @param addTimeout the timeout in milliseconds to allow when attempting to
     *                   add an item to the queue. If this timeout is exceeded
     *                   then usage sharing will be disabled
     * @param takeTimeout the timeout in milliseconds to allow when attempting
     *                    to take an item to the queue
     * @param repeatEvidenceIntervalMinutes the interval (in minutes) which is
     *                                      used to decide if repeat evidence is
     *                                      old enough to consider a new session
     * @param trackSession set if the tracker should consider sessions in share
     *                     usage
     * @param shareUsageUrl the URL to send data to
     * @param blockedHttpHeaders a list of the names of the HTTP headers that
     *                           share usage should not send to 51Degrees
     * @param includedQueryStringParameters a list of the names of query string
     *                                      parameters that share usage should
     *                                      send to 51Degrees
     * @param ignoreDataEvidenceFilter the filter used to determine if an item
     *                                 of evidence should be ignored or not
     */
    protected ShareUsageBase(
        Logger logger,
        double sharePercentage,
        int minimumEntriesPerMessage,
        int maximumQueueSize,
        int addTimeout,
        int takeTimeout,
        int repeatEvidenceIntervalMinutes,
        boolean trackSession,
        String shareUsageUrl,
        List<String> blockedHttpHeaders,
        List<String> includedQueryStringParameters,
        List<Map.Entry<String, String>> ignoreDataEvidenceFilter) {
        this(
            logger,
            sharePercentage,
            minimumEntriesPerMessage,
            maximumQueueSize,
            addTimeout,
            takeTimeout,
            repeatEvidenceIntervalMinutes,
            trackSession,
            shareUsageUrl,
            blockedHttpHeaders,
            includedQueryStringParameters,
            ignoreDataEvidenceFilter,
            fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME);
    }

    /**
     * Constructor
     * @param logger the logger to use
     * @param sharePercentage the approximate proportion of requests to share.
     *                        1 = 100%, 0.5 = 50%, etc.
     * @param minimumEntriesPerMessage the minimum number of request entries per
     *                                 message sent to 51Degrees
     * @param maximumQueueSize the maximum number of items to hold in the queue
     *                         at one time. This must be larger than minimum
     *                         entries
     * @param addTimeout the timeout in milliseconds to allow when attempting to
     *                   add an item to the queue. If this timeout is exceeded
     *                   then usage sharing will be disabled
     * @param takeTimeout the timeout in milliseconds to allow when attempting
     *                    to take an item to the queue
     * @param repeatEvidenceIntervalMinutes the interval (in minutes) which is
     *                                      used to decide if repeat evidence is
     *                                      old enough to consider a new session
     * @param trackSession set if the tracker should consider sessions in share
     *                     usage
     * @param shareUsageUrl the URL to send data to
     * @param blockedHttpHeaders a list of the names of the HTTP headers that
     *                           share usage should not send to 51Degrees
     * @param includedQueryStringParameters a list of the names of query string
     *                                      parameters that share usage should
     *                                      send to 51Degrees
     * @param ignoreDataEvidenceFilter the filter used to determine if an item
     *                                 of evidence should be ignored or not
     * @param sessionCookieName the name of the cookie that contains the session
     *                          id
     */
    protected ShareUsageBase(
        Logger logger,
        double sharePercentage,
        int minimumEntriesPerMessage,
        int maximumQueueSize,
        int addTimeout,
        int takeTimeout,
        int repeatEvidenceIntervalMinutes,
        boolean trackSession,
        String shareUsageUrl,
        List<String> blockedHttpHeaders,
        List<String> includedQueryStringParameters,
        List<Map.Entry<String, String>> ignoreDataEvidenceFilter,
        String sessionCookieName) {
        this(
            logger,
            sharePercentage,
            minimumEntriesPerMessage,
            maximumQueueSize,
            addTimeout,
            takeTimeout,
            repeatEvidenceIntervalMinutes,
            trackSession,
            shareUsageUrl,
            blockedHttpHeaders,
            includedQueryStringParameters,
            ignoreDataEvidenceFilter,
            sessionCookieName,
            null);
    }

    /**
     * Constructor
     * @param logger the logger to use
     * @param sharePercentage the approximate proportion of requests to share.
     *                        1 = 100%, 0.5 = 50%, etc.
     * @param minimumEntriesPerMessage the minimum number of request entries per
     *                                 message sent to 51Degrees
     * @param maximumQueueSize the maximum number of items to hold in the queue
     *                         at one time. This must be larger than minimum
     *                         entries
     * @param addTimeout the timeout in milliseconds to allow when attempting to
     *                   add an item to the queue. If this timeout is exceeded
     *                   then usage sharing will be disabled
     * @param takeTimeout the timeout in milliseconds to allow when attempting
     *                    to take an item to the queue
     * @param repeatEvidenceIntervalMinutes the interval (in minutes) which is
     *                                      used to decide if repeat evidence is
     *                                      old enough to consider a new session
     * @param trackSession set if the tracker should consider sessions in share
     *                     usage
     * @param shareUsageUrl the URL to send data to
     * @param blockedHttpHeaders a list of the names of the HTTP headers that
     *                           share usage should not send to 51Degrees
     * @param includedQueryStringParameters a list of the names of query string
     *                                      parameters that share usage should
     *                                      send to 51Degrees
     * @param ignoreDataEvidenceFilter the filter used to determine if an item
     *                                 of evidence should be ignored or not
     * @param sessionCookieName the name of the cookie that contains the session
     *                          id
     * @param tracker the {@link Tracker} to use to determine if a given
     *                {@link FlowData} instance should be shared or not
     */
    protected ShareUsageBase(
        Logger logger,
        double sharePercentage,
        int minimumEntriesPerMessage,
        int maximumQueueSize,
        int addTimeout,
        int takeTimeout,
        int repeatEvidenceIntervalMinutes,
        boolean trackSession,
        String shareUsageUrl,
        List<String> blockedHttpHeaders,
        List<String> includedQueryStringParameters,
        List<Map.Entry<String, String>> ignoreDataEvidenceFilter,
        String sessionCookieName,
        Tracker tracker) {
        this(
            logger,
            sharePercentage,
            minimumEntriesPerMessage,
            maximumQueueSize,
            addTimeout,
            takeTimeout,
            repeatEvidenceIntervalMinutes,
            trackSession,
            shareUsageUrl,
            blockedHttpHeaders,
            includedQueryStringParameters,
            ignoreDataEvidenceFilter,
            sessionCookieName,
            tracker,
            Constants.SHARE_USAGE_FAILURES_TO_ENTER_RECOVERY_DEFAULT,
            Constants.SHARE_USAGE_FAILURES_WINDOW_SECONDS_DEFAULT,
            Constants.SHARE_USAGE_EXPONENTIAL_BACKOFF_INITIAL_DELAY_SECONDS_DEFAULT,
            Constants.SHARE_USAGE_EXPONENTIAL_BACKOFF_MAX_DELAY_SECONDS_DEFAULT,
            Constants.SHARE_USAGE_EXPONENTIAL_BACKOFF_MULTIPLIER_DEFAULT,
            false,
            false);
    }

    /**
     * Constructor
     * @param logger the logger to use
     * @param sharePercentage the approximate proportion of requests to share.
     *                        1 = 100%, 0.5 = 50%, etc.
     * @param minimumEntriesPerMessage the minimum number of request entries per
     *                                 message sent to 51Degrees
     * @param maximumQueueSize the maximum number of items to hold in the queue
     *                         at one time. This must be larger than minimum
     *                         entries
     * @param addTimeout the timeout in milliseconds to allow when attempting to
     *                   add an item to the queue. If this timeout is exceeded
     *                   then the item is not shared
     * @param takeTimeout the timeout in milliseconds to allow when attempting
     *                    to take an item to the queue
     * @param repeatEvidenceIntervalMinutes the interval (in minutes) which is
     *                                      used to decide if repeat evidence is
     *                                      old enough to consider a new session
     * @param trackSession set if the tracker should consider sessions in share
     *                     usage
     * @param shareUsageUrl the URL to send data to
     * @param blockedHttpHeaders a list of the names of the HTTP headers that
     *                           share usage should not send to 51Degrees
     * @param includedQueryStringParameters a list of the names of query string
     *                                      parameters that share usage should
     *                                      send to 51Degrees
     * @param ignoreDataEvidenceFilter the filter used to determine if an item
     *                                 of evidence should be ignored or not
     * @param sessionCookieName the name of the cookie that contains the session
     *                          id
     * @param tracker the {@link Tracker} to use to determine if a given
     *                {@link FlowData} instance should be shared or not
     * @param failuresToEnterRecovery the number of failed sends within the
     *                                failures window that pauses sending
     * @param failuresWindowSeconds the time in seconds over which failed sends
     *                              are counted
     * @param initialRecoveryDelaySeconds the length in seconds of the first
     *                                    pause
     * @param maxRecoveryDelaySeconds the longest a pause can be, in seconds
     * @param recoveryMultiplier what the pause is multiplied by each time the
     *                           send tried when a pause ends fails
     * @param shareAllEvidence if true then all Evidence is shared, apart from
     *                         the keys that are never shared
     * @param shareAllQueryStringParameters if true then all query string
     *                                      parameters are shared
     */
    protected ShareUsageBase(
        Logger logger,
        double sharePercentage,
        int minimumEntriesPerMessage,
        int maximumQueueSize,
        int addTimeout,
        int takeTimeout,
        int repeatEvidenceIntervalMinutes,
        boolean trackSession,
        String shareUsageUrl,
        List<String> blockedHttpHeaders,
        List<String> includedQueryStringParameters,
        List<Map.Entry<String, String>> ignoreDataEvidenceFilter,
        String sessionCookieName,
        Tracker tracker,
        int failuresToEnterRecovery,
        int failuresWindowSeconds,
        double initialRecoveryDelaySeconds,
        double maxRecoveryDelaySeconds,
        double recoveryMultiplier,
        boolean shareAllEvidence,
        boolean shareAllQueryStringParameters) {
        super(logger, null);

        if (minimumEntriesPerMessage > maximumQueueSize) {
            throw new IllegalArgumentException(
                "The minimum entries per message cannot be larger than " +
                    "the maximum size of the queue.");
        }

        // Make sure the cookie headers are ignored.
        if (!blockedHttpHeaders.contains(Constants.EVIDENCE_HTTPHEADER_COOKIE_SUFFIX)) {
            blockedHttpHeaders.add(Constants.EVIDENCE_HTTPHEADER_COOKIE_SUFFIX);
        }

        evidenceCollection = new LinkedBlockingQueue<>(maximumQueueSize);
        backOff = new ShareUsageBackOff(
            failuresToEnterRecovery,
            failuresWindowSeconds,
            initialRecoveryDelaySeconds,
            maxRecoveryDelaySeconds,
            recoveryMultiplier);

        this.addTimeout = addTimeout;
        this.takeTimeout = takeTimeout;
        this.sharePercentage = sharePercentage;
        this.minEntriesPerMessage = minimumEntriesPerMessage;
        this.intervalMillis = (long) repeatEvidenceIntervalMinutes * 60 * 1000;
        this.shareUsageUrl = shareUsageUrl;

        // Some data is going to stay the same on all requests so we can
        // gather that now.
        languageVersion = System.getProperty("java.version");
        osVersion = System.getProperty("os.name");

        this.enginesVersion = getClass().getPackage().getImplementationVersion();
        coreVersion = Pipeline.class.getPackage().getImplementationVersion();

        includedQueryStringParameters.add(Constants.EVIDENCE_SESSIONID_SUFFIX);
        includedQueryStringParameters.add(Constants.EVIDENCE_SEQUENCE_SUFIX);

        evidenceKeyFilterExclSession = new EvidenceKeyFilterShareUsage(
            blockedHttpHeaders,
            includedQueryStringParameters,
            false,
            sessionCookieName,
            shareAllEvidence,
            shareAllQueryStringParameters);

        this.ignoreDataEvidenceFilter = ignoreDataEvidenceFilter;

        this.tracker = tracker;
        // If no tracker was supplied then create the default one.
        if (tracker == null) {
            CacheConfiguration cacheConfiguration = new CacheConfiguration(
                new LruPutCache.Builder(),
                1000);
            this.tracker = new ShareUsageTracker(
                cacheConfiguration,
                    intervalMillis,
                new EvidenceKeyFilterShareUsageTracker(
                    blockedHttpHeaders,
                    includedQueryStringParameters,
                    trackSession,
                    sessionCookieName,
                    shareAllEvidence,
                    shareAllQueryStringParameters));
        }

        properties = new ArrayList<>();
    }

    /**
     * Return a list of {@link FlowElement} in the pipeline.
     * If the list is null then populate from the pipeline.
     * If there are multiple or no pipelines then log an error.
     * @return list of flow elements
     */
    @SuppressWarnings("rawtypes")
    protected List<String> getFlowElements() {
        if (flowElements == null) {
            Pipeline pipeline;
            if (getPipelines().size() == 1) {
                pipeline = getPipelines().get(0);
                List<String> list = new ArrayList<>();
                for ( FlowElement element : pipeline.getFlowElements()) {
                    list.add(element.getClass().getSimpleName());
                }
                flowElements = list;
            } else {
                // This element has somehow been registered to too
                // many (or zero) pipelines.
                // This means we cannot know the flow elements that
                // make up the pipeline so a warning is logged
                // but otherwise, the system can continue as normal.
                logger.warn("Share usage element registered " +
                    "to " + (getPipelines().size() > 0 ? "too many" : "no") +
                    " pipelines. Unable to send share usage information.");
                flowElements = new ArrayList<>();
            }
        }
        return flowElements;
    }

    @Override
    public String getElementDataKey() {
        return "shareusage";
    }

    /**
     * Get the IP address of the machine that this code is running on.
     * @return machine IP
     */
    protected String getHostAddress() {
        if (hostAddress == null) {
            String address = null;
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                address = socket.getLocalAddress().getHostAddress();
            } catch (UnknownHostException e) {
                logger.debug("The host was unknown", e);
            } catch (SocketException e) {
                logger.debug("There was a socket exception", e);
            }

            hostAddress = address == null ? "" : address;
        }
        return hostAddress;
    }

    @Override
    public void addPipeline(Pipeline pipeline) {
        if (getPipelines().size() > 0) {
            throw new RuntimeException("Cannot add ShareUsageElement to " +
                "multiple pipelines.");
        }
        super.addPipeline(pipeline);
    }

    @Override
    protected void processInternal(FlowData flowData){
        boolean ignoreData = false;
        // The values are found here only for the ignore filter. Otherwise
        // they are found once the tracker allows the call to be shared, as
        // most calls repeat one already shared.
        Map<String, Object> found = null;

        logger.debug("processInternal()");

        if (ignoreDataEvidenceFilter != null &&
            ignoreDataEvidenceFilter.isEmpty() == false) {
            found = ShareUsageEvidence.find(flowData.getEvidence());
            for (Map.Entry<String, String> entry : ignoreDataEvidenceFilter) {
                String key = entry.getKey();
                if (found.containsKey(key)) {
                    if (found.get(key).toString().equals(entry.getValue())) {
                        ignoreData = true;
                        break;
                    }
                }
            }
        }

        if (isCanceled() == false && ignoreData == false) {
            processData(flowData, found);
        }
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return acceptedEvidence;
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return properties;
    }

    /**
     * Indicates whether share usage has been canceled as a result of an error.
     * @return true if share usage has been canceled
     */
    boolean isCanceled() {
        return canceled;
    }

    /**
     * Cancel the sending of usage data.
     */
    protected void cancel() {
        logger.warn("UsageData sending has been cancelled");
        canceled = true;
    }

    /**
     * Returns true once the element has started closing. A consumer that sees
     * this must send everything that is queued, including a final batch that is
     * smaller than {@link #minEntriesPerMessage}.
     * @return true if the element is closing
     */
    protected boolean isStopping() {
        return stopping;
    }

    /**
     * Returns true if there is a thread attempting to send usage data.
     * @return true if data is being sent
     */
    boolean isRunning() {
        return sendDataFuture != null &&
            sendDataFuture.isDone() == false &&
            sendDataFuture.isCancelled() == false;
    }

    /**
     * Get the future for the thread currently attempting to send usage data.
     * @return future for data being sent, or null if no future has yet been created
     */
    Future<?> getSendDataFuture() {
        return sendDataFuture;
    }

    @Override
    public DataFactory<ElementData> getDataFactory() {
        return null;
    }

    @Override
    protected void managedResourcesCleanup() {
        // Tell any consumer - one that is already running as well as the final
        // one queued below - that the queue has to be drained completely rather
        // than stopping once fewer than minEntriesPerMessage entries are left.
        // This is set before anything is submitted, so a consumer can never
        // observe it as false and then exit leaving a partial batch behind.
        stopping = true;
        // Ends a pause at once, so closing never waits for one. Nothing is
        // sent while paused.
        closing.countDown();
        // Queue a final consumer without consulting isRunning(). A consumer
        // that has finished its loop with a sub-threshold remainder still
        // reports as running until its Future is done, so checking would skip
        // the drain that sends the remainder. The executor is single threaded,
        // so the task queued here runs after any consumer already in flight,
        // and shutdown() still allows an already submitted task to finish.
        submitSendData();
        try {
            executor.shutdown();
            if (!executor.awaitTermination(Constants.SHARE_USAGE_DEFAULT_HTTP_POST_TIMEOUT, TimeUnit.MILLISECONDS)){
                logger.warn("Could not send final data on close down");
                executor.shutdownNow();
            }
        } catch (InterruptedException interruptedException) {
            // Not re-interrupting: a Pipeline closes its elements in sequence,
            // so leaving the flag set would make the next element's cleanup
            // abandon its own wait immediately.
            logger.warn("Interrupted while awaiting close down");
        }
        if (evidenceCollection.isEmpty() == false &&
            backOff.remaining(ShareUsageBackOff.now()) > 0) {
            logger.info(
                "{} usage sharing entries were not sent, as sending was " +
                    "paused after failed sends when usage sharing closed.",
                evidenceCollection.size());
        }
    }

    @Override
    protected void unmanagedResourcesCleanup() {

    }

    /**
     * Return true if the address is the localhost address.
     * @param address the address to check
     * @return true if localhost
     */
    @SuppressWarnings("unused")
    private boolean isLocalHost(String address) throws UnknownHostException {
        InetAddress other = InetAddress.getByName(address);
        for (InetAddress host : localHosts) {
            if (host.equals(other)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Process the supplied request data
     * @param data the {@link FlowData} instance that provides the evidence
     * @param found the values {@link ShareUsageEvidence#find} found for it,
     *              or null if they have not been found yet
     */
    private void processData(FlowData data, Map<String, Object> found) {
        logger.debug("processData()");
        if (random.nextDouble() <= sharePercentage) {
            // Check if the tracker will allow sharing of this data
            if (tracker.track(data)) {
                Map<String, Object> shared = found != null ? found :
                    ShareUsageEvidence.find(data.getEvidence());
                // Extract the data we want from the evidence and add
                // it to the collection.
                try {
                    if (evidenceCollection.offer(
                            getDataFromEvidence(shared),
                            addTimeout,
                            TimeUnit.MILLISECONDS)) {
                        queueFullReported = false;
                        logger.debug("Queued {} entries", evidenceCollection.size());
                        // If the collection has enough entries then start
                        // taking data from it to be sent.
                        if (evidenceCollection.size() >= minEntriesPerMessage) {
                            trySendData();
                        }
                    } else {
                        long lost = ++lostData;
                        if (queueFullReported == false ||
                            lost % Constants.QUEUE_FULL_LOG_EVERY == 0) {
                            queueFullReported = true;
                            logger.warn("The usage sharing queue is full, so usage data is " +
                                    "being discarded. This could mean that the queue is too " +
                                    "small, that the proportion of data shared is too high, " +
                                    "or that sending is failing. {} entries have been " +
                                    "discarded.", lost);
                        }
                        if (lost >= lostDataReset) {
                            lostData = 0;
                        }
                    }
                } catch (InterruptedException e) {
                    logger.warn("interrupted Exception while enqueueing ShareUsage data.");
                }
            }
        }
    }

    /**
     * Extract the desired data from the values found.
     * In order to avoid problems with the evidence data being disposed before
     * it is sent, the data placed into a new object rather than being a
     * reference to the existing evidence instance.
     *
     * @param found the values {@link ShareUsageEvidence#find} found
     * @return a {@link ShareUsageData} instance populated with the values
     * the sharing rules allow
     */
    private ShareUsageData getDataFromEvidence(Map<String, Object> found) {
        ShareUsageData shareUsageData = new ShareUsageData();

        for (Map.Entry<String, Object> entry : found.entrySet()) {
            // Check if we can send this piece of evidence
            if (evidenceKeyFilterExclSession.include(entry.getKey()) == false) {
                continue;
            }

            // The keys are compared without regard to case, and the constants
            // are lower case.
            switch (entry.getKey().toLowerCase(Locale.ROOT)) {
                case fiftyone.pipeline.core.Constants.EVIDENCE_CLIENTIP_KEY:
                    // The client IP is dealt with separately for backwards
                    // compatibility purposes.
                    shareUsageData.clientIP = entry.getValue().toString();
                    break;
                case Constants.EVIDENCE_SESSIONID:
                    // The SessionID is dealt with separately.
                    shareUsageData.sessionId = entry.getValue().toString();
                    break;
                case Constants.EVIDENCE_SEQUENCE:
                    // The Sequence is dealt with separately.
                    int sequence;
                    try {
                        sequence = Integer.parseInt(entry.getValue().toString(), 10);
                        shareUsageData.sequence = sequence;
                    } catch (NumberFormatException e) {
                        logger.error("The value '{}' could not be parsed to an integer.", entry.getValue());
                    }
                    break;
                default:
                    shareUsageData.tryAddToData(entry.getKey(), entry.getValue());
            }
        }
        return shareUsageData;
    }


    /**
     * Attempt to send the data to the remote service. This only happens if
     * there is not a task already running.
     */
    protected void trySendData() {
        logger.debug("trySendData");
        if (isCanceled() == false && isRunning() == false) {
            synchronized (lock) {
                if (isRunning() == false) {
                    submitSendData();
                }
            }
        }
    }

    /**
     * Submit a task that drains the queue and sends the data it takes. Unlike
     * {@link #trySendData()} this does not check whether a consumer is already
     * running - the executor is single threaded, so the new task simply runs
     * after any task that is already in flight.
     */
    private void submitSendData() {
        if (isCanceled()) {
            return;
        }
        synchronized (lock) {
            logger.debug(threadMarker, "starting runnable");
            try {
                sendDataFuture = executor.submit(
                        () -> {
                            logger.debug(threadMarker, "runnable is running");
                            try {
                                sendUsageData();
                                logger.debug(threadMarker, "Done sending");
                            } catch (Exception exception) {
                                logger.error("Share usage encountered an error.", exception);
                            }
                        }
                );
            } catch (RejectedExecutionException rejectedExecutionException) {
                // The element has already been closed, so there is nothing that
                // can be done with any data that is still queued.
                logger.debug(threadMarker,
                        "Share usage is shut down, data was not queued for sending.");
            }
        }
    }

    protected abstract void sendUsageData() throws HttpException;

    /**
     * Waits until sending may start again after failed sends. A consumer
     * calls this before each message it sends.
     * @return false if the element started closing while sending was paused,
     * in which case nothing more should be sent
     */
    protected boolean waitWhilePaused() {
        long pause;
        while ((pause = backOff.remaining(ShareUsageBackOff.now())) > 0) {
            try {
                if (closing.await(pause, TimeUnit.MILLISECONDS)) {
                    return false;
                }
            } catch (InterruptedException e) {
                // Not re-interrupting, for the reason given in
                // managedResourcesCleanup. The interrupt comes from closing.
                return false;
            }
        }
        return true;
    }

    /**
     * Records whether the remote service accepted a message, so that repeated
     * failures pause sending and a success ends the pause.
     * @param accepted true if the message was accepted
     */
    protected void recordSendResult(boolean accepted) {
        if (accepted) {
            if (backOff.succeeded()) {
                logger.info("Usage sharing resumed after a send succeeded.");
            }
        } else {
            long pause = backOff.failed(ShareUsageBackOff.now());
            if (pause > 0) {
                logger.warn(
                    "Usage sharing paused for {} seconds after failed " +
                        "sends. Usage data is still queued, up to the " +
                        "maximum queue size, and sending starts again when " +
                        "the pause ends.",
                    pause / 1000.0);
            }
        }
    }

    /**
     * Get engine Version No.
     * @return engine version no
     */
    public String getEnginesVersion() {
        return enginesVersion;
    }

    /**
     * Set Engine Version Number.
     * @param enginesVersion version number string of the engine
     */
    public void setEnginesVersion(String enginesVersion) {
        this.enginesVersion = enginesVersion;
    }

    /**
     * Inner class that is used to store details of data in memory
     * prior to it being sent to 51Degrees.
     */
    protected static class ShareUsageData {
        public String sessionId;
        public int sequence;
        public String clientIP;
        public final Map<String, Map<String, String>> evidenceData = new HashMap<>();

        void tryAddToData(String key, Object value) {
            // Get the category and field names from the evidence key.
            String category = "";
            String field = key;

            int firstSeparator = key.indexOf(EVIDENCE_SEPERATOR);
            if (firstSeparator > 0) {
                category = key.substring(0, firstSeparator);
                field = key.substring(firstSeparator + 1);
            }

            // Add the evidence to the dictionary.
            Map<String, String> categoryDict = evidenceData.get(category);
            if (Objects.isNull(categoryDict)) {
                categoryDict = new HashMap<>();
                evidenceData.put(category, categoryDict);
            }
            categoryDict.put(field, value.toString());
        }

    }
}
