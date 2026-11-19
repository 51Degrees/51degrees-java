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
 * One source property, read once per request as one type, however many
 * scripts name it.
 */
final class SourceRead {

    private final SourceProperty property;
    private final int key;

    /**
     * @param property the property and the type it is read as
     * @param key the index of the Element Data key among the element's
     *            keys, or -1 for a derived property, which is read when
     *            the script that names it runs
     */
    SourceRead(SourceProperty property, int key) {
        this.property = property;
        this.key = key;
    }

    SourceProperty getProperty() {
        return property;
    }

    int getKey() {
        return key;
    }
}
