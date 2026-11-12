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
 * Usage sharing finds the values it shares on every call, and most calls
 * repeat one already shared and are not shared again, so finding them must
 * not copy every value more than once. These tests count how often all the
 * values of the Evidence and of the web request are read in one call.
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
     * With no web request the Evidence is read once to make the session
     * tracking key, and a call that is shared reads it once more for the
     * values it shares. Neither copies it.
     */
    @Test
    public void NoWebRequest_EvidenceReadOnceWhenNotShared()
        throws Exception {
        CountingMap evidence = headers();
        ShareUsageElement element = createElement();
        try {
            FlowData data = MockFlowData.createFromEvidence(evidence, false);
            evidence.takeReads();

            element.process(data);
            int sharedReads = evidence.takeReads();
            assertEquals(1, element.evidenceCollection.size(),
                "The first call was not shared.");

            element.process(data);
            int notSharedReads = evidence.takeReads();
            assertEquals(1, element.evidenceCollection.size(),
                "The repeat was shared.");

            assertEquals(1, notSharedReads,
                "A call that is not shared read all the Evidence more than " +
                    "once, for the session tracking key.");
            assertEquals(2, sharedReads,
                "A call that is shared read all the Evidence more than " +
                    "twice, for the session tracking key and the values " +
                    "shared.");
        } finally {
            element.close();
        }
    }

    /**
     * With a web request the Evidence and the web request are each read
     * once, to make the one copy that the session tracking key and the
     * values shared both come from.
     */
    @Test
    public void WebRequest_EachReadOnceWhetherSharedOrNot() throws Exception {
        CountingMap request = headers();
        CountingMap evidence = new CountingMap();
        evidence.put("header.user-agent", "evidence-ua");
        evidence.put(ShareUsageEvidence.HTTP_REQUEST_KEY, request);
        ShareUsageElement element = createElement();
        try {
            FlowData data = MockFlowData.createFromEvidence(evidence, false);
            evidence.takeReads();
            request.takeReads();

            element.process(data);
            int sharedEvidenceReads = evidence.takeReads();
            int sharedRequestReads = request.takeReads();
            assertEquals(1, element.evidenceCollection.size(),
                "The first call was not shared.");

            element.process(data);
            int notSharedEvidenceReads = evidence.takeReads();
            int notSharedRequestReads = request.takeReads();
            assertEquals(1, element.evidenceCollection.size(),
                "The repeat was shared.");

            assertEquals(1, notSharedRequestReads,
                "A call that is not shared copied the web request more " +
                    "than once.");
            assertEquals(1, notSharedEvidenceReads,
                "A call that is not shared copied the Evidence more than " +
                    "once.");
            assertEquals(1, sharedRequestReads,
                "A call that is shared copied the web request more than " +
                    "once.");
            assertEquals(1, sharedEvidenceReads,
                "A call that is shared copied the Evidence more than once.");
        } finally {
            element.close();
        }
    }
}
