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

import fiftyone.pipeline.annotations.DefaultValue;
import fiftyone.pipeline.util.Check;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Abstract base class for ShareUsageElement builders.
 * @param <T> element type
 */
public abstract class ShareUsageBuilderBase<T extends ShareUsageBase> {

    /** Creates the loggers the element uses. */
    protected final ILoggerFactory loggerFactory;

    /** Used for logging. */
    protected final Logger logger;

    /** How long the same evidence is not shared again, in minutes. */
    protected int repeatEvidenceInterval = Constants.SHARE_USAGE_DEFAULT_REPEAT_EVIDENCE_MINUTES;
    /** The proportion of requests shared, where 1.0 is all of them. */
    protected double sharePercentage = Constants.SHARE_USAGE_DEFAULT_SHARE_PERCENTAGE;
    /** The fewest entries sent in one message. */
    protected int minimumEntriesPerMessage = Constants.SHARE_USAGE_DEFAULT_MIN_ENTRIES_PER_MESSAGE;
    /**
     * The most entries the queue holds. Read and set it through its getter and
     * setter.
     */
    protected int maximumQueueSize = Constants.SHARE_USAGE_DEFAULT_MAX_QUEUE_SIZE;
    /** Milliseconds to wait when adding to a full queue. */
    protected int addTimeout = Constants.SHARE_USAGE_DEFAULT_ADD_TIMEOUT;
    /** Milliseconds to wait for another entry before sending. */
    protected int takeTimeout = Constants.SHARE_USAGE_DEFAULT_TAKE_TIMEOUT;
    /** The address usage data is sent to. */
    protected String shareUsageUrl = Constants.SHARE_USAGE_DEFAULT_URL;
    /** Name of the cookie that holds the session id. */
    protected String sessionCookieName = fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
    /** The HTTP headers that are never shared. */
    protected List<String> blockedHttpHeaders = new ArrayList<>();
    /** Not used, as the query string parameters setting has no effect. */
    protected final List<String> includedQueryStringParameters = new ArrayList<>();
    /** Evidence whose requests are not shared, as pairs of key and value. */
    protected final List<Map.Entry<String, String>> ignoreDataEvidenceFilter = new ArrayList<>();
    /** Whether the session cookie is used to tell repeated evidence apart. */
    protected boolean trackSession;
    /**
     * Failed sends within the window that start the wait before sending again.
     */
    protected int failuresToEnterRecovery = Constants.SHARE_USAGE_FAILURES_TO_ENTER_RECOVERY_DEFAULT;
    /** The window in which failed sends are counted, in seconds. */
    protected int failuresWindowSeconds = Constants.SHARE_USAGE_FAILURES_WINDOW_SECONDS_DEFAULT;
    /** The first wait before sending again, in seconds. */
    protected double initialRecoveryDelaySeconds = Constants.SHARE_USAGE_EXPONENTIAL_BACKOFF_INITIAL_DELAY_SECONDS_DEFAULT;
    /** The longest wait before sending again, in seconds. */
    protected double maxRecoveryDelaySeconds = Constants.SHARE_USAGE_EXPONENTIAL_BACKOFF_MAX_DELAY_SECONDS_DEFAULT;
    /** How much each wait grows after another failure. */
    protected double recoveryMultiplier = Constants.SHARE_USAGE_EXPONENTIAL_BACKOFF_MULTIPLIER_DEFAULT;
    /**
     * Whether all evidence is shared, not only the keys the Pipeline accepts.
     */
    protected boolean shareAllEvidence;
    /** Not used, as the setting has no effect. */
    protected boolean shareAllQueryStringParameters =
        Constants.SHARE_USAGE_DEFAULT_SHARE_ALL_QUERY_STRING_PARAMETERS;

    /**
     * Constructor
     * @param loggerFactory the {@link ILoggerFactory} to use when creating
     *                      loggers for a {@link ShareUsageBuilderBase}
     */
    public ShareUsageBuilderBase(ILoggerFactory loggerFactory) {
        this(
            loggerFactory,
            loggerFactory.getLogger(ShareUsageBuilderBase.class.getName()));
    }

    /**
     * Constructor
     * @param loggerFactory the {@link ILoggerFactory} to use when creating
     *                      loggers for a {@link ShareUsageBuilderBase}
     * @param logger the {@link Logger} to use for {@link ShareUsageElement}
     */
    public ShareUsageBuilderBase(ILoggerFactory loggerFactory, Logger logger) {
        this.logger = logger;
        this.loggerFactory = loggerFactory;
    }

