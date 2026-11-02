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

import fiftyone.ipintelligence.translation.data.ICountriesTranslationData;
import fiftyone.ipintelligence.translation.data.ICountryCodeTranslationData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.junit.Test;

import java.text.Collator;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static fiftyone.ipintelligence.translation.testhelpers.Fixtures.apv;
import static fiftyone.ipintelligence.translation.testhelpers.Fixtures.build;
import static fiftyone.ipintelligence.translation.testhelpers.Fixtures.buildCustom;
import static fiftyone.ipintelligence.translation.testhelpers.Fixtures.noValue;
import static fiftyone.ipintelligence.translation.testhelpers.Fixtures.weighted;
import static fiftyone.ipintelligence.translation.testhelpers.Fixtures.weightify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TranslationTests {

    private static List<String> values(
        AspectPropertyValue<List<IWeightedValue<String>>> value) {
        List<String> result = new ArrayList<>();
        for (IWeightedValue<String> item : value.getValue()) {
            result.add(item.getValue());
        }
        return result;
    }

    // ===================================================================
    // 10a. Ported from the .NET suite.
    // ===================================================================

    @Test
    public void countryNamesFromCodes() throws Exception {
        try (Pipeline pipeline =
                build(apv(weightify("GB", "FR")), apv(weightify("GB", "FR")));
             FlowData data = pipeline.createFlowData()) {
            data.process();
            ICountryCodeTranslationData names =
                data.get(ICountryCodeTranslationData.class);
            assertEquals(Arrays.asList("United Kingdom", "France"),
                values(names.getCountryNamesGeographical()));
            assertEquals(Arrays.asList("United Kingdom", "France"),
                values(names.getCountryNamesPopulation()));
        }
    }

    @Test
    public void translatedCountry() throws Exception {
        try (Pipeline pipeline =
                build(apv(weightify("GB", "FR")), apv(weightify("GB", "FR")));
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "fr_FR");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            assertEquals(Arrays.asList("Royaume-Uni", "France"),
                values(ct.getCountryNamesGeographicalTranslated()));
        }
    }

    @Test
    public void allListsProducedCorrectlySorted() throws Exception {
        // FR has the lower weight, GB the higher: GB should lead.
        List<IWeightedValue<String>> codes = new ArrayList<>();
        codes.add(weighted(30000, "FR"));
        codes.add(weighted(35535, "GB"));
        try (Pipeline pipeline = build(apv(codes), apv(codes));
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "fr_FR");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            List<String> allCodes =
                ct.getCountryCodesGeographicalAll().getValue();
            List<String> allNames =
                ct.getCountryNamesGeographicalAllTranslated().getValue();

            assertEquals("GB", allCodes.get(0));
            assertEquals("FR", allCodes.get(1));
            assertEquals("Royaume-Uni", allNames.get(0));
            assertEquals("France", allNames.get(1));
            assertEquals(250, allCodes.size());
            assertEquals(250, allNames.size());
            // GB and FR are not repeated in the alphabetical tail.
            assertEquals(1, frequency(allCodes, "GB"));
            assertEquals(1, frequency(allCodes, "FR"));
            // Tail is sorted by the locale comparer.
            assertTailSorted(allNames, 2, "fr-FR");
        }
    }

    @Test
    public void allListsProducedCorrectly() throws Exception {
        try (Pipeline pipeline =
                build(apv(weightify("GB", "FR")), apv(weightify("GB", "FR")));
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "fr_FR");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            List<String> allCodes =
                ct.getCountryCodesGeographicalAll().getValue();
            assertEquals("GB", allCodes.get(0));
            assertEquals("FR", allCodes.get(1));
            assertEquals(250, allCodes.size());
        }
    }

    @Test
    public void allListsWithoutLanguage() throws Exception {
        try (Pipeline pipeline =
                build(apv(weightify("GB", "FR")), apv(weightify("GB", "FR")));
             FlowData data = pipeline.createFlowData()) {
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            List<String> allCodes =
                ct.getCountryCodesGeographicalAll().getValue();
            List<String> allNames =
                ct.getCountryNamesGeographicalAllTranslated().getValue();
            assertEquals("GB", allCodes.get(0));
            assertEquals("United Kingdom", allNames.get(0));
            // Tail is the English names, alphabetically.
            assertEquals("Afghanistan", allNames.get(2));
            assertEquals("", ct.getSortingCultureUsed());
        }
    }

    @Test
    public void allListsWithNoIpData() throws Exception {
        try (Pipeline pipeline = build(noValue(), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "de_DE");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            List<String> allCodes =
                ct.getCountryCodesGeographicalAll().getValue();
            List<String> allNames =
                ct.getCountryNamesGeographicalAllTranslated().getValue();
            // Every country is present and fully alphabetical (no weighted
            // leaders).
            assertEquals(250, allCodes.size());
            assertEquals(250, allNames.size());
            assertEquals("de-DE", ct.getSortingCultureUsed());
            assertTailSorted(allNames, 0, "de-DE");
            // The German name is present and aligned with its code.
            assertEquals("Deutschland", allNames.get(allCodes.indexOf("DE")));
        }
    }

    @Test
    public void populationAndGeographicalAreIndependent() throws Exception {
        try (Pipeline pipeline =
                build(apv(weightify("DE")), apv(weightify("US", "CN")));
             FlowData data = pipeline.createFlowData()) {
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            assertEquals("DE",
                ct.getCountryCodesGeographicalAll().getValue().get(0));
            assertEquals("US",
                ct.getCountryCodesPopulationAll().getValue().get(0));
            assertEquals("CN",
                ct.getCountryCodesPopulationAll().getValue().get(1));
        }
    }

    @Test
    public void germanTranslation() throws Exception {
        try (Pipeline pipeline = build(apv(weightify("DE")), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "de_DE");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            assertEquals("Deutschland",
                values(ct.getCountryNamesGeographicalTranslated()).get(0));
            assertEquals("Deutschland",
                ct.getCountryNamesGeographicalAllTranslated().getValue().get(0));
            assertEquals("DE",
                ct.getCountryCodesGeographicalAll().getValue().get(0));
        }
    }

    @Test
    public void acceptLanguageWithDash() throws Exception {
        try (Pipeline pipeline = build(apv(weightify("GB")), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "fr-FR");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            assertEquals("Royaume-Uni",
                values(ct.getCountryNamesGeographicalTranslated()).get(0));
        }
    }

    @Test
    public void englishLanguageNoTranslation() throws Exception {
        try (Pipeline pipeline =
                build(apv(weightify("GB", "FR")), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "en-US,en;q=0.9");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            assertEquals(Arrays.asList("United Kingdom", "France"),
                values(ct.getCountryNamesGeographicalTranslated()));
            assertEquals("", ct.getSortingCultureUsed());
        }
    }

    @Test
    public void englishPreferredOverOtherLanguages() throws Exception {
        try (Pipeline pipeline = build(apv(weightify("DE")), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence(
                "header.accept-language", "en-US,en;q=0.9,de-DE;q=0.5");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            // English wins, so the name stays English (not "Deutschland").
            assertEquals("Germany",
                values(ct.getCountryNamesGeographicalTranslated()).get(0));
        }
    }

    @Test
    public void preferredLanguageMatchedBeforeLowerPriority() throws Exception {
        try (Pipeline pipeline = build(apv(weightify("DE")), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence(
                "header.accept-language", "es,de-DE;q=0.8,fr;q=0.5");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            // Spanish (es -> es_ES) wins.
            assertEquals("Alemania",
                values(ct.getCountryNamesGeographicalTranslated()).get(0));
        }
    }

    // ===================================================================
    // 10b. Gaps not covered by the .NET suite.
    // ===================================================================

    @Test
    public void queryTranslationEvidenceTranslates() throws Exception {
        try (Pipeline pipeline = build(apv(weightify("GB")), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.translation", "fr_FR");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            assertEquals("Royaume-Uni",
                values(ct.getCountryNamesGeographicalTranslated()).get(0));
        }
    }

    @Test
    public void queryTranslationOverridesHeader() throws Exception {
        try (Pipeline pipeline = build(apv(weightify("GB")), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.translation", "fr_FR");
            data.addEvidence("header.accept-language", "de_DE");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            // query.translation has higher precedence, so French wins.
            assertEquals("Royaume-Uni",
                values(ct.getCountryNamesGeographicalTranslated()).get(0));
            assertEquals("fr-FR", ct.getSortingCultureUsed());
        }
    }

    @Test
    public void weightsPreservedThroughTranslation() throws Exception {
        List<IWeightedValue<String>> codes = new ArrayList<>();
        codes.add(weighted(30000, "FR"));
        codes.add(weighted(35535, "GB"));
        try (Pipeline pipeline = build(apv(codes), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "fr_FR");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            List<IWeightedValue<String>> translated =
                ct.getCountryNamesGeographicalTranslated().getValue();
            // Order is preserved (input FR, GB); only the names changed.
            assertEquals("France", translated.get(0).getValue());
            assertEquals(30000, translated.get(0).getRawWeighting());
            assertEquals("Royaume-Uni", translated.get(1).getValue());
            assertEquals(35535, translated.get(1).getRawWeighting());
        }
    }

    @Test
    public void equalWeightTieBreakByName() throws Exception {
        List<IWeightedValue<String>> codes = new ArrayList<>();
        codes.add(weighted(20000, "GB"));
        codes.add(weighted(20000, "FR"));
        try (Pipeline pipeline = build(apv(codes), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "fr_FR");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            List<String> allCodes =
                ct.getCountryCodesGeographicalAll().getValue();
            // Equal weights, so the secondary sort by translated name applies:
            // "France" < "Royaume-Uni", so FR leads despite GB being first in.
            assertEquals("FR", allCodes.get(0));
            assertEquals("GB", allCodes.get(1));
        }
    }

    @Test
    public void unknownLocaleFallsBackToEnglish() throws Exception {
        try (Pipeline pipeline =
                build(apv(weightify("GB", "FR")), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "zz-ZZ");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            assertEquals(Arrays.asList("United Kingdom", "France"),
                values(ct.getCountryNamesGeographicalTranslated()));
            // Every property is still populated.
            assertEquals(250,
                ct.getCountryCodesGeographicalAll().getValue().size());
            assertEquals(250,
                ct.getCountryNamesGeographicalAllTranslated().getValue().size());
            assertEquals("", ct.getSortingCultureUsed());
        }
    }

    @Test
    public void missingSingleNameStaysEnglish() throws Exception {
        // A custom locale map missing "France" but containing "Germany".
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("countries.xx_XX.yml", "Germany: Schland\n");
        List<Map.Entry<String, String>> allCountries = new ArrayList<>();
        allCountries.add(new AbstractMap.SimpleImmutableEntry<>("DE", "Germany"));
        allCountries.add(new AbstractMap.SimpleImmutableEntry<>("FR", "France"));

        try (Pipeline pipeline = buildCustom(
                apv(weightify("DE", "FR")), noValue(), sources, allCountries);
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "xx_XX");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            // Germany is translated; France is missing so stays English.
            assertEquals(Arrays.asList("Schland", "France"),
                values(ct.getCountryNamesGeographicalTranslated()));
        }
    }

    @Test
    public void fullIndexAlignmentIncludingTail() throws Exception {
        try (Pipeline pipeline =
                build(apv(weightify("GB", "FR")), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "fr_FR");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            List<String> allCodes =
                ct.getCountryCodesGeographicalAll().getValue();
            List<String> allNames =
                ct.getCountryNamesGeographicalAllTranslated().getValue();
            assertEquals(allCodes.size(), allNames.size());
            // Spot-check alignment across the alphabetical tail.
            assertEquals("Allemagne", allNames.get(allCodes.indexOf("DE")));
            assertEquals("Espagne", allNames.get(allCodes.indexOf("ES")));
            assertEquals("Italie", allNames.get(allCodes.indexOf("IT")));
            assertEquals("Chine", allNames.get(allCodes.indexOf("CN")));
            assertEquals("Japon", allNames.get(allCodes.indexOf("JP")));
        }
    }

    @Test
    public void sortingCultureUsedReflectsResolvedCulture() throws Exception {
        try (Pipeline pipeline = build(apv(weightify("GB")), noValue());
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "fr_FR");
            data.process();
            assertEquals("fr-FR",
                data.get(ICountriesTranslationData.class)
                    .getSortingCultureUsed());
        }
    }

    @Test
    public void populationTranslatedNamesDifferFromGeographical()
        throws Exception {
        try (Pipeline pipeline =
                build(apv(weightify("DE")), apv(weightify("FR")));
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.accept-language", "fr_FR");
            data.process();
            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            String geo =
                values(ct.getCountryNamesGeographicalTranslated()).get(0);
            String pop =
                values(ct.getCountryNamesPopulationTranslated()).get(0);
            assertEquals("Allemagne", geo);
            assertEquals("France", pop);
            assertNotEquals(geo, pop);
            // The population All list carries the translated name, not a code.
            assertEquals("France",
                ct.getCountryNamesPopulationAllTranslated().getValue().get(0));
        }
    }

    // ===================================================================
    // Helpers
    // ===================================================================

    private static int frequency(List<String> list, String value) {
        int count = 0;
        for (String item : list) {
            if (item.equals(value)) {
                count++;
            }
        }
        return count;
    }

    private static void assertTailSorted(
        List<String> names, int from, String cultureTag) {
        Collator collator =
            Collator.getInstance(Locale.forLanguageTag(cultureTag));
        for (int i = from + 1; i < names.size(); i++) {
            assertTrue(
                "Tail not sorted at index " + i + ": '" +
                    names.get(i - 1) + "' then '" + names.get(i) + "'",
                collator.compare(names.get(i - 1), names.get(i)) <= 0);
        }
    }
}
