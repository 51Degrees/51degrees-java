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

package fiftyone.pipeline.derivedproperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Every fault found in the scripts given to a Derived Property Element
 * builder, raised once after every script has been checked. The message
 * lists every fault, one per line.
 */
public class DerivedScriptValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final DerivedScriptFault[] faults;

    /**
     * Construct the exception.
     * @param faults every fault found
     */
    public DerivedScriptValidationException(List<DerivedScriptFault> faults) {
        super(message(faults));
        this.faults = faults.toArray(new DerivedScriptFault[0]);
    }

    /**
     * @return every fault found, in the order found
     */
    public List<DerivedScriptFault> getFaults() {
        return Collections.unmodifiableList(Arrays.asList(faults));
    }

    private static String message(List<DerivedScriptFault> faults) {
        StringBuilder message = new StringBuilder();
        for (DerivedScriptFault fault : faults) {
            if (message.length() > 0) {
                message.append('\n');
            }
            message.append(fault);
        }
        return message.toString();
    }
}
