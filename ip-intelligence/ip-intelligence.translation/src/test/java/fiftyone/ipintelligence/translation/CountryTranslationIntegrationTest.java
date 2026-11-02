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

import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngineBuilder;
import fiftyone.ipintelligence.translation.data.ICountriesTranslationData;
import fiftyone.ipintelligence.translation.flowelements.CountriesTranslationEngineBuilder;
import fiftyone.ipintelligence.translation.flowelements.CountryCodeTranslationEngineBuilder;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.Constants;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * End-to-end test that drives the real on-premise IP Intelligence engine with
 * a data file, then the two country translation engines, and checks that a
 * complete, localized, ordered country dropdown is produced.
 * <p>
 * The data file is resolved from the {@code TestDataFile} system property, the
 * {@code 51DEGREES_IPI_PATH} environment variable, or a few well known
 * locations, and the test is skipped if none is found.
 * <p>
 * The IP Intelligence element data key has differed between engine versions
 * (it is {@code "ip"} in current builds), so the test reads the actual key
 * from the engine and configures the code translation engine with it. The
 * country names engine reads the codes by {@code IPIntelligenceData} type, so
 * it is unaffected.
 */
public class CountryTranslationIntegrationTest {

    private static final String SAMPLE_IP = "8.8.8.8";

    private static String resolveDataFile() {
        String[] candidates = {
            System.getProperty("TestDataFile"),
            System.getenv("51DEGREES_IPI_PATH"),
            "51Degrees-IPIV4EnterpriseIpiV41.ipi",
            "../51Degrees-IPIV4EnterpriseIpiV41.ipi",
            "../../51Degrees-IPIV4EnterpriseIpiV41.ipi",
            "D:/WorkSpace/51Degrees-IPIV4EnterpriseIpiV41.ipi"
        };
        for (String candidate : candidates) {
            if (candidate != null && new File(candidate).exists()) {
                return candidate;
            }
        }
        return null;
    }

    @Test
    public void translatesRealIpThroughOnPremiseEngine() throws Exception {
        String dataFile = resolveDataFile();
        assumeTrue(
            "Skipping integration test: no IP Intelligence data file found",
            dataFile != null);

        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        PrintStream out = System.out;

        IPIntelligenceOnPremiseEngine ipEngine =
            new IPIntelligenceOnPremiseEngineBuilder(loggerFactory, null)
                .setPerformanceProfile(Constants.PerformanceProfiles.LowMemory)
                .setAutoUpdate(false)
                .build(dataFile, false);
        String ipKey = ipEngine.getElementDataKey();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(ipEngine)
                .addFlowElement(
                    new CountryCodeTranslationEngineBuilder(loggerFactory)
                        .setSourceElementDataKey(ipKey)
                        .build())
                .addFlowElement(
                    new CountriesTranslationEngineBuilder(loggerFactory)
                        .build())
                .build();
             FlowData data = pipeline.createFlowData()) {

            data.addEvidence("query.client-ip", SAMPLE_IP);
            data.addEvidence("header.accept-language", "fr_FR");
            data.process();

            ICountriesTranslationData ct =
                data.get(ICountriesTranslationData.class);
            List<String> codes =
                ct.getCountryCodesGeographicalAll().getValue();
            List<String> names =
                ct.getCountryNamesGeographicalAllTranslated().getValue();
            List<IWeightedValue<String>> weighted =
                ct.getCountryNamesGeographicalTranslated().getValue();

            // Print the real result so the run is self-documenting.
            out.println("Integration test data file: " + dataFile);
            out.println("IP engine element data key: " + ipKey);
            out.println("Client IP: " + SAMPLE_IP + ", language: fr_FR");
            out.println("Weighted geographical countries (most probable first):");
            for (IWeightedValue<String> item : weighted) {
                out.println("  " + item.getValue() +
                    " (weight " + item.getRawWeighting() + ")");
            }
            out.println("Dropdown (first 5 of " + names.size() + "):");
            for (int i = 0; i < Math.min(5, names.size()); i++) {
                out.println("  option value=" + codes.get(i) +
                    " text=" + names.get(i));
            }

            // A complete, index-aligned, localized dropdown.
            assertEquals(250, codes.size());
            assertEquals(codes.size(), names.size());
            assertEquals("fr-FR", ct.getSortingCultureUsed());
            // Localization works through the real engine: a tail country's
            // code is aligned with its French name.
            assertEquals("Allemagne", names.get(codes.indexOf("DE")));

            // The sample IP resolves to weighted countries, which lead the
            // dropdown ordered by weight descending.
            assertTrue("Expected the sample IP to resolve to a country",
                weighted.isEmpty() == false);
            assertEquals(weighted.get(0).getValue(), names.get(0));
            assertTrue(weighted.get(0).getRawWeighting() > 0);
        }
    }
}
