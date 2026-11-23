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
import fiftyone.ipintelligence.translation.Resources;
import fiftyone.ipintelligence.translation.data.ICountryCodeTranslationData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.translation.data.MissingTranslationBehavior;
import fiftyone.pipeline.translation.data.TranslationProperty;
import fiftyone.pipeline.translation.flowelements.TranslationEngineBase;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Engine which takes the weighted country code properties from IP Intelligence
 * and translates them to country names. The country names are always in
 * English. Results are stored using the {@link Constants#COUNTRY_NAMES_KEY}
 * key.
 * <p>
 * All translation files are loaded as resources, so no configuration is
 * required.
 */
public class CountryCodeTranslationEngine
    extends TranslationEngineBase<ICountryCodeTranslationData> {

    private static List<TranslationProperty> translations() {
        List<TranslationProperty> list = new ArrayList<>();
        list.add(new TranslationProperty(
            "CountryCodesGeographical", "CountryNamesGeographical"));
        list.add(new TranslationProperty(
            "CountryCodesPopulation", "CountryNamesPopulation"));
        return list;
    }

    /**
     * Construct a new instance reading from the default IP Intelligence
     * element data key ({@link Constants#IP_INTELLIGENCE_KEY}).
     * @param logger logger instance
     * @param elementDataFactory factory used to create the element data
     */
    public CountryCodeTranslationEngine(
        Logger logger,
        ElementDataFactory<ICountryCodeTranslationData> elementDataFactory) {
        this(Constants.IP_INTELLIGENCE_KEY, logger, elementDataFactory);
    }

    /**
     * Construct a new instance reading from the supplied IP Intelligence
     * element data key. The key is configurable because it has differed
     * between IP Intelligence engine versions.
     * @param sourceElementDataKey element data key of the IP Intelligence
     *                            engine
     * @param logger logger instance
     * @param elementDataFactory factory used to create the element data
     */
    public CountryCodeTranslationEngine(
        String sourceElementDataKey,
        Logger logger,
        ElementDataFactory<ICountryCodeTranslationData> elementDataFactory) {
        super(
            sourceElementDataKey,
            translations(),
            Resources.getCountryCodeResources(),
            "en_GB",
            MissingTranslationBehavior.ORIGINAL,
            ICountryCodeTranslationData.class,
            logger,
            elementDataFactory);
    }

    @Override
    public String getElementDataKey() {
        return Constants.COUNTRY_NAMES_KEY;
    }
}
