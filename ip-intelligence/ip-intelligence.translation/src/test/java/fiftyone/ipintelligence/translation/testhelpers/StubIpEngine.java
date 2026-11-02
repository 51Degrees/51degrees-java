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

package fiftyone.ipintelligence.translation.testhelpers;

import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Stub flow element with the IP Intelligence engine's element data key
 * ({@code "ip"}) which exposes the weighted country code lists, standing in for
 * the on-premise engine in tests.
 */
public class StubIpEngine
    extends FlowElementBase<IPIntelligenceData, ElementPropertyMetaData> {

    private final AspectPropertyValue<List<IWeightedValue<String>>>
        geographical;
    private final AspectPropertyValue<List<IWeightedValue<String>>> population;

    public StubIpEngine(
        Logger logger,
        ElementDataFactory<IPIntelligenceData> elementDataFactory,
        AspectPropertyValue<List<IWeightedValue<String>>> geographical,
        AspectPropertyValue<List<IWeightedValue<String>>> population) {
        super(logger, elementDataFactory);
        this.geographical = geographical;
        this.population = population;
    }

    @Override
    public String getElementDataKey() {
        return "ip";
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
        IPIntelligenceData elementData =
            data.getOrAdd(getTypedDataKey(), getDataFactory());
        elementData.put("countrycodesgeographical", geographical);
        elementData.put("countrycodespopulation", population);
    }

    @Override
    protected void managedResourcesCleanup() {
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }
}
