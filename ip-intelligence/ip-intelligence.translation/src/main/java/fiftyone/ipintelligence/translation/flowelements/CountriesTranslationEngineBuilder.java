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

package fiftyone.ipintelligence.translation.flowelements;

import fiftyone.ipintelligence.translation.Resources;
import fiftyone.ipintelligence.translation.data.CountriesTranslationData;
import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.translation.util.YamlTranslations;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builder for {@link CountriesTranslationEngine}. Loads the country code
 * resource (countrycodes.en_GB.yml), which lists all known country codes and
 * their English names, and passes it to the engine as the set of all known
 * countries. The country name translation resources (countries.*.yml) are
 * handled by the base translation engine.
 */
@ElementBuilder
public class CountriesTranslationEngineBuilder {

    private final ILoggerFactory loggerFactory;

    /**
     * Construct a new builder using the default logger factory.
     */
    public CountriesTranslationEngineBuilder() {
        this(LoggerFactory.getILoggerFactory());
    }

    /**
     * Construct a new builder.
     * @param loggerFactory the logger factory used by the engine and the
     *                     element data it creates
     */
    public CountriesTranslationEngineBuilder(ILoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
    }

    /**
     * Build a new {@link CountriesTranslationEngine}.
     * @return a new engine instance
     */
    public CountriesTranslationEngine build() {
        Map<String, String> codeResources =
            Resources.getCountryCodeResources();
        List<Map.Entry<String, String>> allCountries = new ArrayList<>();
        if (codeResources.isEmpty() == false) {
            String content = codeResources.values().iterator().next();
            for (Map.Entry<String, String> entry :
                YamlTranslations.parse(content).entrySet()) {
                allCountries.add(new AbstractMap.SimpleImmutableEntry<>(
                    entry.getKey(), entry.getValue()));
            }
        }

        return new CountriesTranslationEngine(
            loggerFactory.getLogger(CountriesTranslationEngine.class.getName()),
            Resources.getCountryResources(),
            allCountries,
            (flowData, flowElement) -> new CountriesTranslationData(
                loggerFactory.getLogger(
                    CountriesTranslationData.class.getName()),
                flowData));
    }
}
