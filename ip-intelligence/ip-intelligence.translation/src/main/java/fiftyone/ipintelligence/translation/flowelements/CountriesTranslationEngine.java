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

import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.ipintelligence.translation.Constants;
import fiftyone.ipintelligence.translation.data.ICountriesTranslationData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.Evidence;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.translation.data.Languages;
import fiftyone.pipeline.translation.data.MissingTranslationBehavior;
import fiftyone.pipeline.translation.data.TranslationProperty;
import fiftyone.pipeline.translation.data.Translator;
import fiftyone.pipeline.translation.flowelements.TranslationEngineBase;
import org.slf4j.Logger;

import java.text.Collator;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Engine which takes the English country name properties from
 * {@link CountryCodeTranslationEngine} and the weighted country code
 * properties from the IP Intelligence engine, translates the country names to
 * the browser language, and produces complete ordered lists of all countries
 * combining the weighted results with all known countries.
 * <p>
 * The base class handles the weighted translations
 * (CountryNamesGeographical/Population to their Translated variants). This
 * subclass adds the ordered "All" lists on top. Results are stored using the
 * {@link Constants#COUNTRY_NAMES_TRANSLATED_KEY} key.
 */
public class CountriesTranslationEngine
    extends TranslationEngineBase<ICountriesTranslationData> {

    private static final List<String> EVIDENCE_KEYS = Arrays.asList(
        "query.translation",
        "query.accept-language",
        "header.accept-language");

    private static List<TranslationProperty> translations() {
        List<TranslationProperty> list = new ArrayList<>();
        list.add(new TranslationProperty(
            "CountryNamesGeographical", "CountryNamesGeographicalTranslated"));
        list.add(new TranslationProperty(
            "CountryNamesPopulation", "CountryNamesPopulationTranslated"));
        return list;
    }

    /**
     * All known country codes and their English names, ordered as they appear
     * in countrycodes.en_GB.yml.
     */
    private final List<Map.Entry<String, String>> allCountries;

    /**
     * The set of valid country codes (keys of {@link #allCountries}), used to
     * drop the IP engine's no-match sentinel from the "All" lists.
     */
    private final Set<String> validCodes;

    private List<ElementPropertyMetaData> allProperties;

    /**
     * Construct a new instance.
     * @param logger logger instance
     * @param sources the country name translation sources, keyed on file name
     * @param allCountries all known country codes and their English names,
     *                    ordered as in countrycodes.en_GB.yml
     * @param elementDataFactory factory used to create the element data
     */
    public CountriesTranslationEngine(
        Logger logger,
        Map<String, String> sources,
        List<Map.Entry<String, String>> allCountries,
        ElementDataFactory<ICountriesTranslationData> elementDataFactory) {
        super(
            Constants.COUNTRY_NAMES_KEY,
            translations(),
            sources,
            null,
            MissingTranslationBehavior.ORIGINAL,
            ICountriesTranslationData.class,
            logger,
            elementDataFactory);
        if (allCountries == null) {
            throw new IllegalArgumentException("allCountries");
        }
        this.allCountries = Collections.unmodifiableList(
            new ArrayList<>(allCountries));
        this.validCodes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, String> country : this.allCountries) {
            this.validCodes.add(country.getKey());
        }
    }

    @Override
    public String getElementDataKey() {
        return Constants.COUNTRY_NAMES_TRANSLATED_KEY;
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        if (allProperties == null) {
            List<ElementPropertyMetaData> list =
                new ArrayList<>(super.getProperties());
            String[] names = new String[]{
                "CountryNamesGeographicalAllTranslated",
                "CountryNamesPopulationAllTranslated",
                "CountryCodesGeographicalAll",
                "CountryCodesPopulationAll"};
            for (String name : names) {
                list.add(new ElementPropertyMetaDataDefault(
                    name, this, "", List.class, true));
            }
            allProperties = list;
        }
        return allProperties;
    }

    @Override
    protected void processInternal(FlowData data) {
        // The base class produces the weighted translated names and creates
        // the element data, using the empty (pass-through) translator when the
        // resolved language is English or unknown.
        super.processInternal(data);

        ICountriesTranslationData elementData =
            data.getOrAdd(getTypedDataKey(), getDataFactory());

        String[] cultureUsed = new String[]{""};
        Translator translator;
        Comparator<String> comparator;
        Languages.Match match = resolveMatch(data);
        if (match != null) {
            translator = match.getTranslator();
            comparator = createComparator(match.getLocale(), cultureUsed);
        } else {
            translator = null;
            comparator = String.CASE_INSENSITIVE_ORDER;
        }
        elementData.put("SortingCultureUsed", cultureUsed[0]);

        List<IWeightedValue<String>> geoCodes = readCodes(data, true);
        List<IWeightedValue<String>> popCodes = readCodes(data, false);
        List<IWeightedValue<String>> geoNames = readWeightedNames(
            elementData.getCountryNamesGeographicalTranslated());
        List<IWeightedValue<String>> popNames = readWeightedNames(
            elementData.getCountryNamesPopulationTranslated());

        buildAndStore(elementData, geoNames, geoCodes, translator, comparator,
            "CountryNamesGeographicalAllTranslated",
            "CountryCodesGeographicalAll");
        buildAndStore(elementData, popNames, popCodes, translator, comparator,
            "CountryNamesPopulationAllTranslated",
            "CountryCodesPopulationAll");
    }

    /**
     * Build the complete "All" lists for one dimension and store them in the
     * element data.
     */
    private void buildAndStore(
        ICountriesTranslationData elementData,
        List<IWeightedValue<String>> translatedWeightedNames,
        List<IWeightedValue<String>> weightedCodes,
        Translator translator,
        Comparator<String> comparator,
        String namesProperty,
        String codesProperty) {
        // The weighted countries, most probable first. Drop any code that is
        // not a real country (the IP engine's no-match sentinel) so it never
        // leads the list nor is re-added by the remaining step below.
        List<Map.Entry<String, String>> weighted = new ArrayList<>();
        Set<String> usedCodes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, String> pair :
            buildWeightedTuples(
                translatedWeightedNames, weightedCodes, comparator)) {
            if (validCodes.contains(pair.getKey())) {
                weighted.add(pair);
                usedCodes.add(pair.getKey());
            }
        }

        // Every remaining known country, alphabetical by translated name.
        List<Exception> errors = new ArrayList<>();
        List<Map.Entry<String, String>> remaining = new ArrayList<>();
        for (Map.Entry<String, String> known : allCountries) {
            if (usedCodes.contains(known.getKey()) == false) {
                String name = known.getValue();
                if (translator != null) {
                    Object translated = translator.translate(name, errors);
                    if (translated instanceof String) {
                        name = (String) translated;
                    }
                }
                remaining.add(new AbstractMap.SimpleImmutableEntry<>(
                    known.getKey(), name));
            }
        }
        remaining.sort(new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(
                Map.Entry<String, String> a, Map.Entry<String, String> b) {
                return comparator.compare(a.getValue(), b.getValue());
            }
        });

        List<String> outCodes = new ArrayList<>();
        List<String> outNames = new ArrayList<>();
        for (Map.Entry<String, String> pair : weighted) {
            outCodes.add(pair.getKey());
            outNames.add(pair.getValue());
        }
        for (Map.Entry<String, String> pair : remaining) {
            outCodes.add(pair.getKey());
            outNames.add(pair.getValue());
        }

        elementData.put(codesProperty,
            new AspectPropertyValueDefault<List<String>>(outCodes));
        elementData.put(namesProperty,
            new AspectPropertyValueDefault<List<String>>(outNames));
    }

    /**
     * Zip the translated weighted names with the weighted codes by index, then
     * order by weighting (descending) and translated name (ascending).
     */
    private static List<Map.Entry<String, String>> buildWeightedTuples(
        List<IWeightedValue<String>> translatedNames,
        List<IWeightedValue<String>> codes,
        final Comparator<String> comparator) {
        List<WeightedTuple> tuples = new ArrayList<>();
        if (translatedNames != null && codes != null) {
            int count = Math.min(translatedNames.size(), codes.size());
            for (int i = 0; i < count; i++) {
                IWeightedValue<String> name = translatedNames.get(i);
                IWeightedValue<String> code = codes.get(i);
                tuples.add(new WeightedTuple(
                    code.getValue(), name.getValue(), name.getRawWeighting()));
            }
        }
        tuples.sort(new Comparator<WeightedTuple>() {
            @Override
            public int compare(WeightedTuple a, WeightedTuple b) {
                int byWeight = Integer.compare(b.weight, a.weight);
                if (byWeight != 0) {
                    return byWeight;
                }
                return comparator.compare(a.name, b.name);
            }
        });
        List<Map.Entry<String, String>> result = new ArrayList<>();
        for (WeightedTuple tuple : tuples) {
            result.add(new AbstractMap.SimpleImmutableEntry<>(
                tuple.code, tuple.name));
        }
        return result;
    }

    /**
     * Resolve the target language from the evidence using the base class's
     * available languages. English is treated as the base language (no
     * translation needed), so it yields no match.
     */
    private Languages.Match resolveMatch(FlowData data) {
        Evidence evidence = data.getEvidence();
        if (evidence == null) {
            return null;
        }
        for (String key : EVIDENCE_KEYS) {
            Object value = evidence.get(key);
            if (value instanceof String &&
                ((String) value).trim().isEmpty() == false) {
                Languages.Match match = getLanguages().match((String) value);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static Comparator<String> createComparator(
        String locale, String[] cultureUsed) {
        cultureUsed[0] = "";
        if (locale == null) {
            return String.CASE_INSENSITIVE_ORDER;
        }
        String tag = locale.replace('_', '-');
        final Collator collator =
            Collator.getInstance(Locale.forLanguageTag(tag));
        cultureUsed[0] = tag;
        return new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return collator.compare(a, b);
            }
        };
    }

    private static List<IWeightedValue<String>> readCodes(
        FlowData data, boolean geographical) {
        try {
            IPIntelligenceData ipData = data.get(IPIntelligenceData.class);
            if (ipData != null) {
                AspectPropertyValue<List<IWeightedValue<String>>> codes =
                    geographical
                        ? ipData.getCountryCodesGeographical()
                        : ipData.getCountryCodesPopulation();
                if (codes != null && codes.hasValue()) {
                    return codes.getValue();
                }
            }
        } catch (RuntimeException e) {
            // The IP Intelligence data, or the specific property, may be
            // absent. Treat that as no weighted codes.
        }
        return Collections.emptyList();
    }

    private static List<IWeightedValue<String>> readWeightedNames(
        AspectPropertyValue<List<IWeightedValue<String>>> value) {
        if (value != null && value.hasValue()) {
            return value.getValue();
        }
        return Collections.emptyList();
    }

    /**
     * A code, its translated name and the weighting used to order the weighted
     * countries.
     */
    private static final class WeightedTuple {

        private final String code;
        private final String name;
        private final int weight;

        WeightedTuple(String code, String name, int weight) {
            this.code = code;
            this.name = name;
            this.weight = weight;
        }
    }
}
