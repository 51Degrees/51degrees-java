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

package fiftyone.pipeline.javascriptbuilder;

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Measures what minification costs, as the JavaScript Builder specification
 * asks of the feature, because a slow minification library once caused a
 * customer problems.
 * <p>
 * The element with minification on and the element with it off render in
 * turn, many times, so a busy machine slows both alike, and the test fails
 * only when minifying makes rendering much slower. The times are printed so
 * a change in the cost can be seen.
 * @see <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/pipeline-elements/javascript-builder.md#L109-L114">Specification</a>
 */
public class JavaScriptMinifyPerformanceTests {

    /**
     * Renders of each element before measuring, so the first render, which
     * minifies the template's form, and the JIT compiler's work are not
     * measured.
     */
    private static final int WARM_UP = 200;

    /**
     * Renders of each element that are measured.
     */
    private static final int MEASURED = 2000;

    /**
     * How many times slower a minified render may be than one not
     * minified. Minifying is done once for each form of the template and
     * not for each request, so a minified render costs about the same as
     * one that is not minified, and a minifier run on every request is many
     * times slower. The margin is wide so that timing on a busy machine
     * does not decide the result.
     */
    private static final double MAX_RATIO = 2.0;

    private final TestLoggerFactory loggerFactory = new TestLoggerFactory(null);

    @Test
    public void Minify_PerformanceImpact_Small() throws Exception {
        JavaScriptBuilderElement minified = element(true);
        JavaScriptBuilderElement asWritten = element(false);
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(new SequenceElementBuilder(loggerFactory).build())
            .addFlowElement(new JsonBuilderElementBuilder(loggerFactory).build())
            // Adds the JavaScript Builder's Element Data while the Flow Data
            // is processed, as it is read only afterwards. Both elements
            // below write that one instance.
            .addFlowElement(minified)
            .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.user-agent", "iPhone");
            data.process();

            long first = System.nanoTime();
            minified.process(data);
            long firstMinified = System.nanoTime() - first;

            for (int i = 0; i < WARM_UP; i++) {
                minified.process(data);
                asWritten.process(data);
            }

            long minifiedTime = 0;
            long asWrittenTime = 0;
            for (int i = 0; i < MEASURED; i++) {
                // Each goes first in turn, so neither always follows the
                // other.
                if (i % 2 == 0) {
                    minifiedTime += time(minified, data);
                    asWrittenTime += time(asWritten, data);
                } else {
                    asWrittenTime += time(asWritten, data);
                    minifiedTime += time(minified, data);
                }
            }

            double ratio = (double) minifiedTime / asWrittenTime;
            System.out.printf(
                "JavaScript render: first minified %.3f ms, then %.4f ms " +
                "minified and %.4f ms not minified on average over %d, " +
                "ratio %.2f%n",
                firstMinified / 1e6,
                minifiedTime / 1e6 / MEASURED,
                asWrittenTime / 1e6 / MEASURED,
                MEASURED,
                ratio);
            assertTrue(ratio <= MAX_RATIO, String.format(
                "A minified render took %.2f times as long as one not " +
                "minified, more than the %.1f allowed.", ratio, MAX_RATIO));
        }
    }

    private JavaScriptBuilderElement element(boolean minify) {
        return new JavaScriptBuilderElementBuilder(loggerFactory)
            .setEndpoint("/json")
            .setHost("example.com")
            .setProtocol("https")
            .setMinify(minify)
            .build();
    }

    private static long time(JavaScriptBuilderElement element, FlowData data)
        throws Exception {
        long start = System.nanoTime();
        element.process(data);
        return System.nanoTime() - start;
    }
}
