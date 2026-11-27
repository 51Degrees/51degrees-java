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

package fiftyone.pipeline.engines.services;

import fiftyone.pipeline.engines.flowelements.AspectEngine;

/**
 * Enumeration of reasons why a property may be missing from the results of an
 * {@link AspectEngine}'s processing.
 */
public enum MissingPropertyReason {
    /**
     * The data file does not hold the property, so a higher data tier is
     * needed.
     */
    DataFileUpgradeRequired,
    /** The property was excluded when the engine was configured. */
    PropertyExcludedFromEngineConfiguration,
    /** The resource key does not give the property. */
    PropertyNotAccessibleWithResourceKey,
    /** The resource key gives no properties of the product. */
    ProductNotAccessibleWithResourceKey,
    /**
     * A request to the cloud service failed, so no data was available to
     * populate the property.
     */
    CloudRequestFailed,
    /** No other reason fits. */
    Unknown
}
