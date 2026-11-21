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

package fiftyone.pipeline.engines.configuration;

import java.util.concurrent.ExecutorService;

/**
 * Deprecated, and has no effect. Lazy loading is kept as a public name so
 * that existing code and configuration still build and run, but every Engine
 * processes on the thread that processes the Flow Data, whatever is set
 * here. Long-running work is a lazy property of an Engine's own Element Data
 * instead.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/advanced-features/lazy-loading.md">Specification</a>
 */
public class LazyLoadingConfiguration {
    private final int propertyTimeoutMillis;

    private final ExecutorServiceFactory factory;

    /**
     * Construct a new instance using the default {@link ExecutorServiceFactory}.
     * @param propertyTimeoutMillis kept, but not used
     */
    public LazyLoadingConfiguration(int propertyTimeoutMillis) {
        this(propertyTimeoutMillis, new ExecutorServiceFactoryDefault());
    }

    /**
     * Construct a new instance.
     * @param propertyTimeoutMillis kept, but not used
     * @param factory kept, but no Engine asks it for an executor
     */
    public LazyLoadingConfiguration(
        int propertyTimeoutMillis,
        ExecutorServiceFactory factory) {
        this.propertyTimeoutMillis = propertyTimeoutMillis;
        this.factory = factory;
    }

    /**
     * Get the timeout that was set, which is not used.
     * @return timeout in milliseconds
     */
    public int getPropertyTimeoutMillis() {
        return propertyTimeoutMillis;
    }

    /**
     * Create an {@link ExecutorService} from the factory that was set. No
     * Engine calls this, as lazy loading has no effect.
     * @return new {@link ExecutorService}
     */
    public ExecutorService getExecutorService() {
        return factory.create();
    }
}
