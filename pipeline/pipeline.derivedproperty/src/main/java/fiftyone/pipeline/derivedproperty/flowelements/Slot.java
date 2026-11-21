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

/**
 * One source property as read for one request, converted to the type the
 * script infers for it, or the reason it could not be read.
 */
final class Slot {

    private final boolean available;
    private final boolean bool;
    private final long integer;
    private final double real;
    private final String text;
    private final String reason;

    private Slot(
        boolean available,
        boolean bool,
        long integer,
        double real,
        String text,
        String reason) {
        this.available = available;
        this.bool = bool;
        this.integer = integer;
        this.real = real;
        this.text = text;
        this.reason = reason;
    }

    static Slot ofBool(boolean value) {
        return new Slot(true, value, 0, 0, null, null);
    }

    static Slot ofInt(long value) {
        return new Slot(true, false, value, 0, null, null);
    }

    static Slot ofDouble(double value) {
        return new Slot(true, false, 0, value, null, null);
    }

    static Slot ofString(String value) {
        return new Slot(true, false, 0, 0, value, null);
    }

    static Slot absent(String reason) {
        return new Slot(false, false, 0, 0, null, reason);
    }

    boolean isAvailable() {
        return available;
    }

    boolean getBool() {
        return bool;
    }

    long getInt() {
        return integer;
    }

    double getDouble() {
        return real;
    }

    String getString() {
        return text;
    }

    /**
     * @return why the property could not be read, or null where it was
     */
    String getReason() {
        return reason;
    }

    /**
     * The value as a number, for a property read as int or double.
     * @param type the type the property is read as
     * @return the value
     */
    double number(ValueType type) {
        return type == ValueType.INT ? integer : real;
    }
}
