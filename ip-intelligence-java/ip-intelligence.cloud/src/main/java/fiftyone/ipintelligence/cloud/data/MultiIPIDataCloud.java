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

package fiftyone.ipintelligence.cloud.data;

import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.MultiProfileData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates a list of {@link IPIntelligenceData} instances which can be returned
 * by the 51Degrees Cloud service when evidence matching multiple profiles is
 * provided.
 */
public final class MultiIPIDataCloud
    extends AspectDataBase
    implements MultiProfileData<IPIntelligenceData> {
    private static final String PROFILE_LIST_KEY = "profiles";

    /**
     * Constructs a new instance with a non-thread-safe, case-insensitive
     * {@link Map} as the underlying storage.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     * @param engine the engine which created the instance
     */
    public MultiIPIDataCloud(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
        super(logger, flowData, engine);
        this.put(PROFILE_LIST_KEY, new ArrayList<IPIntelligenceData>());
    }

    /**
     * Constructs a new instance with a non-thread-safe, case-insensitive
     * {@link Map} as the underlying storage.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     * @param engine the engine which created the instance
     * @param missingPropertyService service used to determine the reason for
     *                               a property value being missing
     */
    public MultiIPIDataCloud(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine,
        MissingPropertyService missingPropertyService) {
        super(logger, flowData, engine, missingPropertyService);
        this.put(PROFILE_LIST_KEY, new ArrayList<IPIntelligenceData>());
    }

    @Override
    public List<IPIntelligenceData> getProfiles() {
        return getProfileList();
    }

    @Override
    public void addProfile(IPIntelligenceData profile) {
        getProfileList().add(profile);
    }

    /**
     * Internal method to get the list of profiles from the underlying data.
     * @return the list of profiles from the underlying data.
     */
    @SuppressWarnings("unchecked")
    private List<IPIntelligenceData> getProfileList() {
        return (List<IPIntelligenceData>)this.get(PROFILE_LIST_KEY);
    }
}
