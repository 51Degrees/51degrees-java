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

package fiftyone.pipeline.core.exceptions;

/**
 * Thrown by close() of a Pipeline or Flow Data when something it closes in
 * turn throws, which is the cause. Their close() methods throw no checked
 * exception, so a try-with-resources block needs no catch for them.
 */
public class CloseException extends RuntimeException {
    /**
     * Serializable class version number, which is used during deserialization.
     */
    private static final long serialVersionUID = 4061943563212306174L;

    /**
     * Constructor
     * @param message the exception message
     * @param cause the exception thrown by what was being closed
     */
    public CloseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * The exception to throw for one that close() of a member threw. An
     * unchecked exception is given back as it is, and a checked one becomes
     * the cause of a CloseException. An InterruptedException sets the
     * thread's interrupt status again, so the interrupt is not lost.
     * @param message the message for a CloseException
     * @param cause the exception close() of the member threw
     * @return the exception to throw
     */
    public static RuntimeException of(String message, Exception cause) {
        if (cause instanceof RuntimeException) {
            return (RuntimeException) cause;
        }
        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new CloseException(message, cause);
    }
}
