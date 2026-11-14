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

package fiftyone.pipeline.web;

import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElement;
import fiftyone.pipeline.engines.fiftyone.flowelements.SetHeadersElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;

/**
 * Static methods used on server startup to configure and build the Pipeline and
 * services needed.
 */
public class StartupHelpers {

    /**
     * Get the index in the {@link PipelineOptions#elements} of the element with
     * the name required.
     * @param options the options to search
     * @param name the name to search for
     * @return the zero based index or -1 if not found
     */
    private static int getElementIndex(
        PipelineOptions options,
        String name) {
        int index = 0;
        for (ElementOptions element : options.elements) {
            if (element.builderName.toLowerCase().contains(name.toLowerCase())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Configure the extra web elements required and build the Pipeline using
     * the {@link PipelineBuilder#buildFromConfiguration(PipelineOptions)}
     * method.
     * @param builder to build the Pipeline
     * @param options to build the Pipeline with
     * @param clientSideEvidenceEnabled true if client-side evidence is enabled
     *                                  in the configuration. This will add JSON
     *                                  and JavaScript elements to the Pipeline
     * @return new {@link Pipeline} instance
     * @throws Exception if there was an error building the Pipeline
     */
    public static Pipeline buildFromConfiguration(
        PipelineBuilder builder,
        PipelineOptions options,
        boolean clientSideEvidenceEnabled) throws Exception {

        return StartupHelpers.buildFromConfiguration(builder, options, 
            clientSideEvidenceEnabled, null);
    }

    /**
     * Configure the extra web elements required and build the Pipeline using
     * the {@link PipelineBuilder#buildFromConfiguration(PipelineOptions)}
     * method.
     * @param builder to build the Pipeline
     * @param options to build the Pipeline with
     * @param clientSideEvidenceEnabled true if client-side evidence is enabled
     *                                  in the configuration. This will add JSON
     *                                  and JavaScript elements to the Pipeline
     * @param contextRoot The context-root setting from the web.xml. This is
     *                    needed in order to create the correct callback URL
     *                    for the JavascriptBuilder.
     * @return new {@link Pipeline} instance
     * @throws Exception if there was an error building the Pipeline
     */
    public static Pipeline buildFromConfiguration(
        PipelineBuilder builder,
        PipelineOptions options,
        boolean clientSideEvidenceEnabled,
        String contextRoot) throws Exception {

        return StartupHelpers.buildFromConfiguration(builder, options,
            clientSideEvidenceEnabled, contextRoot, Constants.DEFAULT_JSON_PATH,
            Constants.DEFAULT_ELEMENT_ENABLED,
            Constants.DEFAULT_ELEMENT_ENABLED);
    }

    /**
     * Configure the extra web elements required and build the Pipeline using
     * the {@link PipelineBuilder#buildFromConfiguration(PipelineOptions)}
     * method.
     * @param builder to build the Pipeline
     * @param options to build the Pipeline with
     * @param clientSideEvidenceEnabled true if client-side evidence is enabled
     *                                  in the configuration. This will add JSON
     *                                  and JavaScript elements to the Pipeline
     * @param contextRoot The context-root setting from the web.xml. This is
     *                    needed in order to create the correct callback URL
     *                    for the JavascriptBuilder.
     * @param jsonPath the path the JSON is served from, which the
     *                 JavascriptBuilder calls back to
     * @param sequenceElementEnabled true to add the Sequence Element when the
     *                               configuration does not include one
     * @param setHeadersElementEnabled true to add the Set Headers Element
     *                                 when the configuration does not
     *                                 include one
     * @return new {@link Pipeline} instance
     * @throws Exception if there was an error building the Pipeline
     */
    public static Pipeline buildFromConfiguration(
        PipelineBuilder builder,
        PipelineOptions options,
        boolean clientSideEvidenceEnabled,
        String contextRoot,
        String jsonPath,
        boolean sequenceElementEnabled,
        boolean setHeadersElementEnabled) throws Exception {

        if (options == null ||
            options.elements == null ||
            options.elements.size() == 0) {
            throw new PipelineConfigurationException(
                    "Could not find pipeline configuration information");
        }

        // A caller that has no path set gives the default, so that the
        // JavaScript still calls back to somewhere the JSON is served from.
        String path = jsonPath == null || jsonPath.isEmpty() ?
            Constants.DEFAULT_JSON_PATH : jsonPath;
        String endpoint = "/" + (path.startsWith("/") ?
            path.substring(1) : path);

        // Add the sequence element.
        if (sequenceElementEnabled &&
            getElementIndex(
            options,
            SequenceElement.class.getSimpleName()) == -1) {
            // The sequence element is not included so add it.
            ElementOptions sequenceElement = new ElementOptions();
            sequenceElement.builderName = SequenceElement.class.getSimpleName();
            options.elements.add(sequenceElement);
        }

        // TODO - client size option should ideally be added to the same configuration.
        if (clientSideEvidenceEnabled) {
            int jsIndex = getElementIndex(
                options,
                JavaScriptBuilderElement.class.getSimpleName());

            // Client-side evidence is enabled so make sure the
            // JsonBuilderElement and JavaScriptBuilderElement has been
            // included.
            if (getElementIndex(
                options,
                JsonBuilderElement.class.getSimpleName()) == -1) {
                // The json builder element is not included so add it.
                ElementOptions jsonBuilderElement = new ElementOptions();
                jsonBuilderElement.builderName = JsonBuilderElement.class.getSimpleName();
                if (jsIndex >= 0) {
                    // There is already a javascript builder element
                    // so insert the json builder before it.
                    options.elements.add(jsIndex + 1, jsonBuilderElement);
                    jsIndex++;
                }
                else {
                    options.elements.add(jsonBuilderElement);
                }
            }

            if (jsIndex == -1) {
                // The js builder element is not included so add it.
                ElementOptions javaScriptBuilderElement = new ElementOptions();
                javaScriptBuilderElement.builderName = JavaScriptBuilderElement.class.getSimpleName();
                javaScriptBuilderElement.buildParameters.put("Endpoint", endpoint);
                if(contextRoot != null) {
                    javaScriptBuilderElement.buildParameters.put("ContextRoot", contextRoot);
                }
                options.elements.add(javaScriptBuilderElement);
            }
            else {
                // There is already a JavaScript builder config so check if
                // the endpoint is specified. If not, add it.
                if (options.elements.get(jsIndex).buildParameters.containsKey("Endpoint") == false) {
                    options.elements.get(jsIndex).buildParameters.put("Endpoint", endpoint);
                }
                // Same for context root.
                if(options.elements.get(jsIndex).buildParameters.containsKey("ContextRoot") == false &&
                    contextRoot != null) {
                        options.elements.get(jsIndex).buildParameters.put("ContextRoot", contextRoot);
                }
            }

        }

        // The JSON Builder Element fails on every request without a
        // sequence number, which only the Sequence Element supplies.
        if (getElementIndex(
            options,
            JsonBuilderElement.class.getSimpleName()) != -1 &&
            getElementIndex(
                options,
                SequenceElement.class.getSimpleName()) == -1) {
            throw new PipelineConfigurationException(
                "The Pipeline has a JSON Builder Element but no Sequence " +
                    "Element, and the JSON Builder Element needs the " +
                    "sequence number the Sequence Element supplies. Turn " +
                    "sequence-element-enabled back on, add a Sequence " +
                    "Element to the configuration, or turn off " +
                    "client-side properties.");
        }

        // Add the set-headers element.
        if (setHeadersElementEnabled &&
            getElementIndex(
            options,
            SetHeadersElement.class.getSimpleName()) == -1) {
            // The set-headers element is not included so add it.
            ElementOptions setHeadersElement = new ElementOptions();
            setHeadersElement.builderName = SetHeadersElement.class.getSimpleName();
            options.elements.add(setHeadersElement);
        }
        
        return builder.buildFromConfiguration(options);
    }
}
