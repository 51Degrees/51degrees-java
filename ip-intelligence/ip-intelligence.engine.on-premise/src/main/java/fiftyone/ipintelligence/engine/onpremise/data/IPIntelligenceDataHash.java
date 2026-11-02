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

package fiftyone.ipintelligence.engine.onpremise.data;

import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;

import java.util.List;

/**
 * On-premise specific interface for {@link IPIntelligenceData}.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/ip-intelligence-specification/pipeline-elements/ip-intelligence-on-premise.md#element-data">Specification</a>
 */
public interface IPIntelligenceDataHash extends IPIntelligenceData {

    /**
     * Get the values for the specified property as a List&lt;&gt;.
     * @param propertyName name of the property to get values for
     * @param parametrizedTypes types parametrizing {@link AspectPropertyValue} starting with {@link List}
     * @return values as a list
     */
    AspectPropertyValue<List<?>> getValues(
            String propertyName,
            Class<?>[] parametrizedTypes);
}