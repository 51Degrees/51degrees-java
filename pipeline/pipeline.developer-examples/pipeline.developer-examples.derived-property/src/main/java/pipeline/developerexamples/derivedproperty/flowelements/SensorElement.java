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

package pipeline.developerexamples.derivedproperty.flowelements;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Stands in for an engine in this example. It reads a temperature and a
 * humidity from the evidence, {@code query.temperature} and
 * {@code query.humidity}, and supplies them as the properties
 * {@code Temperature} and {@code Humidity} under the element data key
 * {@code sensor}. A reading that is not in the evidence is not supplied,
 * which is how a real engine leaves out a property it cannot find.
 */
public class SensorElement
    extends FlowElementBase<ElementData, ElementPropertyMetaData> {

    private static final List<String> EVIDENCE =
        Arrays.asList("query.temperature", "query.humidity");

    private volatile List<ElementPropertyMetaData> properties;

    public SensorElement(Logger logger) {
        super(logger, (flowData, element) ->
            new ElementDataBase(logger, flowData) { });
    }

    @Override
    public String getElementDataKey() {
        return "sensor";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return new EvidenceKeyFilterWhitelist(EVIDENCE,
            String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        if (properties == null) {
            properties = Arrays.asList(
                (ElementPropertyMetaData) new ElementPropertyMetaDataDefault(
                    "Temperature", this, "Reading", Integer.class, true),
                new ElementPropertyMetaDataDefault(
                    "Humidity", this, "Reading", Integer.class, true));
        }
        return properties;
    }

    @Override
    protected void processInternal(FlowData data) {
        ElementData readings =
            data.getOrAdd(getTypedDataKey(), getDataFactory());
        supply(data, readings, "query.temperature", "Temperature");
        supply(data, readings, "query.humidity", "Humidity");
    }

    private static void supply(FlowData data, ElementData readings,
                               String evidence, String property) {
        TryGetResult<String> value = data.tryGetEvidence(evidence, String.class);
        if (value.hasValue()) {
            readings.put(property, Integer.parseInt(value.getValue().trim()));
        }
    }

    @Override
    protected void managedResourcesCleanup() {
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }
}
