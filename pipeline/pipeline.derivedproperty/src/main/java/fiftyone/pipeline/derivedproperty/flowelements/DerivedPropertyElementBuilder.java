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

package fiftyone.pipeline.derivedproperty.flowelements;

import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.derivedproperty.BuiltInScript;
import fiftyone.pipeline.derivedproperty.DerivedScriptFault;
import fiftyone.pipeline.derivedproperty.DerivedScriptValidationException;
import fiftyone.pipeline.derivedproperty.data.DerivedPropertyData;
import fiftyone.pipeline.derivedproperty.data.IDerivedPropertyData;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds a {@link DerivedPropertyElement} from scripts given in three ways,
 * which can be mixed, being scripts shipped in this package, files on disk
 * and text in code. There is no way to fetch a script over the network.
 * <p>
 * Every script is parsed and validated when {@link #build()} is called,
 * and every fault in every script is reported at once, in one
 * {@link DerivedScriptValidationException}.
 * <p>
 * A configuration file names this builder as
 * {@code DerivedPropertyElement}, with {@code Scripts} naming built-in
 * scripts and {@code ScriptFiles} naming files, each a comma separated
 * list. Script text from code is not available from configuration.
 */
@ElementBuilder
public class DerivedPropertyElementBuilder {

    /**
     * The characters that make a file name a pattern.
     */
    private static final String WILDCARDS = "*?[{";

    /**
     * A script as given, before it is validated.
     */
    private static final class ScriptText {
        private final String name;
        private final String text;
        private final String source;

        ScriptText(String name, String text, String source) {
            this.name = name;
            this.text = text;
            this.source = source;
        }
    }

    private final ILoggerFactory loggerFactory;
    private final List<ScriptText> scripts = new ArrayList<>();
    private final List<DerivedScriptFault> faults = new ArrayList<>();

    /**
     * Construct a new builder.
     * @param loggerFactory the logger factory for the element and the
     *                      Element Data it creates
     */
    public DerivedPropertyElementBuilder(ILoggerFactory loggerFactory) {
        if (loggerFactory == null) {
            throw new IllegalArgumentException("loggerFactory");
        }
        this.loggerFactory = loggerFactory;
    }

    /**
     * Add a script shipped in this package.
     * @param script the script
     * @return this builder
     */
    public DerivedPropertyElementBuilder addScript(BuiltInScript script) {
        String text = script.readText();
        if (text == null) {
            faults.add(new DerivedScriptFault(script.name(), script.name(),
                "", 0, "the built-in script '" + script.name() + "' is not " +
                "in this package, which was built without the " +
                "derived-properties scripts"));
            return this;
        }
        scripts.add(new ScriptText(script.name(), text, script.name()));
        return this;
    }

    /**
     * Add every file the path matches. The file name may hold the wildcards
     * {@code *} and {@code ?}, as in {@code derived/*.yaml}. A script's Name
     * must equal its file name without the extension. A path matching no
     * file is a fault.
     * @param path the path, relative to the working directory or absolute
     * @return this builder
     */
    public DerivedPropertyElementBuilder addScriptFile(String path) {
        List<Path> matches = matches(path);
        if (matches.isEmpty()) {
            faults.add(new DerivedScriptFault("", path, "", 0,
                "no file matches '" + path + "'"));
            return this;
        }
        for (Path match : matches) {
            String fileName = match.getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            String name = dot > 0 ? fileName.substring(0, dot) : fileName;
            try {
                String text = new String(
                    Files.readAllBytes(match), StandardCharsets.UTF_8);
                scripts.add(new ScriptText(name, text, match.toString()));
            } catch (IOException e) {
                faults.add(new DerivedScriptFault(name, match.toString(), "",
                    0, "the file cannot be read: " + e.getMessage()));
            }
        }
        return this;
    }

    /**
     * Add a script from text in code, read as YAML or JSON as the text
     * shows.
     * @param name the name the script must have
     * @param content the text of the script
     * @return this builder
     */
    public DerivedPropertyElementBuilder addScript(
        String name,
        String content) {
        scripts.add(new ScriptText(name, content, "code"));
        return this;
    }

    /**
     * Add built-in scripts by name, which is how the {@code Scripts} build
     * parameter of a configuration file reaches the builder.
     * @param names the names, separated by commas
     * @return this builder
     */
    public DerivedPropertyElementBuilder setScripts(String names) {
        for (String name : split(names)) {
            BuiltInScript script = BuiltInScript.fromName(name);
            if (script == null) {
                faults.add(new DerivedScriptFault(name, name, "", 0, "'" +
                    name + "' is not a built-in script. The built-in " +
                    "scripts are " + BuiltInScript.names()));
            } else {
                addScript(script);
            }
        }
        return this;
    }

    /**
     * Add script files by path, which is how the {@code ScriptFiles} build
     * parameter of a configuration file reaches the builder.
     * @param paths the paths, separated by commas, each of which may hold
     *              wildcards in its file name
     * @return this builder
     */
    public DerivedPropertyElementBuilder setScriptFiles(String paths) {
        for (String path : split(paths)) {
            addScriptFile(path);
        }
        return this;
    }

    /**
     * Validate every script and build the element.
     * @return a new element
     * @throws DerivedScriptValidationException listing every fault in every
     * script, where any script has one
     * @throws IllegalStateException where no script was added
     */
    public DerivedPropertyElement build() {
        if (scripts.isEmpty() && faults.isEmpty()) {
            throw new IllegalStateException(
                "A Derived Property Element needs at least one script. Add " +
                "one with addScript or addScriptFile, or name one with the " +
                "Scripts or ScriptFiles build parameter.");
        }
        List<DerivedScriptFault> all = new ArrayList<>(faults);
        List<DerivedScript> models = new ArrayList<>();
        for (ScriptText script : scripts) {
            DerivedScriptValidator.Result result = DerivedScriptValidator
                .validate(script.text, script.name, script.source);
            all.addAll(result.getFaults());
            if (result.getScript() != null) {
                models.add(result.getScript());
            }
        }
        all.addAll(duplicates(models));
        if (all.isEmpty() == false) {
            throw new DerivedScriptValidationException(all);
        }
        Logger dataLogger =
            loggerFactory.getLogger(DerivedPropertyData.class.getName());
        DerivedPropertyElement element = new DerivedPropertyElement(
            loggerFactory.getLogger(DerivedPropertyElement.class.getName()),
            (flowData, flowElement) ->
                new DerivedPropertyData(dataLogger, flowData),
            models);
        element.logScripts();
        return element;
    }

    /**
     * Two scripts with the same name, or producing the same output
     * property, as there is one script per name and one value per output
     * property.
     */
    private static List<DerivedScriptFault> duplicates(
        List<DerivedScript> models) {
        List<DerivedScriptFault> found = new ArrayList<>();
        Map<String, DerivedScript> names = new HashMap<>();
        Map<String, DerivedScript> outputs = new HashMap<>();
        for (DerivedScript model : models) {
            String name = model.getName().toLowerCase(Locale.ROOT);
            DerivedScript first = names.get(name);
            if (first != null) {
                found.add(new DerivedScriptFault(model.getName(),
                    model.getSource(), "Name", 0, "the script '" +
                    model.getName() + "' is added more than once, from " +
                    first.getSource() + " and from " + model.getSource()));
            } else {
                names.put(name, model);
            }
            String output =
                model.getOutput().getName().toLowerCase(Locale.ROOT);
            first = outputs.get(output);
            if (first != null) {
                found.add(new DerivedScriptFault(model.getName(),
                    model.getSource(), "Output.Name", 0, "the output " +
                    "property '" + model.getOutput().getName() + "' is " +
                    "also produced by the script '" + first.getName() +
                    "' from " + first.getSource()));
            } else {
                outputs.put(output, model);
            }
        }
        return found;
    }

    /**
     * The entries of a comma separated list, with the white space around
     * each removed and empty entries left out.
     */
    private static List<String> split(String list) {
        List<String> entries = new ArrayList<>();
        if (list == null) {
            return entries;
        }
        for (String entry : list.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty() == false) {
                entries.add(trimmed);
            }
        }
        return entries;
    }

    /**
     * The files a path matches, in name order. Only the file name may hold
     * wildcards, which are read as a glob.
     */
    private static List<Path> matches(String pattern) {
        List<Path> found = new ArrayList<>();
        if (pattern == null || pattern.isEmpty()) {
            return found;
        }
        File file = new File(pattern);
        String glob = file.getName();
        try {
            if (hasWildcard(glob) == false) {
                Path path = file.toPath();
                if (Files.isRegularFile(path)) {
                    found.add(path);
                }
                return found;
            }
            File parent = file.getParentFile();
            Path directory = parent == null ?
                new File(".").toPath() : parent.toPath();
            try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(directory, glob)) {
                for (Path match : stream) {
                    if (Files.isRegularFile(match)) {
                        found.add(parent == null ?
                            match.getFileName() : match);
                    }
                }
            }
        } catch (IOException | DirectoryIteratorException |
                 IllegalArgumentException e) {
            // An invalid path or pattern matches no file.
            return Collections.emptyList();
        }
        found.sort(Comparator.comparing(Path::toString));
        return found;
    }

    private static boolean hasWildcard(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (WILDCARDS.indexOf(name.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }
}
