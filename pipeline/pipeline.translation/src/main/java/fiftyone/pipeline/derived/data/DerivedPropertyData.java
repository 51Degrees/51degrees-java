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

package fiftyone.pipeline.derived.data;

import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.TreeMap;

/**
 * The element data every Derived Property Element in a pipeline writes
 * into. The map is synchronized, because elements running in parallel may
 * write into it at the same time.
 */
public class DerivedPropertyData extends ElementDataBase
    implements IDerivedPropertyData {

    /**
     * Construct a new instance.
     * @param logger used for logging
     * @param flowData the flow data the element data is added to
     */
    public DerivedPropertyData(Logger logger, FlowData flowData) {
        super(logger, flowData, Collections.synchronizedMap(
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> AspectPropertyValue<T> getValue(String property, Class<T> type) {
        return (AspectPropertyValue<T>) get(property);
    }
}
