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

package fiftyone.ipintelligence.translation.data;

import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.translation.data.ITranslationData;

import java.util.List;

/**
 * Contains translated country names and the complete ordered lists of country
 * names and codes, combining the weighted results from IP Intelligence with
 * all known countries.
 */
public interface ICountriesTranslationData extends ITranslationData {

    /**
     * Translated list of country names based on the geographical weighted list
     * from IP Intelligence, translated to the browser language.
     * @return weighted list of localized country names
     */
    AspectPropertyValue<List<IWeightedValue<String>>>
        getCountryNamesGeographicalTranslated();

    /**
     * Translated list of country names based on the population weighted list
     * from IP Intelligence, translated to the browser language.
     * @return weighted list of localized country names
     */
    AspectPropertyValue<List<IWeightedValue<String>>>
        getCountryNamesPopulationTranslated();

    /**
     * Translated list of all country names ordered by geographical weighting
     * (descending), followed by the remaining countries sorted alphabetically
     * by translated name.
     * @return ordered list of localized country names
     */
    AspectPropertyValue<List<String>>
        getCountryNamesGeographicalAllTranslated();

    /**
     * Translated list of all country names ordered by population weighting
     * (descending), followed by the remaining countries sorted alphabetically
     * by translated name.
     * @return ordered list of localized country names
     */
    AspectPropertyValue<List<String>>
        getCountryNamesPopulationAllTranslated();

    /**
     * List of all country codes ordered by geographical weighting
     * (descending), followed by the remaining country codes in alphabetical
     * order of their translated country names. Index-aligned with
     * {@link #getCountryNamesGeographicalAllTranslated()}.
     * @return ordered list of country codes
     */
    AspectPropertyValue<List<String>> getCountryCodesGeographicalAll();

    /**
     * List of all country codes ordered by population weighting (descending),
     * followed by the remaining country codes in alphabetical order of their
     * translated country names. Index-aligned with
     * {@link #getCountryNamesPopulationAllTranslated()}.
     * @return ordered list of country codes
     */
    AspectPropertyValue<List<String>> getCountryCodesPopulationAll();

    /**
     * The culture used by the engine to sort the data. Exposed for test
     * purposes; not intended for customer use. May be an empty string when the
     * invariant (case-insensitive) ordering was used.
     * @return the sorting culture, or an empty string
     */
    String getSortingCultureUsed();
}