    /**
     * Deprecated, and has no effect. A query string or HTTP form parameter
     * is shared only when its name starts with 51D_ or it is Evidence the
     * Pipeline uses, being a key a Flow Element in the Pipeline accepts,
     * whatever this is set to.
     * @param queryStringParameterNames the (case-insensitive) names of
     *                                  the query string parameters
     *                                  to include
     * @return this builder
     */

    @DefaultValue("None, as it has no effect")
    public ShareUsageBuilderBase<T> setIncludedQueryStringParameters(
        List<String> queryStringParameterNames) {
        includedQueryStringParameters.clear();
        includedQueryStringParameters.addAll(queryStringParameterNames);
        return this;
    }

    /**
     * Deprecated, and has no effect. A query string or HTTP form parameter
     * is shared only when its name starts with 51D_ or it is Evidence the
     * Pipeline uses, being a key a Flow Element in the Pipeline accepts,
     * whatever this is set to.
     * @param queryStringParameterNames a comma separated list of
     *                                  (case-insensitive) names of the
     *                                  query string parameters to include
     * @return this builder
     */
    @DefaultValue("None, as it has no effect")
    public ShareUsageBuilderBase<T> setIncludedQueryStringParameters(
            String queryStringParameterNames) {
        includedQueryStringParameters.clear();
        includedQueryStringParameters.addAll(
                Arrays.asList(queryStringParameterNames.split("\\s*,\\s*")));
        return this;
    }

    /**
     * Deprecated, and has no effect. A query string or HTTP form parameter
     * is shared only when its name starts with 51D_ or it is Evidence the
     * Pipeline uses, being a key a Flow Element in the Pipeline accepts,
     * whatever this is set to.
     * @param queryStringParameterName the (case-insensitive) name of the
     *                                 query string parameter to include
     * @return this builder
     */
    @DefaultValue("None, as it has no effect")
    public ShareUsageBuilderBase<T> setIncludedQueryStringParameter(
        String queryStringParameterName) {
        includedQueryStringParameters.add(queryStringParameterName.trim());
        return this;
    }

    /**
     * Deprecated, and has no effect. Setting it shares no query string or
     * HTTP form parameter that would not otherwise be shared, which is one
     * whose name starts with 51D_ or that a Flow Element in the Pipeline
     * accepts.
     * @param shareAll kept, but not used
     * @return this builder
     */
    @DefaultValue(booleanValue = true)
    public ShareUsageBuilderBase<T> setShareAllQueryStringParameters(
        boolean shareAll) {
        this.shareAllQueryStringParameters = shareAll;
        return this;
    }

    /**
     * Deprecated. The value set for the share all query string parameters
     * option, which has no effect.
     * @return the value set
     */
    public boolean isShareAllQueryStringParameters() {
        return shareAllQueryStringParameters;
    }

    /**
     * If set to true, all Evidence is shared, which overrides the blocked
     * HTTP headers and the rule for session values. The keys that are never
     * shared (an email address, the salt for hashed email identifiers, the
     * whole query string, the path and the Cookie header) are still not
     * shared, and nor is any cookie whose name does not start with 51D_, or
     * any query string or form parameter whose name does not start with 51D_
     * and which the Pipeline does not use.
     * <p>
     * Default value is false
     * @param shareAll true to share all Evidence
     * @return this builder
     */
    @DefaultValue(booleanValue = false)
    public ShareUsageBuilderBase<T> setShareAllEvidence(boolean shareAll) {
        this.shareAllEvidence = shareAll;
        return this;
    }

    /**
     * Whether all Evidence is shared.
     * @return true if all Evidence is shared
     */
    public boolean isShareAllEvidence() {
        return shareAllEvidence;
    }

    /**
     * By default, every HTTP header is shared except Cookie. Individual
     * headers can be excluded from sharing by adding them to this list.
     * <p>
     * This setter replaces the current list
     * @param blockedHeaders the (case-insensitive) names of the headers to block
     * @return this builder
     */
    @DefaultValue("All HTTP headers are shared except Cookie")
    public ShareUsageBuilderBase<T> setBlockedHttpHeaders(List<String> blockedHeaders) {
        blockedHttpHeaders = blockedHeaders;
        return this;
    }

