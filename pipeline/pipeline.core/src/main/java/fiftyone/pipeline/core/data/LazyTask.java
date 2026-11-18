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

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

/**
 * The value of a lazy property, worked out once by one task. The task is
 * started by the Flow Element while it processes, with {@link #start}, or by
 * the first call to {@link #get()}. Every caller of {@link #get()}, including
 * the first, waits for the task to finish and then reads its result, so the
 * work is never done twice and no caller reads the value before it is ready.
 * If the task fails, every caller gets its error.
 * <p>
 * The task is the Flow Element's own guard on its own value. It must hold
 * only what it was given when it was made, and never read the Flow Data.
 * @param <T> type of the value
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#lazy-properties">Specification</a>
 */
public final class LazyTask<T> {

    private final FutureTask<T> task;

    /**
     * Make a task that works out the value when it is first asked for, or
     * when {@link #start} is called.
     * @param work the work that gives the value
     */
    public LazyTask(Callable<T> work) {
        task = new FutureTask<>(work);
    }

    /**
     * Start the work on the executor given, and return straight away. A
     * caller of {@link #get()} that comes before the executor has started the
     * work runs it on its own thread instead, and the executor then does
     * nothing.
     * @param executor runs the work
     * @return this task
     */
    public LazyTask<T> start(Executor executor) {
        executor.execute(task);
        return this;
    }

    /**
     * @return true once the work has finished, whether or not it failed
     */
    public boolean isDone() {
        return task.isDone();
    }

    /**
     * Wait for the work to finish and return its value. The work runs on
     * this thread if nothing has started it yet. An unchecked exception or
     * error from the work is thrown as it is, to every caller, and a checked
     * exception is thrown as the cause of a {@link CompletionException}.
     * @return the value
     * @throws CancellationException if the waiting thread is interrupted
     */
    public T get() {
        // A no-op if the work has already started or finished.
        task.run();
        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CancellationException cancelled = new CancellationException(
                "Interrupted while waiting for a lazy value");
            cancelled.initCause(e);
            throw cancelled;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new CompletionException(cause);
        }
    }
}
