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

package fiftyone.ipintelligence.examples.web;

import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.pipeline.core.data.FlowData;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class HtmlContentHelper {

    /**
     * The number of days after which an on-premise data file is considered old
     * enough to warrant a warning on the example pages. Matches the threshold
     * used by the other 51Degrees examples.
     */
    private static final long DATA_FILE_AGE_WARNING_DAYS = 28;

    /**
     * The contact-us message banner has two variants. The cloud variant invites
     * the user to discuss on-premise requirements. The on-premise variant invites
     * the user to explore additional properties and features.
     */
    public enum ContactMessageVariant {
        CLOUD,
        ON_PREMISE
    }

    /**
     * Build the stale-data-file warning shown at the top of the on-premise web
     * example pages, mirroring the other 51Degrees examples. The warning is only
     * rendered when an on-premise engine is present and its data file is more
     * than {@link #DATA_FILE_AGE_WARNING_DAYS} days old. Cloud examples have no
     * local data file, so they receive an empty string.
     *
     * @param flowData the flow data for the request, used to locate the
     *                 on-premise engine and read its data file published date
     * @return the warning markup, or an empty string when no warning is needed
     */
    public static String getDataFileAgeWarning(FlowData flowData) {
        IPIntelligenceOnPremiseEngine engine =
                flowData.getPipeline().getElement(IPIntelligenceOnPremiseEngine.class);
        if (engine == null) {
            // No on-premise engine (e.g. a cloud example).
            return "";
        }
        Date published = engine.getDataFilePublishedDate();
        if (published == null) {
            return "";
        }
        long daysOld = ChronoUnit.DAYS.between(published.toInstant(), Instant.now());
        if (daysOld <= DATA_FILE_AGE_WARNING_DAYS) {
            return "";
        }
        // language=html
        return "<div class=\"c-eg-alert\">\n" +
                "  The IP intelligence data file is " + daysOld + " days old. " +
                "A more recent data file may be needed for the most accurate IP " +
                "intelligence results. The latest Lite data file is available from the " +
                "<a href=\"https://github.com/51Degrees/ip-intelligence-data\">ip-intelligence-data " +
                "repository on GitHub</a>. Find out about the full Enterprise data file, " +
                "which includes automatic daily updates, on our " +
                "<a href=\"https://51degrees.com/pricing?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=web-shared-src-main-java-fiftyone-ipintelligence-examples-web-htmlcontenthelper.java&utm_term=data-file-age-warning\">pricing page</a>.\n" +
                "</div>";
    }

    /**
     * Build the contact-us message banner shown at the bottom of the web example
     * pages. Cloud examples are free by design, so the cloud variant is shown
     * unconditionally. On-premise examples only show the banner when running
     * against the free Lite tier data file, so the {@code show} flag should be
     * wired to {@code engine.getDataSourceTier().equals("Lite")}.
     *
     * @param variant which message wording to use
     * @param show    whether the banner should be rendered at all
     * @return the banner markup, or an empty string when {@code show} is false
     */
    public static String getContactMessage(ContactMessageVariant variant, boolean show) {
        if (show == false) {
            return "";
        }
        String text = variant == ContactMessageVariant.CLOUD
                ? "Want to try on-premise? <a href=\"https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=web-shared-src-main-java-fiftyone-ipintelligence-examples-web-htmlcontenthelper.java&utm_term=contact-us\">Contact us</a> to discuss requirements."
                : "Need more on-premise properties and features? <a href=\"https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=web-shared-src-main-java-fiftyone-ipintelligence-examples-web-htmlcontenthelper.java&utm_term=contact-us\">Contact us</a> to explore the options.";
        // language=html
        return "<div class=\"c-eg-message\">\n" +
                "  <p class=\"c-eg-message__text\">" + text + "</p>\n" +
                "  <a class=\"b-btn c-eg-message__cta\" href=\"https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=web-shared-src-main-java-fiftyone-ipintelligence-examples-web-htmlcontenthelper.java&utm_term=contact-us\">Contact us</a>\n" +
                "</div>";
    }
}