    /**
     * By default, every HTTP header is shared except Cookie. Individual
     * headers can be excluded from sharing by adding them to this list.
     * <p>
     * This setter adds to the blocked headers
     * @param blockedHeaders a comma separated list of
     *                                  (case-insensitive) names of the
     *                                  headers to include
     * @return this builder
     */
    @DefaultValue("All HTTP headers are shared except Cookie")
    public ShareUsageBuilderBase<T> setBlockedHttpHeaders(String blockedHeaders) {
        blockedHttpHeaders.addAll(Arrays.asList(blockedHeaders.split("\\s*,\\s*")));
        return this;
    }

    /**
     * By default, every HTTP header is shared except Cookie. Individual
     * headers can be excluded from sharing by adding them to this list.
     * <p>
     * This setter allows you to add the name of a header to block.
     * @param blockedHeader the (case-insensitive) name of the header to block
     * @return this builder
     */
    @DefaultValue("All HTTP headers are shared except Cookie")
    public ShareUsageBuilderBase<T> setBlockedHttpHeader(String blockedHeader) {
        blockedHttpHeaders.add(blockedHeader.trim());
        return this;
    }

    /**
     * This setter can be used to stop the usage sharing element
     * from sharing anything about specific requests. By default, no values
     * are suppressed.
     * <p>
     * This setter adds to the filter
     * <p>
     * For example, if you wanted to stop sharing any details from requests
     * where the user-agent header was 'ABC', you would set this
     * to "header.user-agent:ABC"
     * @param evidenceFilter Comma separated string containing entries 
     *                       in the format [evidenceKey]:[evidenceValue].
     *                       Any requests with evidence matching these 
     *                       entries will not be shared.
     * @return this builder
     */
    @DefaultValue("All values are shared")
    public ShareUsageBuilderBase<T> setIgnoreFlowDataEvidenceFilter(String evidenceFilter) {
        if (Check.isNullOrBlank(evidenceFilter)) {
            throw new IllegalArgumentException("Evidence filter must be non-null");
        }
        for (String entryString : evidenceFilter.split(",")) {
            String[] split = entryString.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("Evidence filter must be of the form " +
                        "key:value[,key:value] but was \""+evidenceFilter+"\"");
            }
            ignoreDataEvidenceFilter.add(
                    new AbstractMap.SimpleEntry<>(split[0].trim(),split[1].trim()));
        }
        return this;
    }

    /**
     * Set the percentage of data that the {@link ShareUsageElement} should be
     * sharing.
     * <p>
     * Default is 100% represented as 1.0.
     * @param sharePercentage the proportion of events sent to the pipeline that
     *                        should be shared to 51Degrees. 1 = 100%,
     *                        0.5 = 50%, etc.
     * @return this builder
     */
    @DefaultValue(doubleValue = Constants.SHARE_USAGE_DEFAULT_SHARE_PERCENTAGE)
    public ShareUsageBuilderBase<T> setSharePercentage(double sharePercentage) {
        if (sharePercentage < 0 || sharePercentage > 1.0) {
            throw new IllegalArgumentException("Share percentage must be between 0 and 1 (" +
                    sharePercentage + ")");
        }
        this.sharePercentage = sharePercentage;
        return this;
    }

    /**
     * The usage element will group data into single requests before sending it.
     * This setting controls the minimum number of entries before data is sent.
     * If you are sharing large amounts of data, increasing this value is 
     * recommended in order to reduce the overhead of sending HTTP messages.
     * <p>
     * The default value is 50.
     * @param minimumEntriesPerMessage the minimum number of entries to be
     *                                 aggregated by the {@link ShareUsageElement}
     *                                 before they are sent to the remote
     *                                 service
     * @return this builder
     */
    @DefaultValue(intValue=Constants.SHARE_USAGE_DEFAULT_MIN_ENTRIES_PER_MESSAGE)
    public ShareUsageBuilderBase<T> setMinimumEntriesPerMessage(int minimumEntriesPerMessage) {
        if (minimumEntriesPerMessage <= 0) {
            throw new IllegalArgumentException("Minimum entries per message must be greater than 0");
        }
        this.minimumEntriesPerMessage = minimumEntriesPerMessage;
        return this;
    }

    /**
     * Set the maximum number of entries to be stored in the queue to be sent.
     * This must be more than the minimum entries per message. If the queue reaches this size and
     * a new item cannot be enqueued within the add timeout the item will be dropped.
     * <p>
     * By default, the value is 20* the default minimum entries per message, i.e. 1000 entries.
     * @param size the size to set
     * @return this builder
     */
    @DefaultValue(intValue = Constants.SHARE_USAGE_DEFAULT_MAX_QUEUE_SIZE)
    public ShareUsageBuilderBase<T> setMaximumQueueSize(int size) {
        if (size <= minimumEntriesPerMessage) {
            throw new IllegalArgumentException("Maximum queue size must be greater than " +
                    "the minimum entries per message, trying to set " + size +
                    " but minimum is " + minimumEntriesPerMessage);
        }
        maximumQueueSize = size;
        return this;
    }

    /**
     * Get the maximum number of entries to be stored in the queue to be sent. 
     * @return the maximum queue size
     * */
    public int getMaximumQueueSize() {
        return maximumQueueSize;
    }

    /**
     * Set the timeout in milliseconds to allow when attempting to add an item
     * to the queue.
     * @deprecated Use {@link #setAddTimeoutMillis}
     * @param milliseconds timeout to set
     * @return this builder
     */
    @Deprecated
    @DefaultValue(intValue = Constants.SHARE_USAGE_DEFAULT_ADD_TIMEOUT)
    public ShareUsageBuilderBase<T> setAddTimeout(int milliseconds) {
        addTimeout = milliseconds;
        return this;
    }

    /**
     * Set the timeout in milliseconds to allow when attempting to add an item
     * to the queue.
     * <p>
     * Default is 0. i.e. if the item cannot be added then it is discarded.
     * @param milliseconds timeout to set
     * @return this builder
     */
    @DefaultValue(intValue = Constants.SHARE_USAGE_DEFAULT_ADD_TIMEOUT)
    public ShareUsageBuilderBase<T> setAddTimeoutMillis(int milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("Timeout must be greater than 0");
        }
        addTimeout = milliseconds;
        return this;
    }

    /**
     * Set the timeout in milliseconds to allow when attempting to take an item
     * from the queue in order to send to the remote service.
     * @deprecated use {@link #setTakeTimeoutMillis(int)}
     * @param milliseconds timeout to set
     * @return this builder
     */
    @Deprecated
    @DefaultValue(intValue = Constants.SHARE_USAGE_DEFAULT_TAKE_TIMEOUT)
    public ShareUsageBuilderBase<T> setTakeTimeout(int milliseconds) {
        takeTimeout = milliseconds;
        return this;
    }
    /**
     * Set the timeout in milliseconds to allow when attempting to take an item
     * from the queue in order to send to the remote service.
     * <p>
     * Default value is 0, i.e. if there are no more items available, send straight away.
     * @param milliseconds timeout to set
     * @return this builder
     */
    @DefaultValue(intValue = Constants.SHARE_USAGE_DEFAULT_TAKE_TIMEOUT)
    public ShareUsageBuilderBase<T> setTakeTimeoutMillis(int milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("Timeout must be greater than 0");
        }
        takeTimeout = milliseconds;
        return this;
    }

    /**
     * Set the URL to use when sharing usage data.
     * <p>
     * The default is to send to 51Degrees - see {@link Constants#SHARE_USAGE_DEFAULT_URL}
     * @param shareUsageUrl the URL to use when sharing usage data
     * @return this builder
     */
    @DefaultValue(value="Send to 51D - " + Constants.SHARE_USAGE_DEFAULT_URL)
    public ShareUsageBuilderBase<T> setShareUsageUrl(String shareUsageUrl) {
        try {
            URL url = new URI(shareUsageUrl).toURL();
            assert !url.toString().isEmpty();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        this.shareUsageUrl = shareUsageUrl;
        return this;
    }

    /**
     * Set the name of the cookie that contains the session id.
     * <p>
     * The default value is "JSESSIONID"
     * <p>
     * This setting has no effect if TrackSession is false.
     * @param cookieName the name of the cookie that contains the session id
     * @return this builder
     */
    @DefaultValue(value=fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME)
    public ShareUsageBuilderBase<T> setSessionCookieName(String cookieName) {
        // test that the cookie name is valid
        HttpCookie cookie = new HttpCookie(cookieName, "test");
        assert !cookie.getName().isEmpty();
        this.sessionCookieName = cookieName;
        return this;
    }

    /**
     * If exactly the same evidence values are seen multiple times within this
     * time limit then they will only be shared once.
     * <p>
     * The default is 20 minutes. An interval of 0, or a negative one,
     * shares every repeat.
     * @param interval the interval in minutes
     * @return this builder
     */
    @DefaultValue(intValue=Constants.SHARE_USAGE_DEFAULT_REPEAT_EVIDENCE_MINUTES)
    public ShareUsageBuilderBase<T> setRepeatEvidenceIntervalMinutes(int interval) {
        this.repeatEvidenceInterval = interval;
        return this;
    }

    /**
     * If set to true, the configured session cookie will be used to
     * identify user sessions.
     * <p>
     * This will help to differentiate duplicate values that should not be 
     * shared.
     * <p>
     * Default value is false
     * @param track boolean value sets whether the usage element should track sessions
     * @return this builder
     */
    @DefaultValue(value="false")
    public ShareUsageBuilderBase<T> setTrackSession(boolean track) {
        this.trackSession = track;
        return this;
    }

    /**
     * Set the number of failed sends within the failures window that pauses
     * sending. Usage data is still queued while sending is paused, up to the
     * maximum queue size.
     * @param failuresToEnterRecovery the number of failed sends, from 1 to 100
     * @return this builder
     */
    @DefaultValue(intValue = Constants.SHARE_USAGE_FAILURES_TO_ENTER_RECOVERY_DEFAULT)
    public ShareUsageBuilderBase<T> setFailuresToEnterRecovery(
        int failuresToEnterRecovery) {
        if (failuresToEnterRecovery < Constants.SHARE_USAGE_FAILURES_TO_ENTER_RECOVERY_MIN ||
            failuresToEnterRecovery > Constants.SHARE_USAGE_FAILURES_TO_ENTER_RECOVERY_MAX) {
            throw new IllegalArgumentException(
                "failuresToEnterRecovery must be from " +
                    Constants.SHARE_USAGE_FAILURES_TO_ENTER_RECOVERY_MIN + " to " +
                    Constants.SHARE_USAGE_FAILURES_TO_ENTER_RECOVERY_MAX +
                    ". Received: " + failuresToEnterRecovery + ".");
        }
        this.failuresToEnterRecovery = failuresToEnterRecovery;
        return this;
    }

    /**
     * Set the time in seconds over which failed sends are counted.
     * @param failuresWindowSeconds the time in seconds, 1 or more
     * @return this builder
     */
    @DefaultValue(intValue = Constants.SHARE_USAGE_FAILURES_WINDOW_SECONDS_DEFAULT)
    public ShareUsageBuilderBase<T> setFailuresWindowSeconds(
        int failuresWindowSeconds) {
        if (failuresWindowSeconds < 1) {
            throw new IllegalArgumentException(
                "failuresWindowSeconds must be 1 or more.");
        }
        this.failuresWindowSeconds = failuresWindowSeconds;
        return this;
    }

    /**
     * Set the length in seconds of the first pause after failed sends.
     * @param initialDelaySeconds the length in seconds, more than 0
     * @return this builder
     */
    @DefaultValue(doubleValue = Constants.SHARE_USAGE_EXPONENTIAL_BACKOFF_INITIAL_DELAY_SECONDS_DEFAULT)
    public ShareUsageBuilderBase<T> setInitialRecoveryDelaySeconds(
        double initialDelaySeconds) {
        if ((initialDelaySeconds > 0) == false) {
            throw new IllegalArgumentException(
                "initialDelaySeconds must be more than 0.");
        }
        this.initialRecoveryDelaySeconds = initialDelaySeconds;
        return this;
    }

    /**
     * Set the longest a pause can be, in seconds.
     * @param maxDelaySeconds the length in seconds, more than 0
     * @return this builder
     */
    @DefaultValue(doubleValue = Constants.SHARE_USAGE_EXPONENTIAL_BACKOFF_MAX_DELAY_SECONDS_DEFAULT)
    public ShareUsageBuilderBase<T> setMaxRecoveryDelaySeconds(
        double maxDelaySeconds) {
        if ((maxDelaySeconds > 0) == false) {
            throw new IllegalArgumentException(
                "maxDelaySeconds must be more than 0.");
        }
        this.maxRecoveryDelaySeconds = maxDelaySeconds;
        return this;
    }

    /**
     * Set what the pause is multiplied by each time the send tried when a
     * pause ends fails. 2 doubles the pause each time.
     * @param multiplier the multiplier, 1 or more
     * @return this builder
     */
    @DefaultValue(doubleValue = Constants.SHARE_USAGE_EXPONENTIAL_BACKOFF_MULTIPLIER_DEFAULT)
    public ShareUsageBuilderBase<T> setRecoveryMultiplier(double multiplier) {
        if ((multiplier >= 1) == false) {
            throw new IllegalArgumentException(
                "multiplier must be 1 or more.");
        }
        this.recoveryMultiplier = multiplier;
        return this;
    }

    /**
     * Create the {@link ShareUsageElement}.
     * @return the newly created target instance
     * @throws IOException if there was an exception creating the element
     */
    public abstract T build() throws IOException;
}
