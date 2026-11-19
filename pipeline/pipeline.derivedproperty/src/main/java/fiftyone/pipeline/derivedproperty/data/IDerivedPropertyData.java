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

package fiftyone.pipeline.derivedproperty.data;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;

/**
 * The Element Data every Derived Property Element writes under the key
 * {@code derived}. It holds one {@link AspectPropertyValue} per script,
 * keyed by the script's output property name, matched without regard to
 * case.
 */
public interface IDerivedPropertyData extends ElementData {

    /**
     * Get a derived property's value. A value that has no value carries a
     * message naming every source property that could not be read, and
     * {@link AspectPropertyValue#getValue()} then throws with that message.
     * @param propertyName the output property name of a script
     * @param valueType the type the script gives, being {@link String},
     *                  {@link Boolean}, {@link Integer} or {@link Double}
     * @param <T> the value type
     * @return the value
     * @throws fiftyone.pipeline.engines.exceptions.PropertyMissingException
     * where no script produces the property
     * @throws ClassCastException where the value is not of the type given
     */
    <T> AspectPropertyValue<T> getAs(String propertyName, Class<T> valueType);
}
