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

package fiftyone.ipintelligence.translation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads the translation YAML files that are wired in from the IP Intelligence
 * data submodule and copied onto the classpath under this package by the build
 * (see the module pom).
 * <p>
 * The set of country name locale files is known and stable, so the files are
 * loaded by name rather than by scanning the classpath, which keeps loading
 * reliable whether running from a directory or a packaged jar. A file that is
 * not present is skipped.
 */
public class Resources {

    /**
     * The single country-code to English-name file. This is also the list of
     * all known countries.
     */
    private static final String COUNTRY_CODE_FILE = "countrycodes.en_GB.yml";

    /**
     * The country English-name to localized-name files, one per shipped
     * locale.
     */
    private static final String[] COUNTRY_FILES = new String[]{
        "countries.de_DE.yml",
        "countries.es_ES.yml",
        "countries.fr_FR.yml",
        "countries.it_IT.yml",
        "countries.nl_NL.yml",
        "countries.pl_PL.yml",
        "countries.pt_PT.yml",
        "countries.sv_SE.yml",
        "countries.tr_TR.yml",
        "countries.uk_UA.yml"
    };

    private Resources() {
    }

    /**
     * Get the country name translation YAML files (English name to localized
     * name).
     * @return map of file contents keyed on file name
     */
    public static Map<String, String> getCountryResources() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String name : COUNTRY_FILES) {
            String content = read(name);
            if (content != null) {
                result.put(name, content);
            }
        }
        return result;
    }

    /**
     * Get the country code translation YAML file (ISO code to English name).
     * @return map of file contents keyed on file name
     */
    public static Map<String, String> getCountryCodeResources() {
        Map<String, String> result = new LinkedHashMap<>();
        String content = read(COUNTRY_CODE_FILE);
        if (content != null) {
            result.put(COUNTRY_CODE_FILE, content);
        }
        return result;
    }

    /**
     * Read a resource from this package as a UTF-8 string, or null if it is
     * not present.
     */
    private static String read(String name) {
        try (InputStream stream = Resources.class.getResourceAsStream(name)) {
            if (stream == null) {
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
