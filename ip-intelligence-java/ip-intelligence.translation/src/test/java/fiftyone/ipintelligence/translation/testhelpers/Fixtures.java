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

package fiftyone.ipintelligence.translation.testhelpers;

import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.ipintelligence.translation.flowelements.CountriesTranslationEngine;
import fiftyone.ipintelligence.translation.flowelements.CountriesTranslationEngineBuilder;
import fiftyone.ipintelligence.translation.flowelements.CountryCodeTranslationEngine;
import fiftyone.ipintelligence.translation.flowelements.CountryCodeTranslationEngineBuilder;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WeightedValue;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Helpers for building country translation pipelines in tests, with a stub IP
 * Intelligence engine producing the weighted country codes.
 */
public final class Fixtures {

    private static final ILoggerFactory LOGGER_FACTORY =
        LoggerFactory.getILoggerFactory();

    private Fixtures() {
    }

    /**
     * Build a weighted list of codes with descending weights assigned by input
     * order, so the first code is the most probable.
     * @param codes the codes, most probable first
     * @return the weighted list
     */
    public static List<IWeightedValue<String>> weightify(String... codes) {
        List<IWeightedValue<String>> list = new ArrayList<>();
        for (int i = 0; i < codes.length; i++) {
            list.add(new WeightedValue<String>(
                (codes.length - i) * 1000, codes[i]));
        }
        return list;
    }

    /**
     * Build a single weighted code with an explicit weight.
     * @param rawWeight the raw weight (0..65535)
     * @param code the code
     * @return the weighted value
     */
    public static IWeightedValue<String> weighted(int rawWeight, String code) {
        return new WeightedValue<String>(rawWeight, code);
    }

    /**
     * Wrap a weighted list in an {@link AspectPropertyValue} with a value.
     * @param list the weighted list
     * @return the wrapped value
     */
    public static AspectPropertyValue<List<IWeightedValue<String>>> apv(
        List<IWeightedValue<String>> list) {
        return new AspectPropertyValueDefault<List<IWeightedValue<String>>>(
            list);
    }

    /**
     * A no-value {@link AspectPropertyValue}, used when the IP engine produced
     * no weighted codes for a dimension.
     * @return a no-value wrapper
     */
    public static AspectPropertyValue<List<IWeightedValue<String>>> noValue() {
        return new AspectPropertyValueDefault<List<IWeightedValue<String>>>();
    }

    /**
     * Build a pipeline of: stub IP engine -> country code translation engine
     * -> countries translation engine.
     * @param geographical the geographical weighted codes
     * @param population the population weighted codes
     * @return the pipeline
     * @throws Exception if the pipeline cannot be built
     */
    @SuppressWarnings("unchecked")
    public static Pipeline build(
        AspectPropertyValue<List<IWeightedValue<String>>> geographical,
        AspectPropertyValue<List<IWeightedValue<String>>> population)
        throws Exception {
        final Logger logger = LOGGER_FACTORY.getLogger("StubIpEngine");
        final AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData>
            mockEngine = mock(AspectEngine.class);
        ElementDataFactory<IPIntelligenceData> ipFactory =
            (flowData, flowElement) ->
                new TestIpiData(logger, flowData, mockEngine, null);

        StubIpEngine stub =
            new StubIpEngine(logger, ipFactory, geographical, population);
        CountryCodeTranslationEngine codeEngine =
            new CountryCodeTranslationEngineBuilder(LOGGER_FACTORY).build();
        CountriesTranslationEngine countriesEngine =
            new CountriesTranslationEngineBuilder(LOGGER_FACTORY).build();

        return new PipelineBuilder(LOGGER_FACTORY)
            .addFlowElement(stub)
            .addFlowElement(codeEngine)
            .addFlowElement(countriesEngine)
            .build();
    }

    /**
     * Build a pipeline as {@link #build} but with a countries translation
     * engine using custom country name sources and a custom set of all known
     * countries. Used to exercise the missing-single-name behaviour with a
     * deliberately incomplete locale map.
     * @param geographical the geographical weighted codes
     * @param population the population weighted codes
     * @param countriesSources custom country name sources, keyed on file name
     * @param allCountries custom ordered (code, English name) list
     * @return the pipeline
     * @throws Exception if the pipeline cannot be built
     */
    @SuppressWarnings("unchecked")
    public static Pipeline buildCustom(
        AspectPropertyValue<List<IWeightedValue<String>>> geographical,
        AspectPropertyValue<List<IWeightedValue<String>>> population,
        Map<String, String> countriesSources,
        List<Map.Entry<String, String>> allCountries)
        throws Exception {
        final Logger logger = LOGGER_FACTORY.getLogger("StubIpEngine");
        final AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData>
            mockEngine = mock(AspectEngine.class);
        ElementDataFactory<IPIntelligenceData> ipFactory =
            (flowData, flowElement) ->
                new TestIpiData(logger, flowData, mockEngine, null);

        StubIpEngine stub =
            new StubIpEngine(logger, ipFactory, geographical, population);
        CountryCodeTranslationEngine codeEngine =
            new CountryCodeTranslationEngineBuilder(LOGGER_FACTORY).build();
        CountriesTranslationEngine countriesEngine =
            new CountriesTranslationEngine(
                LOGGER_FACTORY.getLogger("CountriesTranslationEngine"),
                countriesSources,
                allCountries,
                (flowData, flowElement) -> new fiftyone.ipintelligence
                    .translation.data.CountriesTranslationData(
                        LOGGER_FACTORY.getLogger("CountriesTranslationData"),
                        flowData));

        return new PipelineBuilder(LOGGER_FACTORY)
            .addFlowElement(stub)
            .addFlowElement(codeEngine)
            .addFlowElement(countriesEngine)
            .build();
    }
}
