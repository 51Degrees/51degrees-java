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

package fiftyone.devicedetection.hash.engine.onpremise.flowelements;

/** Constants used by the Hash engine. */
public class Constants {
    /** Create a new instance. */
    public Constants() {
    }

	/** Names and descriptions of the match metric properties. */
	public static class MatchMetrics {
	    /** Create a new instance. */
	    public MatchMetrics() {
	    }

		/** Category of the match metric properties. */
		public static final String CATEGORY = "Device Metrics";

		/** Component the match metric properties belong to. */
		public static final String COMPONENT = "Metrics";

		/* Names */
		/** Name of the matched nodes property. */
		public static final String MATCHED_NODES = "MatchedNodes";
		/** Name of the difference property. */
		public static final String DIFFERENCE = "Difference";
		/** Name of the drift property. */
		public static final String DRIFT = "Drift";
		/** Name of the device id property. */
		public static final String DEVICE_ID = "DeviceId";
		/** Name of the matched User-Agents property. */
		public static final String USER_AGENTS = "UserAgents";
		/** Name of the match method property. */
		public static final String METHOD = "Method";
		/** Name of the iterations property. */
		public static final String ITERATIONS = "Iterations";
		
		/* Match metric properties description */
		/** Description of the matched nodes property. */
		public static final String MATCHED_NODES_DESCRIPTION = "Indicates the number of " +
			"hash nodes matched within the evidence.";
		/** Description of the difference property. */
		public static final String DIFFERENCE_DESCRIPTION = "Used when detection method " +
			"is not Exact or None. This is an integer value and the larger the " +
			"value the less confident the detector is in this result.";
		/** Description of the drift property. */
		public static final String DRIFT_DESCRIPTION = "Total difference in character " +
			"positions where the substrings hashes were found away from where " +
			"they were expected.";
		/** Description of the device id property. */
		public static final String DEVICE_ID_DESCRIPTION = "Consists of four components " +
			"separated by a hyphen symbol: Hardware-Platform-Browser-IsCrawler " +
			"where each Component represents an ID of the corresponding Profile.";
		/** Description of the matched User-Agents property. */
		public static final String USER_AGENTS_DESCRIPTION = "The matched User-Agents.";
		/** Description of the match method property. */
		public static final String METHOD_DESCRIPTION = "Provides information about " +
			"the algorithm that was used to perform detection for a particular " +
			"User-Agent.";
		/** Description of the iterations property. */
		public static final String ITERATIONS_DESCRIPTION = "The number of iterations " +
			"carried out in order to find a match. This is the number of nodes " +
			"in the graph which have been visited.";
	}
}
