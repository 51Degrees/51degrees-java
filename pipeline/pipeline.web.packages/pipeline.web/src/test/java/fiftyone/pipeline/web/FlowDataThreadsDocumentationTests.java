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

package fiftyone.pipeline.web;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks that the web integration documentation and every web integration
 * example say that a Flow Data cannot be used by two threads at once, and
 * show that work handed to another thread gets values, not the Flow Data.
 * Reads source and documentation only.
 */
public class FlowDataThreadsDocumentationTests {

    private static final String STATEMENT =
        "a flow data cannot be used by two threads at once";

    private static final String HAND_OFF = "not the flow data";

    private static final Pattern LINE_LEADERS =
        Pattern.compile("(?m)^\\s*(\\*|//|#|>)+");

    private static final Pattern WEB_IMPORT = Pattern.compile(
        "import\\s+(javax\\.servlet|jakarta\\.servlet|" +
            "org\\.springframework\\.web)");

    private static final List<String> DOCUMENTS = Arrays.asList(
        "pipeline/README.md",
        "examples/device-detection/README.md",
        "examples/ip-intelligence/README.md",
        "location/README.md",
        "pipeline/pipeline.web.packages/pipeline.web/src/main/java/" +
            "fiftyone/pipeline/web/PipelineFilter.java",
        "pipeline/pipeline.web.packages/pipeline.web/src/main/java/" +
            "fiftyone/pipeline/web/services/FlowDataProviderCore.java",
        "pipeline/pipeline.web.packages/pipeline.web.mvc/src/main/java/" +
            "fiftyone/pipeline/web/mvc/components/FiftyOneInterceptor.java",
        "pipeline/pipeline.web.packages/pipeline.web.mvc/src/main/java/" +
            "fiftyone/pipeline/web/mvc/components/FlowDataProvider.java");

    private static final List<String> KNOWN_EXAMPLES = Arrays.asList(
        "examples/device-detection/web/getting-started.cloud/src/main/java/" +
            "fiftyone/devicedetection/examples/web/GettingStartedWebCloud.java",
        "examples/device-detection/web/getting-started.onprem/src/main/" +
            "java/fiftyone/devicedetection/examples/web/" +
            "GettingStartedWebOnPrem.java",
        "examples/ip-intelligence/web/getting-started.api/src/main/java/" +
            "fiftyone/ipintelligence/examples/web/GettingStartedApi.java",
        "examples/ip-intelligence/web/getting-started.cloud/src/main/java/" +
            "fiftyone/ipintelligence/examples/web/GettingStartedWebCloud.java",
        "examples/ip-intelligence/web/getting-started.cloud.mixed/src/main/" +
            "java/fiftyone/ipintelligence/examples/web/" +
            "GettingStartedWebCloudMixed.java",
        "examples/ip-intelligence/web/getting-started.mixed/src/main/java/" +
            "fiftyone/ipintelligence/examples/web/GettingStartedWebMixed.java",
        "examples/ip-intelligence/web/getting-started.onprem/src/main/java/" +
            "fiftyone/ipintelligence/examples/web/GettingStartedWebOnPrem.java",
        "location/geo-location.web.examples/geo-location.web.examples.mvc/" +
            "src/main/java/fiftyone/geolocation/web/examples/mvc/" +
            "configuration/ExampleMvcConfig.java",
        "location/geo-location.web.examples/geo-location.web.examples.mvc/" +
            "src/main/java/fiftyone/geolocation/web/examples/mvc/" +
            "controller/ExampleController.java",
        "location/geo-location.web.examples/" +
            "geo-location.web.examples.servlet/src/main/java/fiftyone/" +
            "geolocation/web/examples/servlet/Example.java",
        "pipeline/pipeline.developer-examples/" +
            "pipeline.developer-examples.clientside-element-mvc/src/main/" +
            "java/controller/ExampleController.java");

    @Test
    public void theWebIntegrationDocumentationSaysIt() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String document : DOCUMENTS) {
            String text = normalised(root().resolve(document));
            if (text.contains(STATEMENT) == false ||
                text.contains(HAND_OFF) == false) {
                missing.add(document);
            }
        }
        assertEquals(
            "These do not say that a Flow Data cannot be used by two " +
                "threads at once, with values, not the Flow Data, handed " +
                "to other threads",
            new ArrayList<String>(),
            missing);
    }

    @Test
    public void thePipelineReadmeShowsAHandOff() throws IOException {
        String readme = new String(
            Files.readAllBytes(root().resolve("pipeline/README.md")),
            StandardCharsets.UTF_8);
        int section = readme.indexOf("Flow Data and threads");
        assertTrue(
            "pipeline/README.md has no Flow Data and threads section",
            section >= 0);
        String rest = readme.substring(section);
        assertTrue(
            "The Flow Data and threads section shows no work handed to " +
                "an executor",
            rest.contains(".submit("));
    }

    @Test
    public void everyWebExampleSaysIt() throws IOException {
        Path root = root();
        List<String> found = webExamples(root);
        for (String known : KNOWN_EXAMPLES) {
            assertTrue(
                "The web example " + known + " was not found",
                found.contains(known));
        }
        List<String> missing = new ArrayList<>();
        for (String example : found) {
            String text = normalised(root.resolve(example));
            if (text.contains(STATEMENT) == false ||
                text.contains(HAND_OFF) == false) {
                missing.add(example);
            }
        }
        assertEquals(
            "These web examples do not say that a Flow Data cannot be " +
                "used by two threads at once, with values, not the Flow " +
                "Data, handed to other threads",
            new ArrayList<String>(),
            missing);
    }

    /**
     * Every main Java source outside the web packages that uses a servlet
     * or Spring web API and gets or creates a Flow Data.
     */
    private static List<String> webExamples(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> sources = paths
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> isExampleSource(root.relativize(p)))
                .collect(Collectors.toList());
            List<String> examples = new ArrayList<>();
            for (Path source : sources) {
                String text = new String(
                    Files.readAllBytes(source),
                    StandardCharsets.UTF_8);
                if (WEB_IMPORT.matcher(text).find() &&
                    (text.contains("getFlowData(") ||
                        text.contains("createFlowData("))) {
                    examples.add(relative(root, source));
                }
            }
            return examples;
        }
    }

    private static boolean isExampleSource(Path relative) {
        String path = relative.toString().replace(File.separatorChar, '/');
        return path.contains("/src/main/java/") &&
            path.startsWith("cxx/") == false &&
            path.startsWith("pipeline/pipeline.web.packages/") == false &&
            path.contains("/target/") == false &&
            path.contains("/node_modules/") == false;
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path).toString()
            .replace(File.separatorChar, '/');
    }

    /**
     * The text with comment and Markdown line leaders removed, white space
     * collapsed and in lower case, so a sentence wrapped over lines matches.
     */
    private static String normalised(Path path) throws IOException {
        String text = new String(
            Files.readAllBytes(path),
            StandardCharsets.UTF_8);
        return LINE_LEADERS.matcher(text).replaceAll(" ")
            .replaceAll("\\s+", " ")
            .toLowerCase();
    }

    /**
     * The repository root, being the nearest folder above the working
     * folder that holds the pipeline, examples and location folders.
     */
    private static Path root() {
        File folder = new File(System.getProperty("user.dir"))
            .getAbsoluteFile();
        while (folder != null) {
            if (new File(folder, "pipeline").isDirectory() &&
                new File(folder, "examples").isDirectory() &&
                new File(folder, "location").isDirectory()) {
                return folder.toPath();
            }
            folder = folder.getParentFile();
        }
        assertNotNull("The repository root was not found", folder);
        return null;
    }
}
