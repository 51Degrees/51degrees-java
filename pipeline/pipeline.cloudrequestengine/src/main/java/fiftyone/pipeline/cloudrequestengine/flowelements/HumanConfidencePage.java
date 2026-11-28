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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * What one page needs so that its sealed Human Confidence result can be
 * redeemed. Made by
 * {@link CloudRequestEngineDefault#newHumanConfidencePage()}, once for each
 * page served.
 * <p>
 * The page loads the 51Degrees client script from
 * {@link #scriptAddress(String)}, which carries the page's transaction, and
 * sends {@link #getId()} back as the {@code humanconfidencepage} parameter
 * beside the sealed result. The engine keeps the transaction and finds it
 * by the page id, so the transaction is never read from a request.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/human-confidence.md#the-transaction">The transaction</a>
 */
public final class HumanConfidencePage {

    private final String id;
    private final String transaction;

    HumanConfidencePage(String id, String transaction) {
        this.id = id;
        this.transaction = transaction;
    }

    /**
     * The page id the page sends back as {@code humanconfidencepage}, so
     * the engine can find the transaction it kept for the page.
     * @return the page id
     */
    public String getId() {
        return id;
    }

    /**
     * The address of the 51Degrees client script with this page's
     * transaction added as the {@code transaction} parameter.
     * @param address the client script's address, such as
     * {@code https://cloud.51degrees.com/api/v4/<resource key>.js}
     * @return the address to load the script from
     */
    public String scriptAddress(String address) {
        try {
            return address + (address.indexOf('?') < 0 ? '?' : '&') +
                "transaction=" + URLEncoder.encode(transaction, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The transaction, for the engine alone.
     */
    String getTransaction() {
        return transaction;
    }
}
