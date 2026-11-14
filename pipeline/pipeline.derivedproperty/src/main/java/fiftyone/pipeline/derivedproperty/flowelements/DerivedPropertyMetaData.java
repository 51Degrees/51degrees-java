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
 * The full Property metadata block of one script a Derived Property Element
 * holds. Its {@link #getOutput()} is the whole definition of the derived
 * property.
 */
public final class DerivedPropertyMetaData {

    private final String name;
    private final String version;
    private final int format;
    private final boolean deprecated;
    private final String deprecationNote;
    private final String source;
    private final DerivedOutputMetaData output;

    DerivedPropertyMetaData(DerivedScript script) {
        this.name = script.getName();
        this.version = script.getVersion();
        this.format = script.getFormat();
        this.deprecated = script.isDeprecated();
        this.deprecationNote = script.getDeprecationNote();
        this.source = script.getSource();
        this.output = new DerivedOutputMetaData(script.getOutput());
    }

    /**
     * @return the script's name, which is what selects it
     */
    public String getName() {
        return name;
    }

    /**
     * @return the author's semantic version, which plays no part in
     * selecting the script
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return the version of the script language, which is 1
     */
    public int getFormat() {
        return format;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    /**
     * @return what to use instead, or empty where the script is not
     * deprecated
     */
    public String getDeprecationNote() {
        return deprecationNote;
    }

    /**
     * @return the built-in script's name, the file path, or "code"
     */
    public String getSource() {
        return source;
    }

    /**
     * @return the definition of the property the script produces
     */
    public DerivedOutputMetaData getOutput() {
        return output;
    }
}
