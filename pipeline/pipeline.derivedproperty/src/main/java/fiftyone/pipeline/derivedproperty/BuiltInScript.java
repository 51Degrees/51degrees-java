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

package fiftyone.pipeline.derivedproperty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Names every script shipped in this package. The scripts are the
 * {@code scripts} folder of the derived-properties repository, and each
 * constant is named exactly as its script is, so {@link #name()} is the
 * script's name.
 */
public enum BuiltInScript {

    /**
     * The confidence that the request came from a device being used by a
     * human who is viewing the page, being High, Medium, Low or Unknown.
     */
    HumanConfidence,

    /**
     * Whether the General Data Protection Regulation, or the United
     * Kingdom's version of it, is likely to apply to the person making the
     * request, judged from ip.CountryCode.
     */
    IsGdpr;

    /**
     * The folder on the class path the scripts are packaged in.
     */
    public static final String RESOURCE_FOLDER =
        "/fiftyone/pipeline/derivedproperty/scripts/";

    /**
     * The file extensions a script is read from, in the order tried.
     */
    private static final String[] EXTENSIONS = {".yaml", ".yml", ".json"};

    /**
     * The size of each read from the packaged script.
     */
    private static final int BUFFER_BYTES = 8192;

    /**
     * The text of the script, read from the package.
     * @return the text, or null where the package was built without the
     * script
     */
    public String readText() {
        for (String extension : EXTENSIONS) {
            try (InputStream stream = BuiltInScript.class.getResourceAsStream(
                RESOURCE_FOLDER + name() + extension)) {
                if (stream != null) {
                    return readAll(stream);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(
                    "The built-in script '" + name() + "' cannot be read.",
                    e);
            }
        }
        return null;
    }

    /**
     * The built-in script with the name given, matched exactly.
     * @param name the script's name
     * @return the script, or null where no built-in script has the name
     */
    public static BuiltInScript fromName(String name) {
        for (BuiltInScript script : values()) {
            if (script.name().equals(name)) {
                return script;
            }
        }
        return null;
    }

    /**
     * Every built-in script's name, for a message.
     * @return the names, separated by a comma and a space
     */
    public static String names() {
        StringBuilder names = new StringBuilder();
        for (BuiltInScript script : values()) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(script.name());
        }
        return names.toString();
    }

    private static String readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_BYTES];
        int read;
        while ((read = stream.read(buffer)) > 0) {
            bytes.write(buffer, 0, read);
        }
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }
}
