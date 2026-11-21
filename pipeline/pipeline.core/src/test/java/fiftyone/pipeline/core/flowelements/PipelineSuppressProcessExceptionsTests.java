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

import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The suppress process exceptions setting can be read through the public
 * Pipeline interface, so that a wrapper such as the web integration can
 * decide from it. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/exception-handling.md#L42">exception-handling.md</a>:
 * "This property MUST be exposed as a public property as part of the
 * `Pipeline` public interface as `IsSuppressProcessExceptions`".
 */
public class PipelineSuppressProcessExceptionsTests {

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    /**
     * The Pipeline interface declares a public isSuppressProcessExceptions
     * that returns a boolean.
     */
    @Test
    public void pipelineInterface_DeclaresIsSuppressProcessExceptions()
        throws Exception {
        Method method = Pipeline.class.getMethod("isSuppressProcessExceptions");
        assertEquals(boolean.class, method.getReturnType());
        assertTrue(Modifier.isPublic(method.getModifiers()));
    }

    /**
     * The value read through the interface is the one the builder set, for
     * both settings.
     */
    @Test
    public void isSuppressProcessExceptions_GivesTheBuilderSetting()
        throws Exception {
        for (boolean suppress : new boolean[] { true, false }) {
            try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                    .setSuppressProcessException(suppress)
                    .build()) {
                Method method =
                    Pipeline.class.getMethod("isSuppressProcessExceptions");
                assertEquals(suppress, method.invoke(pipeline));
            }
        }
    }
}
