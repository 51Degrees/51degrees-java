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

package fiftyone.pipeline.derived.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Raised once after every script has been checked, listing every fault
 * found, one per line, rather than stopping at the first.
 */
public class DerivedScriptException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final transient List<DerivedScriptFault> faults;

    /**
     * Construct a new exception.
     * @param faults every fault found
     */
    public DerivedScriptException(List<DerivedScriptFault> faults) {
        super(message(faults));
        this.faults = Collections.unmodifiableList(new ArrayList<>(faults));
    }

    /**
     * Every fault found.
     * @return the faults
     */
    public List<DerivedScriptFault> getFaults() {
        return faults;
    }

    private static String message(List<DerivedScriptFault> faults) {
        StringBuilder message = new StringBuilder();
        message.append(faults.size() == 1 ?
            "1 fault was found in the derived property scripts." :
            faults.size() + " faults were found in the derived property scripts.");
        for (DerivedScriptFault fault : faults) {
            message.append(System.lineSeparator()).append(fault);
        }
        return message.toString();
    }
}
