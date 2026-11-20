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

package fiftyone.devicedetection.hash.engine.onpremise.flowelements;

import fiftyone.pipeline.engines.fiftyone.data.CloseableIterable;
import fiftyone.pipeline.engines.fiftyone.data.CloseableIterableDefault;
import fiftyone.pipeline.engines.fiftyone.data.ComponentMetaData;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;
import fiftyone.pipeline.engines.fiftyone.data.ProfileMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Metrics component, which holds the match metric properties. The data
 * file has no such component, so the engine adds it to the components the
 * native code gives, and each match metric property names it as its
 * component.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/device-detection-specification/pipeline-elements/device-detection-on-premise.md#match-metric-properties">Specification</a>
 */
class MetricsComponentMetaData implements ComponentMetaData {

    /**
     * Id of the Metrics component. The data file's components are numbered
     * from 1, so this is the largest id a component can have, which no data
     * file component uses.
     */
    static final byte COMPONENT_ID = (byte) 255;

    private final List<FiftyOneAspectPropertyMetaData> properties =
        new ArrayList<>();

    /**
     * Add a match metric property to the component. Called only while the
     * engine makes its match metric properties.
     * @param property to add
     */
    void addProperty(FiftyOneAspectPropertyMetaData property) {
        properties.add(property);
    }

    /**
     * The match metric properties, in the order they were added. Holds no
     * native memory, so there is nothing to close.
     * @return unmodifiable list of the properties
     */
    List<FiftyOneAspectPropertyMetaData> getPropertyList() {
        return Collections.unmodifiableList(properties);
    }

    @Override
    public byte getComponentId() {
        return COMPONENT_ID;
    }

    @Override
    public String getName() {
        return Constants.MatchMetrics.COMPONENT;
    }

    /**
     * The match metrics have no profiles.
     * @return null
     */
    @Override
    public ProfileMetaData getDefaultProfile() {
        return null;
    }

    @Override
    public CloseableIterable<FiftyOneAspectPropertyMetaData> getProperties() {
        return new CloseableIterableDefault<>(
            Collections.unmodifiableList(properties));
    }

    @Override
    public FiftyOneAspectPropertyMetaData getProperty(String propertyName) {
        for (FiftyOneAspectPropertyMetaData property : properties) {
            if (property.getName().equalsIgnoreCase(propertyName)) {
                return property;
            }
        }
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return getName();
    }
}
