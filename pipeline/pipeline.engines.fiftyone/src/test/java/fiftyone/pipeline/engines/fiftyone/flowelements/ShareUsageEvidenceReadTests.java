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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.fiftyone.data.ShareUsageEvidence;
import fiftyone.pipeline.engines.services.DataUploader;
import fiftyone.pipeline.engines.testhelpers.data.MockFlowData;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Usage sharing is called for every request, and most calls repeat one
 * already shared, which the tracker does not share again. Such a call must
 * not copy every value of the Evidence, or of the web request, more than
 * once. These tests count how often all the values of each are read in a
 * call that is not shared.
 */
public class ShareUsageEvidenceReadTests {

    /**
     * Enough values that copying them is most of the cost of a call.
     */
    private static final int VALUE_COUNT = 100;

    /**
     * A map keyed without regard to case and holding no null value, as
     * Evidence and the web request are, that counts how often all its
     * values are read.
     */
    private static final class CountingMap
        extends AbstractMap<String, Object> {

        private final Map<String, Object> values =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        private int reads;

        /**
         * @return the reads since this was last called
         */
        int takeReads() {
            int result = reads;
            reads = 0;
            return result;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            reads++;
            return values.entrySet();
        }

        @Override
        public Object get(Object key) {
            return values.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return values.containsKey(key);
        }

        @Override
        public Object put(String key, Object value) {
            return values.put(key, value);
        }

        @Override
        public int size() {
            return values.size();
        }
    }

    private static CountingMap headers() {
        CountingMap headers = new CountingMap();
        for (int i = 0; i < VALUE_COUNT; i++) {
            headers.put("header.x-test-" + i, "value-" + i);
        }
        return headers;
    }

    /**
     * An element with the default tracker, sharing every call that is not
     * a repeat, and sending nothing until 50 calls are queued.
     */
    private static ShareUsageElement createElement() throws Exception {
        return new ShareUsageBuilder(
            LoggerFactory.getILoggerFactory(),
            mock(DataUploader.class))
            .build();
    }

    /**
     * With no web request a call that is not shared reads the Evidence once,
     * to make the session tracking key, and does not copy it.
     */
    @Test
    public void NoWebRequest_EvidenceReadOnceWhenNotShared()
        throws Exception {
        CountingMap evidence = headers();
        ShareUsageElement element = createElement();
        try {
            FlowData data = MockFlowData.createFromEvidence(evidence, false);
            element.process(data);
            assertEquals(1, element.evidenceCollection.size(),
                "The first call was not shared.");

            evidence.takeReads();
            element.process(data);
            int reads = evidence.takeReads();
            assertEquals(1, element.evidenceCollection.size(),
                "The repeat was shared.");

            assertEquals(1, reads,
                "A call that is not shared read all the Evidence more than " +
                    "once, when the session tracking key needs it once.");
        } finally {
            element.close();
        }
    }

    /**
     * With a web request a call that is not shared reads the Evidence and
     * the web request once each, to make the one copy that the session
     * tracking key is made from.
     */
    @Test
    public void WebRequest_EachCopiedOnceWhenNotShared() throws Exception {
        CountingMap request = headers();
        CountingMap evidence = new CountingMap();
        evidence.put("header.user-agent", "evidence-ua");
        evidence.put(ShareUsageEvidence.HTTP_REQUEST_KEY, request);
        ShareUsageElement element = createElement();
        try {
            FlowData data = MockFlowData.createFromEvidence(evidence, false);
            element.process(data);
            assertEquals(1, element.evidenceCollection.size(),
                "The first call was not shared.");

            evidence.takeReads();
            request.takeReads();
            element.process(data);
            int evidenceReads = evidence.takeReads();
            int requestReads = request.takeReads();
            assertEquals(1, element.evidenceCollection.size(),
                "The repeat was shared.");

            assertEquals(1, requestReads,
                "A call that is not shared copied the web request more " +
                    "than once.");
            assertEquals(1, evidenceReads,
                "A call that is not shared copied the Evidence more than " +
                    "once.");
        } finally {
            element.close();
        }
    }
}
