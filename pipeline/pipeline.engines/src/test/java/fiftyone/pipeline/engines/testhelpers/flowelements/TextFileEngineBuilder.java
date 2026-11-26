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

package fiftyone.pipeline.engines.testhelpers.flowelements;

import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.SingleFileAspectEngineBuilderBase;
import fiftyone.pipeline.engines.services.DataUpdateService;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import fiftyone.pipeline.engines.testhelpers.data.EmptyEngineData;
import org.slf4j.ILoggerFactory;

import java.util.List;

/**
 * Builds a {@link TextFileEngine} from one data file.
 */
public class TextFileEngineBuilder extends SingleFileAspectEngineBuilderBase<
    TextFileEngineBuilder,
    TextFileEngine> {

    @Override
    protected TextFileEngineBuilder self() {
        return this;
    }

    public TextFileEngineBuilder(
        ILoggerFactory loggerFactory,
        DataUpdateService dataUpdateService) {
        super(loggerFactory, dataUpdateService);
    }

    @Override
    protected TextFileEngine newEngine(List<String> properties)
        throws Exception {
        DataFileConfiguration config = dataFileConfigs.get(0);
        return new TextFileEngine(
            config.getDataFilePath(),
            loggerFactory.getLogger(TextFileEngine.class.getName()),
            (flowData, element) -> new EmptyEngineData(
                loggerFactory.getLogger(EmptyEngineData.class.getName()),
                flowData,
                (AspectEngine<? extends AspectData,
                    ? extends AspectPropertyMetaData>) element,
                MissingPropertyServiceDefault.getInstance()),
            tempDir);
    }

    @Override
    public TextFileEngineBuilder setPerformanceProfile(
        Constants.PerformanceProfiles profile) {
        return this;
    }
}
