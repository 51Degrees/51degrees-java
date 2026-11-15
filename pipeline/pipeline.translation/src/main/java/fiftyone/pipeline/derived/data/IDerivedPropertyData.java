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

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;

/**
 * The element data of the Derived Property Element, under the key
 * {@code derived}, holding one value per script keyed by the output
 * property name. A value whose source properties were not all available
 * has no value, and its no-value message names every one that was missing.
 */
public interface IDerivedPropertyData extends ElementData {

    /**
     * The value a script produced.
     * @param property the output property name
     * @param type the type of the value
     * @param <T> the type of the value
     * @return the value, which may have no value, or null if no script in
     * the pipeline produces the property
     */
    <T> AspectPropertyValue<T> getValue(String property, Class<T> type);
}
