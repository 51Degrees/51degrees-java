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

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import fiftyone.pipeline.engines.Constants;
import static fiftyone.ipintelligence.shared.testhelpers.FileUtils.LITE_IPI_DATA_FILE_NAME;
import static fiftyone.ipintelligence.shared.testhelpers.FileUtils.ENTERPRISE_IPI_V41_DATA_FILE_NAME;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

@Ignore
public class EngineTests {
	protected static final ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

	private IPIntelligenceOnPremiseEngine createEngine(File dataFile) throws Exception {
		return new IPIntelligenceOnPremiseEngineBuilder(loggerFactory, null)
                .setPerformanceProfile(Constants.PerformanceProfiles.MaxPerformance)
                .setAutoUpdate(false)
                .build(dataFile.toString(), false);
	}
	
	/**
	 * Shared method for dataDataSourceTier tests.
	 * @param fileName
	 * @throws Exception
	 */
	private void testDataSourceTier(String fileName) throws Exception {
		File dataFile = null;
		try {
			dataFile = getFilePath(fileName);
		}
		catch (IllegalArgumentException e) {
			assumeTrue(e.getMessage(), false);
		}
		
		IPIntelligenceOnPremiseEngine engine = createEngine(dataFile);
		String tier = engine.getDataSourceTier();
		engine.close();
		
		if (fileName.equals(LITE_IPI_DATA_FILE_NAME)) {
			assertEquals("Lite", tier);
		}
		else {
			assertTrue(tier.equalsIgnoreCase("Enterprise"));
		}
	}
	
	/**
	 * Check that getDataSourceTier returns correct product for Lite data file
	 * @throws Exception
	 */
	@Test
	public void Engine_Ipi_GetDataSourceTier_Lite() throws Exception {
		testDataSourceTier(LITE_IPI_DATA_FILE_NAME);
	}
	
	/**
	 * Check that getDataSourceTier returns correct product for Enterprise data
	 * file
	 * @throws Exception
	 */
	@Test
	public void Engine_Ipi_GetDataSourceTier_Enterprise() throws Exception {
		testDataSourceTier(ENTERPRISE_IPI_V41_DATA_FILE_NAME);
	}

}
