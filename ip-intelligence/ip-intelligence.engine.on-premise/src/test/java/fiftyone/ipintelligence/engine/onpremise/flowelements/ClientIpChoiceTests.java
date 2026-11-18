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
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * When the Evidence holds more than one client IP key, one address is used,
 * a query key ahead of a server key, and the Property values describe that
 * address.
 */
public class ClientIpChoiceTests extends TestsBase {

    private static final String QUERY_KEY = "query.client-ip";

    private static final String SERVER_KEY = "server.true-client-ip-51d";

    private static final String QUERY_ADDRESS = "116.154.188.222";

    private static final String SERVER_ADDRESS = "185.28.167.77";

    @Before
    public void init() throws Exception {
        testInitialize(Constants.PerformanceProfiles.LowMemory);
    }

    @After
    public void cleanup() {
        testCleanup();
    }

    @Test
    public void QueryAndServerKeys_QueryAddressUsed() throws Exception {
        assertAccepted(QUERY_KEY);
        assertAccepted(SERVER_KEY);
        Map<String, String> query = values(QUERY_KEY, QUERY_ADDRESS);
        Map<String, String> server = values(SERVER_KEY, SERVER_ADDRESS);
        assertNotEquals(
            "Expected the two addresses to give different values.",
            query,
            server);

        assertEquals(query, values(
            QUERY_KEY, QUERY_ADDRESS, SERVER_KEY, SERVER_ADDRESS));
        assertEquals(query, values(
            SERVER_KEY, SERVER_ADDRESS, QUERY_KEY, QUERY_ADDRESS));
    }

    @Test
    public void ServerKeyOnly_ServerAddressUsed() throws Exception {
        assertAccepted(SERVER_KEY);
        assertEquals(
            values(QUERY_KEY, SERVER_ADDRESS),
            values(SERVER_KEY, SERVER_ADDRESS));
    }

    private void assertAccepted(String key) {
        assertTrue("Expected the engine to accept '" + key + "'.",
            getWrapper().getEngine().getEvidenceKeyFilter().include(key));
    }

    /**
     * Process Evidence given as key, value pairs and give the registered
     * name, country and owner.
     */
    private Map<String, String> values(String... evidence) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        try (FlowData data = getWrapper().getPipeline().createFlowData()) {
            for (int i = 0; i < evidence.length; i += 2) {
                data.addEvidence(evidence[i], evidence[i + 1]);
            }
            data.process();
            IPIntelligenceData ipi = data.get(IPIntelligenceData.class);
            values.put("RegisteredName", ipi.getRegisteredName().getValue());
            values.put("RegisteredCountry",
                ipi.getRegisteredCountry().getValue());
            values.put("RegisteredOwner",
                ipi.getRegisteredOwner().getValue());
        }
        return values;
    }
}
