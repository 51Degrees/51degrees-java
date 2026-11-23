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

package pipeline.developerexamples.fodid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that the 51Did creator context demo server listens on port 5151
 * when the _51DEGREES_EXAMPLE_HTTP_PORTS environment variable does not
 * choose another, as the readme says.
 */
class CreatorContextDemoServerPortTests {

    @Test
    void listensOn5151ByDefault() {
        assertEquals(5151, CreatorContextDemoServer.DEFAULT_PORT);
        assertEquals(5151, CreatorContextDemoServer.port(null));
        assertEquals(5151, CreatorContextDemoServer.port(" "));
    }

    @Test
    void listensOnTheFirstPortTheVariableLists() {
        assertEquals(8100, CreatorContextDemoServer.port("8100"));
        assertEquals(8101, CreatorContextDemoServer.port(" 8101, 8102"));
    }
}
