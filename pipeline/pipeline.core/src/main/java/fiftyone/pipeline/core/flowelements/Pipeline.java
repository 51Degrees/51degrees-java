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

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.services.PipelineService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The Pipeline groups together {@link FlowElement}s and is a factory for
 * {@link FlowData} which serves as the evidence, results, and a means to access
 * the Pipeline's processing capabilities.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#pipeline">Specification</a>
 */
public interface Pipeline extends AutoCloseable {

    /**
     * Close the Pipeline and, if it was built to, its Flow Elements and the
     * services added to it. It throws no checked exception, so a
     * try-with-resources block needs no catch for it.
     * @throws fiftyone.pipeline.core.exceptions.CloseException if closing a
     * Flow Element or service threw. Every Flow Element and service is
     * closed first, and the first failure is the cause, with any others
     * suppressed in it.
     */
    @Override
    void close();

    /**
     * Create a new {@link FlowData} instance linked to this Pipeline. The
     * resulting FlowData's {@link FlowData#process()} method will cause
     * the FlowData to be processed.
     * @return a new {@link FlowData} linked to this Pipeline
     */
    FlowData createFlowData();

    /**
     * Get the evidence keys used by all the {@link FlowElement}s in the
     * Pipeline.
     * @return merged list of evidence keys
     */
    EvidenceKeyFilter getEvidenceKeyFilter();

    /**
     * True if any of the {@link FlowElement}s in this pipeline will create
     * multiple threads and execute in parallel. False otherwise.
     * @return true if any {@link FlowElement}s are concurrent
     */
    boolean isConcurrent();

    /**
     * True if exceptions that happen while processing are recorded in
     * {@link FlowData#getErrors()} and logged, rather than thrown once
     * processing is complete. A wrapper of the Pipeline, such as the web
     * integration, can read it to decide whether to suppress and log an
     * exception of its own. See the
     * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/exception-handling.md#L42">exception handling specification</a>.
     * <p>
     * The default gives the builder's default, so that an implementation
     * written before this method was added still compiles. Override it to
     * give the real setting.
     * @return true if exceptions during processing are suppressed
     */
    default boolean isSuppressProcessExceptions() {
        return fiftyone.pipeline.core.Constants
            .DEFAULT_SUPPRESS_PROCESS_EXCEPTIONS;
    }

    /**
     * Indicates whether the Pipeline has been closed using the {@link #close()}
     * method, either explicitly or as a result of a 'try with resource'.
     * @return true if the Pipeline has been closed
     */
    boolean isClosed();
    
    /**
     * Add a service to be used by this Pipeline. If the service supports 
     * Closeable or AutoCloseable, it will be closed automatically when the
     * pipeline is closed.
     * @param service the service to add
     */
    void addService(PipelineService service);
    
    /**
     * Add a collection of services. If the services support Closeable or
     * AutoCloseable, they will be closed automatically when the pipeline is
     * closed.
     * @param services to be added
     * @return true if successfully added
     */
    boolean addServices(Collection<PipelineService> services);
    
    /**
     * Returns the an unmodified list of services that this Pipeline is
     * monitoring
     * @return immutable list of pipeline services
     */
    List<PipelineService> getServices();

    /**
     * Get the specified element from the pipeline. If the pipeline contains
     * multiple elements of the requested type, this method will return null.
     * @param type the type of the {@link FlowElement} to get
     * @param <T> the type of the {@link FlowElement} to get
     * @return An instance of the specified {@link FlowElement} if the pipeline
     * contains one. Null is returned if there is no such instance or there are
     * multiple instances of that type.
     */
    <T extends FlowElement<?, ?>> T getElement(Class<T> type);

    /**
     * Get list of the flow elements that are part of this pipeline.
     * @return an immutable list of the flow elements
     */
    List<FlowElement<?, ?>> getFlowElements();
    /**
     * Get the map of available properties for an
     * {@link FlowElement#getElementDataKey()}. The map returned contains the
     * {@link ElementPropertyMetaData}s keyed on the name field.
     * @return an immutable map of available properties
     */
    Map<String, Map<String, ElementPropertyMetaData>> getElementAvailableProperties();
    
}
