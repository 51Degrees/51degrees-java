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

package fiftyone.pipeline.core.flowelements;

/**
 * Implemented by a {@link FlowElement} that must be the last Flow Element of
 * a {@link Pipeline} and outside any parallel group, such as the usage
 * sharing element, which reads what every other Flow Element has added to
 * the Evidence. Building a Pipeline that puts one anywhere else fails with a
 * {@link fiftyone.pipeline.core.exceptions.PipelineConfigurationException}.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/usage-sharing-element.md#position-in-the-pipeline">Specification</a>
 */
public interface RunsLast {

    /**
     * The name the error gives the element when a Pipeline puts it anywhere
     * but last, such as "The usage sharing element".
     * @return the name of the element
     */
    String getRunsLastName();
}
