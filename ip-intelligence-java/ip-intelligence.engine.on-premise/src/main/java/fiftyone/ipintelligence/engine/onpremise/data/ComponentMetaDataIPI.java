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

package fiftyone.ipintelligence.engine.onpremise.data;

import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.engine.onpremise.interop.PropertyIterable;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.ComponentMetaDataSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.PropertyMetaDataCollectionSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.PropertyMetaDataSwig;
import fiftyone.pipeline.engines.fiftyone.data.ComponentMetaData;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;
import fiftyone.pipeline.engines.fiftyone.data.ProfileMetaData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * On-premise implementation of the {@link ComponentMetaData} interface.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/data-model-specification/README.md#component">Specification</a>
 */
public class ComponentMetaDataIPI implements ComponentMetaData {

    private final ComponentMetaDataSwig source;

    private final IPIntelligenceOnPremiseEngine engine;

    private final List<FiftyOneAspectPropertyMetaData> properties = new ArrayList<>();

    /**
     * Construct a new instance.
     * @param engine the engine creating the instance
     * @param source the source metadata from the native engine
     */
    public ComponentMetaDataIPI(
        IPIntelligenceOnPremiseEngine engine,
        ComponentMetaDataSwig source) {
        this.engine = engine;
        this.source = source;
    }

    @Override
    public byte getComponentId() {
        return source.getComponentId();
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public ProfileMetaData getDefaultProfile() {
        return new ProfileMetaDataIPI(
            engine,
            engine.getMetaData().getDefaultProfileForComponent(source));
    }

    @Override
    public Iterable<FiftyOneAspectPropertyMetaData> getProperties() {
        return new PropertyIterable(
            engine,
            properties,
            engine.getMetaData().getPropertiesForComponent(source));
    }

    @Override
    public FiftyOneAspectPropertyMetaData getProperty(String propertyName) {
        FiftyOneAspectPropertyMetaData result = null;
        PropertyMetaDataCollectionSwig components =
            engine.getMetaData().getPropertiesForComponent(source);
        PropertyMetaDataSwig value = components.getByKey(propertyName);
        if (value != null) {
            result = new PropertyMetaDataIPI(engine, value);
        }
        components.delete();
        return result;
    }

    @Override
    public int hashCode() {
        return getComponentId();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ComponentMetaData) {
            return equals((ComponentMetaData) obj);
        }
        return super.equals(obj);
    }

    public boolean equals(ComponentMetaData other) {
        return getComponentId() == other.getComponentId();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void close() throws IOException {
        source.delete();
    }
}