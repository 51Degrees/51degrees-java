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

package fiftyone.pipeline.annotations;

import java.lang.annotation.*;

/**
 * Gives the default value of a builder setting, which the builder listing
 * shows. The first of the values that is set is used.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface DefaultValue {
    /**
     * The default as text.
     *
     * @return the value, or empty when not set
     */
    String value() default "";
    /**
     * The default as an integer.
     *
     * @return the value, or Integer.MIN_VALUE when not set
     */
    int intValue() default Integer.MIN_VALUE;
    /**
     * The default as a double.
     *
     * @return the value, or Double.MIN_VALUE when not set
     */
    double doubleValue() default Double.MIN_VALUE;
    /**
     * The default as a boolean, used when no other value is set.
     *
     * @return the value
     */
    boolean booleanValue() default false;
}
