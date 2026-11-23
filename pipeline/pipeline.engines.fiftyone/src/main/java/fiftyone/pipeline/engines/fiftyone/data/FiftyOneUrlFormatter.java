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


package fiftyone.pipeline.engines.fiftyone.data;

import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.DataUpdateUrlFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Formats the data update URL of a 51Degrees data file, and says which
 * product of the data update service the data file needs.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/data-updates.md#update-polling">Specification</a>
 */
public class FiftyOneUrlFormatter implements DataUpdateUrlFormatter {

    private final String product;

    /**
     * Construct a formatter that names no product in the URL. The product
     * the data file needs is found from the tier of the data its engine has
     * loaded.
     */
    public FiftyOneUrlFormatter() {
        this(null);
    }

    /**
     * Construct a formatter that asks for the product given, for example
     * V4TAC. Use this when the license key holds several products and the
     * data file is downloaded before the engine has loaded any data, as
     * update on start-up does.
     * @param product the product name, or null to name no product
     */
    public FiftyOneUrlFormatter(String product) {
        this.product = product == null || product.isEmpty() ? null : product;
    }

    @Override
    public String getFormattedDataUpdateUrl(AspectEngineDataFile dataFile) {
        if (dataFile instanceof FiftyOneDataFile) {
            FiftyOneDataFile fiftyOneDataFile = (FiftyOneDataFile) dataFile;
            List<String> parameters = new ArrayList<>(Arrays.asList(
                "LicenseKeys=" + stringJoin(dataFile.getConfiguration().getDataUpdateLicenseKeys(), "|"),
                "Download=True",
                "Type=" + fiftyOneDataFile.getDataUpdateDownloadType()));
            if (product != null) {
                parameters.add("Product=" + product);
            }
            return dataFile.getConfiguration().getDataUpdateUrl() + "?" +
                stringJoin(parameters, "&");
        }
        else {
            return dataFile.getConfiguration().getDataUpdateUrl();
        }
    }

    @Override
    public String getDataUpdateProduct(AspectEngineDataFile dataFile) {
        if (product != null) {
            return product;
        }
        // The data file names the product of the data it has loaded.
        return dataFile.getDataUpdateProduct();
    }
}
