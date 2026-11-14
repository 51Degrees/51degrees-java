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

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stands in for an element that supplies source properties, such as device
 * detection. It lists every property it is given a name for, as an engine
 * lists the properties it can supply, and writes the values it holds under
 * its Element Data key. A name with no value is listed but not written.
 */
public final class StubSourceElement
    extends FlowElementBase<ElementData, ElementPropertyMetaData> {

    private final String key;
    private final List<ElementPropertyMetaData> properties;
    private final Map<String, Object> values;
    private final boolean addData;

    /**
     * @param key the Element Data key
     * @param names the properties listed
     * @param values the values written, by property name
     * @param addData false to write no Element Data at all
     */
    StubSourceElement(
        String key,
        Collection<String> names,
        Map<String, Object> values,
        boolean addData) {
        super(LoggerFactory.getLogger(StubSourceElement.class),
            (flowData, element) -> new ElementDataBase(
                LoggerFactory.getLogger(StubSourceElement.class),
                flowData) {
            });
        this.key = key;
        this.values = values == null ?
            Collections.<String, Object>emptyMap() :
            new LinkedHashMap<>(values);
        List<ElementPropertyMetaData> listed = new ArrayList<>();
        for (String name : names) {
            listed.add(new ElementPropertyMetaDataDefault(
                name, this, "", Object.class, true));
        }
        this.properties = Collections.unmodifiableList(listed);
        this.addData = addData;
    }

    StubSourceElement(
        String key,
        Collection<String> names,
        Map<String, Object> values) {
        this(key, names, values, true);
    }

    @Override
    public String getElementDataKey() {
        return key;
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return new EvidenceKeyFilterWhitelist(Collections.<String>emptyList());
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return properties;
    }

    @Override
    protected void processInternal(FlowData data) {
        if (addData == false) {
            return;
        }
        ElementData elementData =
            data.getOrAdd(getTypedDataKey(), getDataFactory());
        for (Map.Entry<String, Object> value : values.entrySet()) {
            elementData.put(value.getKey(), value.getValue());
        }
    }

    @Override
    protected void managedResourcesCleanup() {
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }
}
