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

import fiftyone.ipintelligence.translation.Constants;
import fiftyone.ipintelligence.translation.data.CountryCodeTranslationData;
import fiftyone.pipeline.annotations.ElementBuilder;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Builder for {@link CountryCodeTranslationEngine}. This requires no
 * configuration as all the translation files are loaded as resources.
 */
@ElementBuilder
public class CountryCodeTranslationEngineBuilder {

    private final ILoggerFactory loggerFactory;
    private String sourceElementDataKey = Constants.IP_INTELLIGENCE_KEY;

    /**
     * Construct a new builder using the default logger factory.
     */
    public CountryCodeTranslationEngineBuilder() {
        this(LoggerFactory.getILoggerFactory());
    }

    /**
     * Construct a new builder.
     * @param loggerFactory the logger factory used by the engine and the
     *                     element data it creates
     */
    public CountryCodeTranslationEngineBuilder(ILoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
    }

    /**
     * Set the element data key of the IP Intelligence engine to read the
     * weighted country codes from. Defaults to
     * {@link Constants#IP_INTELLIGENCE_KEY}.
     * @param sourceElementDataKey the IP Intelligence element data key
     * @return this builder
     */
    public CountryCodeTranslationEngineBuilder setSourceElementDataKey(
        String sourceElementDataKey) {
        this.sourceElementDataKey = sourceElementDataKey;
        return this;
    }

    /**
     * Build a new {@link CountryCodeTranslationEngine}.
     * @return a new engine instance
     */
    public CountryCodeTranslationEngine build() {
        return new CountryCodeTranslationEngine(
            sourceElementDataKey,
            loggerFactory.getLogger(
                CountryCodeTranslationEngine.class.getName()),
            (flowData, flowElement) -> new CountryCodeTranslationData(
                loggerFactory.getLogger(
                    CountryCodeTranslationData.class.getName()),
                flowData));
    }
}
