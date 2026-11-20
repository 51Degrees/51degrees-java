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

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.DataUploader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A query string or form parameter is shared only when its name starts with
 * 51D_ or a Flow Element in the Pipeline accepts it, for GET and POST
 * requests alike, whatever the configuration. The deprecated included query
 * string parameters and share all query string parameters options, and
 * sharing all Evidence, share no other parameter.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/usage-sharing-element.md#shared-evidence">Specification</a>
 */
public class ShareUsageParameterTests {

    private static final String WEB_REQUEST = "server.http-request";

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private static class Capture implements DataUploader {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        @Override
        public OutputStream getOutputStream() {
            return bytes;
        }

        @Override
        public int getResponseCode() {
            return 200;
        }

        String text() {
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Accepts query.user-agent and the form parameter query.used-form-field,
     * so the Pipeline uses them.
     */
    private static class Acceptor
        extends FlowElementBase<ElementData, ElementPropertyMetaData> {
        Acceptor() {
            super(LoggerFactory.getLogger(Acceptor.class),
                (flowData, element) -> null);
        }

        @Override
        protected void processInternal(FlowData data) {
        }

        @Override
        public String getElementDataKey() {
            return "acceptor";
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(
                Arrays.asList("query.user-agent", "query.used-form-field"));
        }

        @Override
        public List<ElementPropertyMetaData> getProperties() {
            return Collections.emptyList();
        }

        @Override
        protected void managedResourcesCleanup() {
        }

        @Override
        protected void unmanagedResourcesCleanup() {
        }
    }

    /**
     * Share one request and give the record that was sent.
     * @param options which of the options that must not widen what is
     *                shared to set: none, included, shareall or everything
     */
    private String share(String options) throws Exception {
        Capture capture = new Capture();
        ShareUsageBuilder builder = new ShareUsageBuilder(
            loggerFactory,
            capture);
        builder.setMinimumEntriesPerMessage(1);
        if (options.equals("included") || options.equals("everything")) {
            builder.setIncludedQueryStringParameters("password,formpassword");
        }
        if (options.equals("shareall") || options.equals("everything")) {
            builder.setShareAllQueryStringParameters(true);
        }
        if (options.equals("everything")) {
            builder.setShareAllEvidence(true);
        }
        ShareUsageElement shareUsage = builder.build();

        Map<String, Object> post = new HashMap<>();
        post.put("query.formpassword", "form-secret-value");
        post.put("query.51D_FormValue", "form-51d-value");
        post.put("query.used-form-field", "form-accepted-value");
        post.put("header.user-agent", "header-value");

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(new Acceptor())
                .addFlowElement(shareUsage)
                .setAutoCloseElements(true)
                .build()) {
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("query.password", "query-secret-value");
                data.addEvidence("query.51D_QueryValue", "query-51d-value");
                data.addEvidence("query.user-agent", "accepted-value");
                // The web request view of a POST, which carries the form
                // parameters as query. keys.
                data.addEvidence(WEB_REQUEST, post);
                data.process();
            }
        }
        return capture.text();
    }

    /**
     * Share two requests that differ only in one query parameter and give
     * how many of them were sent. The repeat Evidence check hashes the
     * values that would be shared, so the second is a repeat only when the
     * parameter is not shared.
     * @param key the query parameter whose value differs
     */
    private int sharedOfTwo(String key) throws Exception {
        Capture capture = new Capture();
        ShareUsageBuilder builder = new ShareUsageBuilder(
            loggerFactory,
            capture);
        builder.setMinimumEntriesPerMessage(1);
        ShareUsageElement shareUsage = builder.build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(new Acceptor())
                .addFlowElement(shareUsage)
                .setAutoCloseElements(true)
                .build()) {
            for (int i = 1; i <= 2; i++) {
                try (FlowData data = pipeline.createFlowData()) {
                    data.addEvidence("header.user-agent", "same-header");
                    data.addEvidence(key, "value-" + i);
                    data.process();
                }
            }
        }
        String record = capture.text();
        return record.split("same-header", -1).length - 1;
    }

    @ParameterizedTest
    @ValueSource(strings = {"query.user-agent", "query.51D_Value"})
    public void repeats_DifferInASharedParameter_BothShared(String key)
        throws Exception {
        assertEquals(2, sharedOfTwo(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"query.password"})
    public void repeats_DifferInAParameterNotShared_OneShared(String key)
        throws Exception {
        assertEquals(1, sharedOfTwo(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "included", "shareall", "everything"})
    public void parameters_SharedOnlyWhen51DOrUsed(String options)
        throws Exception {
        String record = share(options);

        assertTrue(record.contains("header-value"), record);
        assertTrue(record.contains("query-51d-value"), record);
        assertTrue(record.contains("form-51d-value"), record);
        assertTrue(record.contains("accepted-value"), record);
        assertFalse(record.contains("query-secret-value"), record);
        assertFalse(record.contains("form-secret-value"), record);
    }

    /**
     * A form parameter sent only in the body of a POST, which a Flow Element
     * in the Pipeline accepts, is shared under every setup, just as a used
     * query string parameter in the Evidence is.
     */
    @ParameterizedTest
    @ValueSource(strings = {"none", "included", "shareall", "everything"})
    public void postParameter_UsedByThePipeline_Shared(String options)
        throws Exception {
        String record = share(options);

        assertTrue(record.contains("form-accepted-value"), record);
    }
}
