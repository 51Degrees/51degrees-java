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

/** Default settings and messages shared by the engines. */
public class Constants {
    /** Create a new instance. */
    public Constants() {
    }

    /** Prefix of the names of 51Degrees cookies. */
    public static final String FIFTYONE_COOKIE_PREFIX = "51d_";
    /** Default name of the session cookie. */
    public static final String DEFAULT_SESSION_COOKIE_NAME = "JSESSIONID";
    /** Default time between checks for a new data file, in seconds. */
    public static final int DATA_UPDATE_POLLING_DEFAULT = 30 * 60;
    /**
     * Default most seconds added at random to the time between checks, so that
     * many servers do not check at once.
     */
    public static final int DATA_UPDATE_RANDOMISATION_DEFAULT = 10 * 60;
    /** Whether automatic data file updates are on by default. */
    public static final boolean DEFAULT_AUTOUPDATE_ENABLED = true;
    /** Whether the data file is watched for changes by default. */
    public static final boolean DEFAULT_WATCHER_ENABLED = true;
    /** Whether a downloaded data file is decompressed by default. */
    public static final boolean DEFAULT_DECOMPRESS = true;
    /** Whether the MD5 hash of a downloaded data file is checked by default. */
    public static final boolean DEFAULT_VERIFY_MD5 = true;
    /**
     * Whether an update request sends the data file's time by default, so the
     * server can answer that nothing is new.
     */
    public static final boolean DEFAULT_VERIFY_IF_MODIFIED_SINCE = true;
    /** Whether the data file is updated when the engine starts by default. */
    public static final boolean DEFAULT_UPDATE_ON_STARTUP = false;

    /**
     * Deprecated. The default of the lazy loading option, which has no
     * effect.
     */
    public static final boolean DEFAULT_LAZY_LOADING = false;

    /**
     * Deprecated. The default timeout of the lazy loading option, which has
     * no effect.
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

    /** How an on-premise engine trades memory for speed. */
    public enum PerformanceProfiles {
        /**
         * Reads the data file as needed with no caching, using the least
         * memory.
         */
        LowMemory,
        /** Holds the whole data file in memory for the fastest detection. */
        MaxPerformance,
        /** Holds the data file in memory. */
        HighPerformance,
        /** Holds some data in memory and caches the rest as it is read. */
        Balanced,
        /**
         * As Balanced, reading from a temporary copy so the data file is not
         * locked.
         */
        BalancedTemp
    }

    /** Messages that explain why a property has no value. */
    public static class MissingPropertyMessages {
        /** Create a new instance. */
        public MissingPropertyMessages() {
        }

        /** Start of every message, naming the property and element. */
        public static final String PREFIX =
            "Property '%s' not found in data for element '%s'. ";
        /** The property needs a higher data tier, which the message names. */
        public static final String DATA_UPGRADE_REQUIRED =
            "This is because your license or data file does not include " +
            "this property. It is available with the %s license/data for " +
            "the %s. To upgrade, see " +
            "https://51degrees.com/pricing?utm_source=code&utm_medium=comment&utm_campaign=51degrees-java&utm_content=pipeline.engines-src-main-java-fiftyone-pipeline-engines-constants.java&utm_term=data_upgrade_required.";
        /**
         * The upgrade message where the data tiers the property is present
         * in are not known.
         */
        public static final String DATA_UPGRADE_REQUIRED_TIERS_NOT_KNOWN =
            "This is because your license or data file does not include " +
            "this property. To see which licenses include it, see " +
            "https://51degrees.com/pricing?utm_source=code&utm_medium=comment&utm_campaign=51degrees-java&utm_content=pipeline.engines-src-main-java-fiftyone-pipeline-engines-constants.java&utm_term=data_upgrade_required_tiers_not_known.";
        /** The property was excluded when the engine was configured. */
        public static final String PROPERTY_EXCLUDED =
            "This is because the property has been excluded when configuring " +
            "the engine.";
        /** The resource key gives no properties of the product named. */
        public static final String PRODUCT_NOT_IN_CLOUD_RESOURCE =
            "This is because your resource key does not include access to " +
            "any properties under '%s'. Check or create a resource key with " +
            "the properties you need in the configurator at " +
            "https://configure.51degrees.com?utm_source=code&utm_medium=comment&utm_campaign=51degrees-java&utm_content=pipeline.engines-src-main-java-fiftyone-pipeline-engines-constants.java&utm_term=product_not_in_cloud_resource.";
        /**
         * The resource key does not give the property, and the message lists
         * those it does give.
         */
        public static final String PROPERTY_NOT_IN_CLOUD_RESOURCE =
            "This is because your resource key does not include access to " +
            "this property. The properties included for your key under " +
            "'%s' are %s. Check or change your key in the configurator at " +
            "https://configure.51degrees.com?utm_source=code&utm_medium=comment&utm_campaign=51degrees-java&utm_content=pipeline.engines-src-main-java-fiftyone-pipeline-engines-constants.java&utm_term=property_not_in_cloud_resource.";
        /**
         * The separator of the names in a list in a message.
         */
        public static final String LIST_SEPARATOR = ",";
        /** A cloud request failed, so there was no data. */
        public static final String CLOUD_REQUEST_FAILED =
            "This is because an upstream cloud request failed, so no data " +
            "was available to populate the property.";
        /**
         * The Data Key a CloudRequestFailed message names when no engine
         * populates the Element Data.
         */
        public static final String UNKNOWN_DATA_KEY = "Unknown";
        /** No other reason fits. */
        public static final String UNKNOWN =
            "The reason for this is unknown. Please check that the aspect " +
            "and property name are correct.";
    }
}
