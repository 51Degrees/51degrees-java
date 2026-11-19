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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * The defaults of a pre-packaged Pipeline builder come from {@link Constants}
 * and keep the values they have always had.
 */
public class PrePackagedPipelineBuilderBaseTests {

    private static class TestBuilder
        extends PrePackagedPipelineBuilderBase<TestBuilder> {
    }

    @Test
    public void Defaults_FromConstants() {
        TestBuilder builder = new TestBuilder();
        assertEquals(Constants.DEFAULT_LAZY_LOADING, builder.lazyLoading);
        assertEquals(
            Constants.DEFAULT_LAZY_LOADING_TIMEOUT_MILLIS,
            builder.lazyLoadingTimeoutMillis);
        assertEquals(Constants.DEFAULT_RESULTS_CACHE, builder.resultsCache);
        assertEquals(
            Constants.DEFAULT_RESULTS_CACHE_SIZE,
            builder.resultsCacheSize);
    }

    @Test
    public void Defaults_Values() {
        assertFalse(Constants.DEFAULT_LAZY_LOADING);
        assertEquals(1000, Constants.DEFAULT_LAZY_LOADING_TIMEOUT_MILLIS);
        assertFalse(Constants.DEFAULT_RESULTS_CACHE);
        assertEquals(1000, Constants.DEFAULT_RESULTS_CACHE_SIZE);
        assertEquals(
            CacheConfiguration.defaultSize,
            Constants.DEFAULT_RESULTS_CACHE_SIZE);
    }

    @Test
    public void UseWithoutSize_KeepsDefaults() {
        TestBuilder builder = new TestBuilder()
            .useLazyLoading()
            .useResultsCache();
        assertEquals(
            Constants.DEFAULT_LAZY_LOADING_TIMEOUT_MILLIS,
            builder.lazyLoadingTimeoutMillis);
        assertEquals(
            Constants.DEFAULT_RESULTS_CACHE_SIZE,
            builder.resultsCacheSize);
    }
}
