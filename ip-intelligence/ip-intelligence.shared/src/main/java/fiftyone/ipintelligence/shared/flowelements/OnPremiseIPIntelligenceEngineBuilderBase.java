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

package fiftyone.ipintelligence.shared.flowelements;

import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.fiftyone.flowelements.FiftyOneAspectEngine;
import fiftyone.pipeline.engines.fiftyone.flowelements.FiftyOneOnPremiseAspectEngineBuilderBase;
import fiftyone.pipeline.engines.services.DataUpdateService;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Base builder class for 51Degrees on-premise IP Intelligence engines.
 * @param <TBuilder> the specific builder type to use as the return type from
 *                  the fluent builder methods
 * @param <TEngine> the type of the engine that this builder will build
 */
public abstract class OnPremiseIPIntelligenceEngineBuilderBase<
    TBuilder extends OnPremiseIPIntelligenceEngineBuilderBase<TBuilder, TEngine>,
    TEngine extends FiftyOneAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData>>
    extends FiftyOneOnPremiseAspectEngineBuilderBase<TBuilder, TEngine> {

    /**
     * Default constructor which uses the {@link ILoggerFactory} implementation
     * returned by {@link LoggerFactory#getILoggerFactory()}.
     */
    public OnPremiseIPIntelligenceEngineBuilderBase() {
        super();
    }

    /**
     * Construct a new instance using the {@link ILoggerFactory} supplied.
     * @param loggerFactory the logger factory to use
     */
    public OnPremiseIPIntelligenceEngineBuilderBase(
        ILoggerFactory loggerFactory) {
        super(loggerFactory);
    }

    /**
     * Construct a new instance using the {@link ILoggerFactory} and
     * {@link DataUpdateService} supplied.
     * @param loggerFactory the logger factory to use
     * @param dataUpdateService the {@link DataUpdateService} to use when
     *                          automatic updates happen on the data file
     */
    public OnPremiseIPIntelligenceEngineBuilderBase(
        ILoggerFactory loggerFactory,
        DataUpdateService dataUpdateService) {
        super(loggerFactory, dataUpdateService);
    }
}