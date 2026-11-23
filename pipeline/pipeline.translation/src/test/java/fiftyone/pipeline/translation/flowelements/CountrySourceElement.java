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

package fiftyone.pipeline.translation.flowelements;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Stands in for the element a Translation Engine reads from, giving one
 * country name under the element data key {@code source}.
 */
public class CountrySourceElement
    extends FlowElementBase<ElementData, ElementPropertyMetaData> {

    private final String country;

    CountrySourceElement(String country) {
        super(LoggerFactory.getLogger(CountrySourceElement.class),
            (flowData, element) -> new ElementDataBase(
                LoggerFactory.getLogger(CountrySourceElement.class),
                flowData) {
            });
        this.country = country;
    }

    @Override
    public String getElementDataKey() {
        return "source";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return new EvidenceKeyFilterWhitelist(Collections.<String>emptyList());
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return Collections.emptyList();
    }

    @Override
    protected void processInternal(FlowData data) {
        ElementData elementData =
            data.getOrAdd(getTypedDataKey(), getDataFactory());
        elementData.put("Country", country);
    }

    @Override
    protected void managedResourcesCleanup() {
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }
}
