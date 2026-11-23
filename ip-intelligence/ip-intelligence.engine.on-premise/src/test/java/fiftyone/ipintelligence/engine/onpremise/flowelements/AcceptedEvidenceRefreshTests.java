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

package fiftyone.ipintelligence.engine.onpremise.flowelements;

import fiftyone.ipintelligence.engine.onpremise.TestsBase;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.EngineIpiSwig;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.engines.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * The engine reads the Evidence keys the data file accepts from the native
 * engine when it starts and again whenever the data is refreshed. Both free
 * data files accept the same keys, so the test puts a key no data file has
 * in place of the keys before a refresh, and checks that the refresh
 * replaces it with the keys the native engine gives.
 */
public class AcceptedEvidenceRefreshTests extends TestsBase {

    private static final String STALE_KEY = "query.stale-key";

    @Before
    public void init() throws Exception {
        testInitialize(Constants.PerformanceProfiles.LowMemory);
    }

    @After
    public void cleanup() {
        testCleanup();
    }

    @Test
    public void StartUp_KeysAreTheNativeEngines() throws Exception {
        IPIntelligenceOnPremiseEngine engine = getWrapper().getEngine();
        assertEquals(nativeKeys(engine), filterKeys(engine));
    }

    @Test
    public void Refresh_ReadsTheKeysAgain() throws Exception {
        IPIntelligenceOnPremiseEngine engine = getWrapper().getEngine();
        List<String> stale = Collections.singletonList(STALE_KEY);
        set(engine, "evidenceKeys", stale);
        set(engine, "evidenceKeyFilter", new EvidenceKeyFilterWhitelist(
            stale, String.CASE_INSENSITIVE_ORDER));

        engine.refreshData(engine.getDataFileMetaData().getIdentifier());

        TreeSet<String> keys = filterKeys(engine);
        assertFalse(keys.toString(), keys.contains(STALE_KEY));
        assertEquals(nativeKeys(engine), keys);
    }

    private static TreeSet<String> filterKeys(
        IPIntelligenceOnPremiseEngine engine) {
        Map<String, Integer> whitelist = ((EvidenceKeyFilterWhitelist)
            engine.getEvidenceKeyFilter()).getWhitelist();
        return new TreeSet<>(whitelist.keySet());
    }

    private static TreeSet<String> nativeKeys(
        IPIntelligenceOnPremiseEngine engine) throws Exception {
        EngineIpiSwig nativeEngine = (EngineIpiSwig) get(engine, "engine");
        return new TreeSet<>(new ArrayList<>(nativeEngine.getKeys()));
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = IPIntelligenceOnPremiseEngine.class
            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value)
        throws Exception {
        Field field = IPIntelligenceOnPremiseEngine.class
            .getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
