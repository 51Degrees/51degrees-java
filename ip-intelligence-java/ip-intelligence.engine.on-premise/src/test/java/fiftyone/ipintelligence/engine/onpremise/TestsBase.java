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

package fiftyone.ipintelligence.engine.onpremise;

import fiftyone.ipintelligence.shared.testhelpers.IpAddressGenerator;
import fiftyone.ipintelligence.shared.testhelpers.FileUtils;
import fiftyone.pipeline.engines.Constants;

import static fiftyone.ipintelligence.shared.testhelpers.FileUtils.IP_ADDRESSES_FILE_NAME;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

public class TestsBase {

    private WrapperHash wrapper = null;
    private IpAddressGenerator ipAddresses;

    protected WrapperHash getWrapper() {
        return wrapper;
    }

    protected IpAddressGenerator getIpAddresses() {
        return ipAddresses;
    }

    protected void testInitialize(Constants.PerformanceProfiles profile) throws Exception {
        wrapper = new WrapperHash(
            FileUtils.getHashFile(),
            profile);
        ipAddresses = new IpAddressGenerator(
            getFilePath(IP_ADDRESSES_FILE_NAME));
    }

    public void testCleanup() {
        if (wrapper != null) {
            wrapper.close();
        }
    }
}
