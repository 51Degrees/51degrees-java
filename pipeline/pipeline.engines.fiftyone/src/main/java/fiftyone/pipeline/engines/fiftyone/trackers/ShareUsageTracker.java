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

package fiftyone.pipeline.engines.fiftyone.trackers;

import fiftyone.pipeline.core.data.DataKey;
import fiftyone.pipeline.core.data.DataKeyBuilder;
import fiftyone.pipeline.core.data.DataKeyBuilderDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.fiftyone.data.ShareUsageEvidence;
import fiftyone.pipeline.engines.trackers.TrackerBase;

import java.util.Date;
import java.util.Map;

/**
 * A tracker used by share usage to attempt to avoid repeatedly sending data
 * relating to the same user session. Evidence that matches an entry is shared
 * again once the interval has passed since it was last shared, so a repeat is
 * shared at most once in each interval.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/usage-sharing-element.md#session-tracking">Specification</a>
 */
public class ShareUsageTracker extends TrackerBase<Date> {

    /**
     * The filter that defines which evidence keys to use in the tracker.
     */
    private final EvidenceKeyFilter filter;

    /**
     * The interval in milliseconds.
     */
    private final long interval;

    /**
     * The key of the call this thread is tracking, set only while
     * {@link #track(FlowData, Map)} runs.
     */
    private final ThreadLocal<DataKey> callKey = new ThreadLocal<>();

    /**
     * Construct a new instance.
     * @param configuration the cache configuration to use when building the
     *                      internal cache used by this tracker
     * @param intervalMillis how long after evidence is shared a repeat of it is
     *                       not shared, in milliseconds
     * @param filter the {@link EvidenceKeyFilter} that defines the evidence
     *               values to use when creating a key from an {@link FlowData}
     */
    public ShareUsageTracker(
        CacheConfiguration configuration,
        long intervalMillis,
        EvidenceKeyFilter filter) {
        super(configuration);
        this.interval = intervalMillis;
        this.filter = filter;
    }

    @Override
    protected Date newValue(FlowData data) {
        // The time the new evidence is shared.
        return new Date();
    }

    @Override
    protected boolean match(FlowData data, Date value) {
        // Sharing a repeat stores the time in the cached value, so the next
        // interval starts from this share.
        long now = System.currentTimeMillis();
        synchronized (value) {
            if (value.getTime() <= now - interval) {
                value.setTime(now);
                return true;
            }
            return false;
        }
    }

    @Override
    protected EvidenceKeyFilter getFilter() {
        return filter;
    }

    @Override
    public boolean track(FlowData data) {
        return track(data, ShareUsageEvidence.find(data.getEvidence()));
    }

    /**
     * Tracks a call from the values {@link ShareUsageEvidence#find} has
     * already found for it, so they are not found again. The key is made
     * once, and the look up and the store of the cache both use it.
     * @param data the {@link FlowData} of the call
     * @param found the values found for the call
     * @return true if the values should be shared
     */
    public boolean track(FlowData data, Map<String, Object> found) {
        callKey.set(keyOf(found));
        try {
            return super.track(data);
        } finally {
            callKey.remove();
        }
    }

    /**
     * The key of the call being tracked on this thread when there is one,
     * otherwise a key made from the values found for the data.
     */
    @Override
    protected DataKey generateKey(FlowData data) {
        DataKey key = callKey.get();
        if (key != null) {
            return key;
        }
        return keyOf(ShareUsageEvidence.find(data.getEvidence()));
    }

    /**
     * The key is made from the values found that the filter includes, which
     * take in the web request's values, as the session tracking hash is of
     * the values that would be shared. The Evidence alone does not hold them.
     * @param found the values {@link ShareUsageEvidence#find} found
     * @return the key
     */
    private DataKey keyOf(Map<String, Object> found) {
        DataKeyBuilder builder = new DataKeyBuilderDefault();
        for (Map.Entry<String, Object> entry : found.entrySet()) {
            if (filter.include(entry.getKey())) {
                builder.add(
                    filter.order(entry.getKey()),
                    entry.getKey(),
                    entry.getValue());
            }
        }
        return builder.build();
    }
}
