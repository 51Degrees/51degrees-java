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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import org.slf4j.ILoggerFactory;

/**
 * Builds a {@link HumanConfidenceElement}, which gives the HumanConfidence
 * and HumanConfidenceSource Properties from the Cloud Request Engine it is
 * given.
 */
public class HumanConfidenceElementBuilder {

    private final ILoggerFactory loggerFactory;

    private CloudRequestEngine requestEngine;

    /**
     * @param loggerFactory makes the element's logger
     */
    public HumanConfidenceElementBuilder(ILoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
    }

    /**
     * Sets the Cloud Request Engine whose JSON call the element reads. It
     * comes before the element in the Pipeline.
     * @param requestEngine the Cloud Request Engine
     * @return this builder
     */
    public HumanConfidenceElementBuilder setRequestEngine(
        CloudRequestEngine requestEngine) {
        this.requestEngine = requestEngine;
        return this;
    }

    /**
     * @return a new element
     */
    public HumanConfidenceElement build() {
        return new HumanConfidenceElement(
            loggerFactory.getLogger(HumanConfidenceElement.class.getName()),
            requestEngine);
    }
}
