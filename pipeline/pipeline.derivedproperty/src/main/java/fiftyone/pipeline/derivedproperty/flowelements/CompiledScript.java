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

package fiftyone.pipeline.derivedproperty.flowelements;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A script with each of its source properties resolved to one of the
 * element's reads.
 */
final class CompiledScript {

    private final DerivedScript model;
    private final int[] reads;

    /**
     * Records, without a lock, that the error for a request with no Element
     * Data under a key the script reads has been logged. It is the only
     * state shared between requests.
     */
    private final AtomicBoolean logged = new AtomicBoolean(false);

    /**
     * @param model the validated script
     * @param reads for each of the script's source properties, the index of
     *              the element's read that supplies it
     */
    CompiledScript(DerivedScript model, int[] reads) {
        this.model = model;
        this.reads = reads.clone();
    }

    DerivedScript getModel() {
        return model;
    }

    int readCount() {
        return reads.length;
    }

    int read(int i) {
        return reads[i];
    }

    /**
     * @return true the first time it is called, and false after that
     */
    boolean firstMissingData() {
        return logged.compareAndSet(false, true);
    }
}
