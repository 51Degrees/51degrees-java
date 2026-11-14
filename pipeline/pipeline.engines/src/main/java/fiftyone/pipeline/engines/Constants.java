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

package fiftyone.pipeline.engines;

import fiftyone.pipeline.engines.configuration.CacheConfiguration;

public class Constants {
    public static final String FIFTYONE_COOKIE_PREFIX = "51d_";
    public static final String DEFAULT_SESSION_COOKIE_NAME = "JSESSIONID";
    public static final int DATA_UPDATE_POLLING_DEFAULT = 30 * 60;
    public static final int DATA_UPDATE_RANDOMISATION_DEFAULT = 10 * 60;
    public static final boolean DEFAULT_AUTOUPDATE_ENABLED = true;
    public static final boolean DEFAULT_WATCHER_ENABLED = true;
    public static final boolean DEFAULT_DECOMPRESS = true;
    public static final boolean DEFAULT_VERIFY_MD5 = true;
    public static final boolean DEFAULT_VERIFY_IF_MODIFIED_SINCE = true;
    public static final boolean DEFAULT_UPDATE_ON_STARTUP = false;

    /**
     * Whether a pre-packaged Pipeline loads results lazily, unless its builder
     * is told otherwise.
     */
    public static final boolean DEFAULT_LAZY_LOADING = false;

    /**
     * How long a lazily loaded value is waited for when lazy loading is used
     * without a timeout.
     */
    public static final long DEFAULT_LAZY_LOADING_TIMEOUT_MILLIS = 1000;

    /**
     * Whether a pre-packaged Pipeline caches results, unless its builder is
     * told otherwise.
     */
    public static final boolean DEFAULT_RESULTS_CACHE = false;

    /**
     * How many results are cached when the results cache is used without a
     * size, which is the cache size the caching specification gives.
     */
    public static final int DEFAULT_RESULTS_CACHE_SIZE =
        CacheConfiguration.defaultSize;

    public enum PerformanceProfiles {
        LowMemory,
        MaxPerformance,
        HighPerformance,
        Balanced,
        BalancedTemp
    }

    public static class MissingPropertyMessages {
        public static final String PREFIX =
            "Property '%s' not found in data for element '%s'. ";
        public static final String DATA_UPGRADE_REQUIRED =
            "This is because your license and/or data file " +
                "does not include this property. The property is available " +
                "with the %s license/data for the %s";
        public static final String PROPERTY_EXCLUDED =
            "This is because the property has been excluded when configuring " +
            "the engine.";
        public static final String PRODUCT_NOT_IN_CLOUD_RESOURCE =
            "This is because your resource key does not include access to " +
            "any properties under '%s'. For more details on resource keys, " +
            "see our explainer: " +
            "https://51degrees.com/documentation/_services__cloud__resource_keys.html?utm_source=code&utm_medium=comment&utm_campaign=51degrees-java&utm_content=pipeline.engines-src-main-java-fiftyone-pipeline-engines-constants.java&utm_term=product_not_in_cloud_resource";
        public static final String PROPERTY_NOT_IN_CLOUD_RESOURCE =
            "This is because your resource key does not include access to " +
            "this property. Properties that are included for this key under " +
            "'%s' are %s. For more details on resource keys, see our " +
            "explainer: " +
            "https://51degrees.com/documentation/_services__cloud__resource_keys.html?utm_source=code&utm_medium=comment&utm_campaign=51degrees-java&utm_content=pipeline.engines-src-main-java-fiftyone-pipeline-engines-constants.java&utm_term=property_not_in_cloud_resource";
        public static final String UNKNOWN =
            "The reason for this is unknown. Please check that the aspect " +
            "and property name are correct.";
    }
}
