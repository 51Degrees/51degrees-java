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

package fiftyone.pipeline.derived.flowelements;

import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.derived.data.DerivedPropertyData;
import fiftyone.pipeline.derived.script.DerivedScript;
import fiftyone.pipeline.derived.script.DerivedScriptException;
import fiftyone.pipeline.derived.script.DerivedScriptFault;
import fiftyone.pipeline.derived.script.DerivedScriptParser;
import fiftyone.pipeline.util.FileFinder;
import org.slf4j.ILoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a {@link DerivedPropertyElement}. Scripts arrive as text in code
 * ({@link #addScript(String, String)}), as files on disk
 * ({@link #addScriptFile(String)}, where the file name may hold a
 * wildcard), or by built-in name ({@link #addScript(String)}), and the
 * three can be mixed. Every script is parsed, validated and compiled when
 * {@link #build()} is called, and every fault in every script is reported
 * at once.
 * <p>
 * A configuration file names this builder as {@code DerivedPropertyElement}
 * and passes {@code ScriptFiles} and {@code Scripts}, each a comma
 * separated list. Script text is not available from configuration.
 */
@ElementBuilder
public class DerivedPropertyElementBuilder {

    private static final String CODE = "code";

    private final ILoggerFactory loggerFactory;

    /** Each script's text, file name (or null) and source. */
    private final List<String[]> scripts = new ArrayList<>();
    private final List<DerivedScriptFault> faults = new ArrayList<>();

    /**
     * Construct a new builder.
     * @param loggerFactory the logger factory for the element and its data
     */
    public DerivedPropertyElementBuilder(ILoggerFactory loggerFactory) {
        if (loggerFactory == null) {
            throw new IllegalArgumentException("loggerFactory");
        }
        this.loggerFactory = loggerFactory;
    }

    /**
     * Adds a script given as text, YAML or JSON, which is detected from the
     * content.
     * @param name the name the script is known by
     * @param content the text of the script
     * @return this builder
     */
    public DerivedPropertyElementBuilder addScript(String name, String content) {
        // The script's own Name must equal the name it is given.
        scripts.add(new String[]{content, name, CODE});
        return this;
    }

    /**
     * Adds a script shipped in the package, by name. This package embeds
     * no scripts, so a name is reported as a fault when the element is
     * built.
     * @param name the name of the built-in script
     * @return this builder
     */
    public DerivedPropertyElementBuilder addScript(String name) {
        faults.add(new DerivedScriptFault(name, name, "",
            "There is no built-in script named '" + name + "'. This " +
            "package embeds no scripts, so give the script as a file with " +
            "addScriptFile or as text with addScript(name, content)."));
        return this;
    }

    /**
     * Adds a script file, or every file a wildcard in the file name
     * matches, such as {@code derived/*.yaml}. A relative path that is not
     * found from the working directory is searched for below it, as the
     * examples find their files.
     * @param path the path of the file
     * @return this builder
     */
    public DerivedPropertyElementBuilder addScriptFile(String path) {
        String trimmed = path == null ? "" : path.trim();
        try {
            if (trimmed.contains("*")) {
                File file = new File(trimmed);
                File parent = file.getParentFile();
                Path folder = (parent != null ? parent : new File(".")).toPath();
                boolean found = false;
                try (DirectoryStream<Path> stream =
                         Files.newDirectoryStream(folder, file.getName())) {
                    for (Path match : stream) {
                        addFile(match.toFile());
                        found = true;
                    }
                }
                if (found == false) {
                    faults.add(new DerivedScriptFault(trimmed, trimmed, "",
                        "No script file matches '" + trimmed + "'."));
                }
            } else {
                File file = new File(trimmed);
                if (file.isFile() == false && file.isAbsolute() == false) {
                    try {
                        file = FileFinder.getFilePath(trimmed);
                    } catch (IllegalArgumentException notFound) {
                        // Reported below as a file that cannot be read.
                    }
                }
                addFile(file);
            }
        } catch (IOException e) {
            faults.add(new DerivedScriptFault(trimmed, trimmed, "",
                "The script file cannot be read. " + e.getMessage()));
        }
        return this;
    }

    private void addFile(File file) throws IOException {
        String text = new String(Files.readAllBytes(file.toPath()),
            StandardCharsets.UTF_8);
        String fileName = file.getName();
        int dot = fileName.lastIndexOf('.');
        scripts.add(new String[]{text,
            dot > 0 ? fileName.substring(0, dot) : fileName,
            file.getPath()});
    }

    /**
     * Adds script files from a comma separated list of paths, which is how
     * a configuration file passes {@code ScriptFiles}.
     * @param paths the paths, separated by commas
     * @return this builder
     */
    public DerivedPropertyElementBuilder setScriptFiles(String paths) {
        for (String path : paths.split(",")) {
            if (path.trim().isEmpty() == false) {
                addScriptFile(path);
            }
        }
        return this;
    }

    /**
     * Adds built-in scripts from a comma separated list of names, which is
     * how a configuration file passes {@code Scripts}.
     * @param names the names, separated by commas
     * @return this builder
     */
    public DerivedPropertyElementBuilder setScripts(String names) {
        for (String name : names.split(",")) {
            if (name.trim().isEmpty() == false) {
                addScript(name.trim());
            }
        }
        return this;
    }

    /**
     * Parses, validates and compiles every script, and builds the element.
     * @return the element
     * @throws DerivedScriptException listing every fault in every script
     */
    public DerivedPropertyElement build() {
        List<DerivedScriptFault> all = new ArrayList<>(faults);
        List<DerivedScript> compiled = new ArrayList<>();
        for (String[] script : scripts) {
            DerivedScript result = DerivedScriptParser.parse(
                script[0], script[1], script[2], all);
            if (result != null) {
                compiled.add(result);
            }
        }
        // One script per output property in an element.
        Map<String, String> outputs = new HashMap<>();
        for (DerivedScript script : compiled) {
            String other = outputs.put(script.getOutputName().toLowerCase(),
                script.getName());
            if (other != null) {
                all.add(new DerivedScriptFault(script.getName(),
                    script.getSource(), "Output.Name", "The scripts '" +
                    other + "' and '" + script.getName() + "' both produce '" +
                    script.getOutputName() + "'."));
            }
        }
        if (all.isEmpty() == false) {
            throw new DerivedScriptException(all);
        }
        if (compiled.isEmpty()) {
            throw new IllegalArgumentException("A Derived Property Element " +
                "needs at least one script.");
        }
        return new DerivedPropertyElement(
            loggerFactory.getLogger(DerivedPropertyElement.class.getName()),
            (flowData, element) -> new DerivedPropertyData(
                loggerFactory.getLogger(DerivedPropertyData.class.getName()),
                flowData),
            compiled);
    }
}
