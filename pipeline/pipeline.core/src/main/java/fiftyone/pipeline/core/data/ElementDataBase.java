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
 * <p>
 * Element Data that can be pooled goes back to its Flow Element's pool when
 * the Flow Data that holds it is closed, and may then be given to another
 * request. Keeping a reference to Element Data after its Flow Data is closed
 * is not supported, and reads do not check for it.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/resource-cleanup.md#reusing-element-data">Reusing Element Data</a>
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
     * Where this instance goes when the Flow Data that holds it is closed, or
     * null when it is not pooled. Set by the Flow Element that gave it out.
     */
    private volatile Runnable onRelease = null;

    /**
     * Whether this instance can be given out again once the Flow Data that
     * held it is closed. An implementation whose only state is its values
     * returns true. One that keeps other state of the request, or puts
     * values in its constructor, also puts that back in {@link #onReuse()}.
     * One that a caller may keep after the Flow Data is closed keeps the
     * default.
     * @return false unless an implementation opts in
     */
    protected boolean isPoolable() {
        return false;
    }

    /**
     * @return true if this instance can be returned to its Flow Element's
     * pool
     */
    public boolean canBePooled() {
        return isPoolable();
    }

    /**
     * Set what happens to this instance when the Flow Data that holds it is
     * closed. Used by the Flow Element that gave it out.
     * @param onRelease run once on release, or null for none
     */
    public void setOnRelease(Runnable onRelease) {
        this.onRelease = onRelease;
    }

    /**
     * Keep this instance out of its pool, as a cache that holds it does.
     */
    public void keepFromPool() {
        onRelease = null;
    }

    /**
     * Whether a lazy task may still write this instance. An implementation
     * with a lazy task returns true until the task has finished.
     * @return false unless an implementation has a lazy task running
     */
    protected boolean hasLazyTaskRunning() {
        return false;
    }

    /**
     * Called when the Flow Data that holds this instance is closed. A pooled
     * instance goes back to its Flow Element's pool, once only. One whose
     * lazy task is still running is left for the garbage collector instead,
     * because the task may still write it.
     */
    public void release() {
        Runnable release = onRelease;
        onRelease = null;
        if (release != null && hasLazyTaskRunning() == false) {
            release.run();
        }
    }

    /**
     * Make a pooled instance ready for the next Flow Data, with no values
     * and open for writing.
     * @param flowData the Flow Data it is given to
     */
    public void reuse(FlowData flowData) {
        clearValues();
        finished = false;
        pipeline = flowData.getPipeline();
        onReuse();
    }

    /**
     * Put back, as the constructor left it, any state other than the values
     * that this instance kept from the request it served before. Called when
     * a pooled instance is given to the next Flow Data.
     */
    protected void onReuse() {
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
