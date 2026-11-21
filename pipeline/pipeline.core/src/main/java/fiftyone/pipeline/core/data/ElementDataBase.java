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

package fiftyone.pipeline.core.data;

import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import org.slf4j.Logger;

import java.util.Map;
import java.util.TreeMap;

/**
 * Base implementation of {@link ElementData}.
 * Represents property values that have be determined by a specific
 * {@link FlowElement} based on the supplied evidence.
 * <p>
 * An instance is written only by the {@link FlowElement} that creates
 * it, while that element processes. It is not intended to be written
 * from several threads at once, nor read while it is being written.
 * </p>
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#element-data">Specification</a>
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Thread safety</a>
  * <p>
 * Element Data is written only by the Flow Element that provides it, or the
 * Flow Elements that share it, and only while they process. Once a Flow
 * Element that writes it has finished, {@link #put(String, Object)},
 * {@link #populateFromMap(Map)} and every setter built on them fail with an
 * {@link IllegalStateException}. Values kept once they have been read, with
 * {@link #keepValues(Map)}, are not writes. Like the Flow Data that holds
 * it, Element Data is not safe to use from several threads at once.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 */
public abstract class ElementDataBase extends DataBase implements ElementData {

    private Pipeline pipeline;

    /**
     * Set once a Flow Element that writes this instance has finished, after
     * which a write fails. A write tests it, and nothing waits on it.
     */
    private volatile boolean finished = false;

    private static final String FINISHED_MESSAGE =
        "This Element Data's Flow Element has finished processing, so it " +
        "is read only. Element Data is written only by the Flow Element " +
        "that provides it, while it processes.";

    /**
     * Constructs a new instance with a non-thread-safe, case-insensitive
     * {@link Map} as the underlying storage.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     */
    public ElementDataBase(Logger logger, FlowData flowData) {
        this(logger, flowData, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Constructs a new instance with a custom {@link Map} as the underlying
     * storage.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     * @param data the custom {@link Map} implementation to use as the
     *             underlying storage
     */
    public ElementDataBase(
        Logger logger,
        FlowData flowData,
        Map<String, Object> data) {
        super(logger, data);
        pipeline = flowData.getPipeline();
    }

    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * Record that a Flow Element that writes this instance has finished, so
     * that a later write fails. Called by the Flow Element when it finishes
     * processing, whether or not it succeeded.
     */
    public void finishWriting() {
        finished = true;
    }

    /**
     * Open this instance for writing again, as a Flow Element that shares it
     * does when it starts to process.
     */
    public void startWriting() {
        finished = false;
    }

    /**
     * @return true once a Flow Element that writes this instance has
     * finished, until one that shares it starts to process
     */
    public boolean isFinishedWriting() {
        return finished;
    }

    /**
     * Store a value, which only the Flow Element that provides this instance,
     * or one that shares it, does while it processes.
     * @throws IllegalStateException if the Flow Element has finished
     */
    @Override
    public void put(String key, Object value) {
        if (finished) {
            throw new IllegalStateException(FINISHED_MESSAGE);
        }
        super.put(key, value);
    }

    /**
     * Store values, which only the Flow Element that provides this instance,
     * or one that shares it, does while it processes.
     * @throws IllegalStateException if the Flow Element has finished
     */
    @Override
    public void populateFromMap(Map<String, Object> values) {
        checkWriting();
        super.populateFromMap(values);
    }

    /**
     * Keep values this instance has read, such as those read from a results
     * instance, so they are not read again. Keeping them is not a write, so
     * it works once the Flow Element has finished.
     * @param values the values read
     */
    protected void keepValues(Map<String, Object> values) {
        super.populateFromMap(values);
    }

    /**
     * Fail a write once a Flow Element that writes this instance has
     * finished. A write method that does not go through
     * {@link #put(String, Object)} calls it first.
     * @throws IllegalStateException if the Flow Element has finished
     */
    protected void checkWriting() {
        if (finished) {
            throw new IllegalStateException(FINISHED_MESSAGE);
        }
    }
}
